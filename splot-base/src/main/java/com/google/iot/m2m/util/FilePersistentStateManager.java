/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.iot.m2m.util;

import com.google.common.collect.ImmutableMap;
import com.google.iot.cbor.*;
import com.google.iot.m2m.base.CorruptPersistentStateException;
import com.google.iot.m2m.base.PersistentStateInterface;
import com.google.iot.m2m.base.PersistentStateListener;
import com.google.iot.m2m.base.PersistentStateManager;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A persistent state manager that uses a {@link File} object to store data.
 *
 * <p>This implementation takes some reasonable precautions to ensure that the data is written
 * atomically to the disk. However, if the underlying file system is not journaled then no
 * guarantees can be made.
 */
public final class FilePersistentStateManager implements PersistentStateManager {
    private static final boolean DEBUG = false;
    private static final int ASYNC_SAVE_DELAY_MS = 250;
    private static final Logger LOGGER =
            Logger.getLogger(FilePersistentStateManager.class.getCanonicalName());

    private final ScheduledExecutorService mExecutor;
    private final Map<String, PersistentStateInterface> mManagedObjects = new HashMap<>();
    private final CborMap mPersistentState = CborMap.create();
    private final File mFile;
    private final File mTempFile;
    private final File mOldFile;
    private final AtomicBoolean mWritePending = new AtomicBoolean(false);

    /**
     * Creates a new {@link PersistentStateManager} that is backed by a {@link File}.
     *
     * @param file the {@link File} to use for loading/storing the serialized persistent state
     * @return a new {@link FilePersistentStateManager} instance
     * @throws IllegalArgumentException if {@code file} is a directory
     * @throws IOException if there was a problem with reading the contents of {@code file}
     * @throws CorruptPersistentStateException if the data stored in {@code file} could not be
     *     parsed correctly
     */
    public static FilePersistentStateManager create(File file)
            throws IOException, CorruptPersistentStateException {
        FilePersistentStateManager ret = new FilePersistentStateManager(file);

        try {
            ret.loadInitialData();

        } catch (Throwable e) {
            ret.closeNoFlush();
            throw e;
        }

        return ret;
    }

    /**
     * Creates a new {@link PersistentStateManager} that is backed by a {@link File}, automatically
     * erasing it if is corrupted.
     *
     * @param file the {@link File} to use for loading/storing the serialized persistent state
     * @return a new {@link FilePersistentStateManager} instance
     * @throws IllegalArgumentException if {@code file} is a directory
     * @throws IOException if there was a problem with reading the contents of {@code file}
     */
    public static FilePersistentStateManager createAndResetIfCorrupt(File file) throws IOException {
        FilePersistentStateManager ret = new FilePersistentStateManager(file);

        try {
            ret.loadInitialData();

        } catch (CorruptPersistentStateException e) {
            LOGGER.warning("Persistent data was corrupted, clearing and starting over.");

            if (ret.mFile.delete()) {
                LOGGER.warning("Deleted \"" + ret.mFile + "\"");
            }

            if (ret.mOldFile.delete()) {
                LOGGER.warning("Deleted \"" + ret.mOldFile + "\"");
            }

            if (ret.mTempFile.delete()) {
                LOGGER.warning("Deleted \"" + ret.mTempFile + "\"");
            }

        } catch (Throwable e) {
            ret.closeNoFlush();
            throw e;
        }

        return ret;
    }

    private FilePersistentStateManager(File file) {
        mFile = file;

        if (mFile.isDirectory()) {
            throw new IllegalArgumentException("File cannot be a directory");
        }

        mTempFile = new File(mFile.getParentFile(), mFile.getName() + ".temp");
        mOldFile = new File(mFile.getParentFile(), mFile.getName() + ".old");

        mExecutor = new ScheduledThreadPoolExecutor(1) {
            @Override
            protected void afterExecute(Runnable r, @Nullable Throwable t) {
                super.afterExecute(r, t);

                if (t != null) {
                    Thread.getDefaultUncaughtExceptionHandler()
                            .uncaughtException(Thread.currentThread(), t);
                }
            }
        };
    }

    private void loadInitialData() throws IOException, CorruptPersistentStateException {
        if (!mFile.exists()) {
            if (mOldFile.exists()) {
                mPersistentState.mapValue().putAll(readStateFromFile(mOldFile, true).mapValue());

            } else {
                if (DEBUG) LOGGER.info("Persistent state file " + mFile + " doesn't yet exist.");
            }

        } else {
            CborMap map;

            try {
                map = readStateFromFile(mFile, false);

            } catch (IOException | CorruptPersistentStateException x) {
                if (!mOldFile.exists()) {
                    throw x;
                }

                try {
                    LOGGER.warning("Attempting to recover from " + mOldFile);
                    map = readStateFromFile(mOldFile, true);
                } catch (IOException | CorruptPersistentStateException ignored) {
                    // Throw the first exception, since that was closer to the
                    // original error that we want to report.
                    throw x;
                }
            }

            mPersistentState.mapValue().putAll(map.mapValue());
        }
    }

    private CborMap readStateFromFile(File file, boolean newOnEmpty)
            throws IOException, CorruptPersistentStateException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            final CborReader reader = CborReader.createFromInputStream(inputStream);

            final CborObject object = reader.readDataItem();

            if (!(object instanceof CborMap)) {
                LOGGER.warning(
                        "Persistent state file "
                                + file
                                + " was corrupted: Invalid top-level object");
                throw new CorruptPersistentStateException("Invalid top level object");
            }

            if (DEBUG) LOGGER.info("Loaded persistent state from " + file + ": "
                    + object.toString(0));

            return (CborMap) object;

        } catch (NoSuchElementException x) {
            String explanation = "Persistent state file " + file + " was empty";

            LOGGER.warning(explanation);

            if (newOnEmpty) {
                return CborMap.create();
            } else {
                throw new CorruptPersistentStateException(explanation, x);
            }

        } catch (CborParseException x) {
            String explanation = "Persistent state file " + file + " was corrupted: " + x;
            LOGGER.warning(explanation);
            throw new CorruptPersistentStateException(explanation, x);

        } catch (CborRuntimeException x) {
            if (x.getCause() instanceof IOException) {
                throw (IOException) x.getCause();
            }
            throw new CorruptPersistentStateException(x);
        }
    }

    private void writeStateToFile() throws IOException {
        synchronized (mFile) {
            final CborMap state;

            synchronized (mPersistentState) {
                mWritePending.set(false);
                state = mPersistentState.copy();
            }

            if (DEBUG) LOGGER.info("Writing persistent state to " + mTempFile);

            try (FileOutputStream outputStream = new FileOutputStream(mTempFile, false)) {
                CborWriter.createFromOutputStream(outputStream)
                        .writeTag(CborTag.SELF_DESCRIBE_CBOR)
                        .writeDataItem(state);
                outputStream.flush();
            }

            if (DEBUG) LOGGER.info("Moving " + mTempFile + " to " + mFile);

            // Try a straightforward rename first. If it works it seems like the most safe approach.
            if (!mTempFile.renameTo(mFile)) {
                // That didn't work, presumably because mFile still exists.
                // let's try a more elaborate approach where we move mFile
                // out of the way first (to mOldFile) and then copy mTempFile over:

                if (mOldFile.exists()) {
                    if (!mOldFile.delete()) {
                        LOGGER.warning("Unable to delete " + mOldFile);
                    }
                }

                if (mFile.exists()) {
                    if (!mFile.renameTo(mOldFile)) {
                        throw new IOException("Can't atomically update " + mFile);
                    }
                }

                if (!mTempFile.renameTo(mFile)) {
                    boolean rollbackSuccessful = mOldFile.renameTo(mFile);
                    throw new IOException(
                            "Can't atomically update "
                                    + mFile
                                    + (rollbackSuccessful
                                            ? ", Rollback successful"
                                            : ", ROLLBACK FAILED"));
                }

                if (!mOldFile.delete()) {
                    LOGGER.warning("Unable to delete " + mOldFile);
                }
            }

            if (DEBUG) LOGGER.info("Persistent state has been saved to " + mFile);
        }
    }

    private void scheduleWriteStateToFile() {
        if (mWritePending.compareAndSet(false, true)) {
            mExecutor.schedule(
                    () -> {
                        try {
                            if (mWritePending.compareAndSet(true, false)) {
                                writeStateToFile();
                            }
                        } catch (IOException x) {
                            // Not much we can do from this context other than log this.
                            LOGGER.log(Level.SEVERE, "Unable to write state", x);
                        }
                    },
                    ASYNC_SAVE_DELAY_MS,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void startManaging(
            String objectName, PersistentStateInterface objectInstance) {
        boolean needsSave = false;

        synchronized (mManagedObjects) {
            mManagedObjects.put(objectName, objectInstance);
        }

        synchronized (mPersistentState) {
            CborObject obj = null;
            if (mPersistentState.containsKey(objectName)) {
                obj = mPersistentState.get(objectName);
            }

            try {
                if (obj instanceof CborMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = ((CborMap) obj).toJavaObject(Map.class);
                    objectInstance.initWithPersistentState(map);
                } else {
                    objectInstance.initWithPersistentState(ImmutableMap.of());
                    needsSave = true;
                }
            } catch (CborConversionException e) {
                LOGGER.severe("Caught exception while starting to manage " + objectName + ": " + e);
                throw new AssertionError(e);
            }
        }

        objectInstance.setPersistentStateListener(
                new PersistentStateListener() {
                    @Override
                    public void onSavePersistentState(
                            PersistentStateInterface persistentStateInterface) {
                        refresh(objectName);
                        scheduleWriteStateToFile();
                    }

                    @Override
                    public void onSavePersistentStateBlocking(
                            PersistentStateInterface persistentStateInterface) throws IOException {
                        refresh(objectName);
                        writeStateToFile();
                    }
                });

        refresh(objectName);

        if (needsSave) {
            scheduleWriteStateToFile();
        }
    }

    @Override
    public synchronized void stopManaging(String objectName) {
        refresh(objectName);
        synchronized (mManagedObjects) {
            PersistentStateInterface objectInstance = mManagedObjects.get(objectName);
            objectInstance.setPersistentStateListener(null);
            mManagedObjects.remove(objectName);
        }
    }

    @Override
    public void refresh(String objectName) {
        final PersistentStateInterface obj;

        synchronized (mManagedObjects) {
            obj = mManagedObjects.get(objectName);
        }

        if (obj != null) {
            final Map<String, Object> stateJava = obj.copyPersistentState();

            try {
                final CborMap stateCbor = CborMap.createFromJavaObject(stateJava);

                synchronized (mPersistentState) {
                    mPersistentState.put(objectName, stateCbor);
                }
            } catch (CborConversionException e) {
                LOGGER.severe(
                        "Unable to serialize persistent state of \"" + objectName + "\": " + e);
                LOGGER.severe(
                        "This is most likely a bug in <" + obj + ">. State was: " + stateJava);
                throw new CborRuntimeException(e);
            }
        }
    }

    @Override
    public void refresh() {
        synchronized (mManagedObjects) {
            mManagedObjects.keySet().forEach(this::refresh);
        }
    }

    @Override
    public void reset(String objectName) {
        synchronized (mPersistentState) {
            mPersistentState.remove(objectName);
        }
    }

    @Override
    public void reset() {
        synchronized (mPersistentState) {
            mPersistentState.clear();
        }
    }

    @Override
    public void flush() throws IOException {
        writeStateToFile();
    }

    private void closeNoFlush() {
        mExecutor.shutdownNow();
        try {
            mExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        closeNoFlush();
    }
}

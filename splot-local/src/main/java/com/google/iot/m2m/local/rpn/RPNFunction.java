/*
 * Copyright (C) 2019 Google Inc.
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
package com.google.iot.m2m.local.rpn;

import com.google.iot.m2m.base.InvalidValueException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RPNFunction implements Function<Object, Object> {
    final RPNContext mContext;
    RPNOperation mOperation = null;
    Runnable mListener = null;

    RPNFunction(RPNContext context, RPNOperation operation) {
        mContext = context;
        mOperation = operation;
    }

    private final static RPNFunction sIdentity = new RPNFunction(new RPNContext(), RPNOperation.NOOP);
    public static RPNFunction identity() {
        return sIdentity;
    }

    @Override
    public Object apply(Object o) {
        RPNStack stack = new RPNStack();

        stack.push(o);

        try {
            if (mOperation != null) {
                mOperation.perform(mContext, stack);
            }
        } catch (InvalidValueException e) {
            throw new RPNInvalidTypeForOperatorException(e);
        }

        if (stack.isEmpty()) {
            o = RPNContext.STOP;
        } else {
            o = stack.pop();
        }

        return o;
    }

    void didChange() {
        Runnable listener = mListener;
        if (listener != null) {
            listener.run();
        }
    }

    void setListener(Runnable listener) {
        mListener = listener;
    }
}

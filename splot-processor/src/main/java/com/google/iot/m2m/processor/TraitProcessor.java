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
package com.google.iot.m2m.processor;

import com.google.auto.service.AutoService;
import com.google.iot.m2m.annotation.Trait;
import com.squareup.javapoet.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(Processor.class)
public class TraitProcessor extends AbstractProcessor {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER =
            Logger.getLogger(AbstractProcessor.class.getCanonicalName());

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private ProcessingEnvironment mProcessingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mProcessingEnv = processingEnv;
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new LinkedHashSet<>();
        annotataions.add(Trait.class.getCanonicalName());
        return annotataions;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public Set<TypeElement> getTraitSubclasses(RoundEnvironment roundEnv) {
        Set<TypeElement> elements = new LinkedHashSet<>();
        for (Element element : roundEnv.getRootElements()) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            if (!Trait.class
                    .getCanonicalName()
                    .equals(((TypeElement) element).getSuperclass().toString())) {
                continue;
            }
            elements.add((TypeElement) element);
        }
        return elements;
    }

    private void processTrait(TypeElement type) {
        ClassName traitClass = (ClassName) TypeName.get(type.asType());
        LocalTraitClassBuilder builder = new LocalTraitClassBuilder(mProcessingEnv, traitClass);

        for (Element element : type.getEnclosedElements()) {
            if (!(element instanceof VariableElement)) {
                continue;
            }

            VariableElement variableElement = (VariableElement) element;

            if ("TRAIT_SUPPORTS_CHILDREN".equals(variableElement.getSimpleName().toString())) {
                builder.setSupportsChildren(
                        Boolean.TRUE.equals(variableElement.getConstantValue()));
                continue;
            }

            if (LocalTraitClassBuilder.isElementTraitMethod(variableElement)) {
                builder.addTraitMethod(variableElement);

            } else if (LocalTraitClassBuilder.isElementTraitProperty(variableElement)) {
                builder.addTraitProperty(variableElement);
            }
        }

        try {
            JavaFile.builder(
                            elementUtils.getPackageOf(type).getQualifiedName().toString(),
                            builder.build())
                    .build()
                    .writeTo(filer);

        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Caught exception: " + e, type);
            e.printStackTrace();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        boolean claimed =
                (annotations.size() == 1
                        && annotations
                                .iterator()
                                .next()
                                .getQualifiedName()
                                .contentEquals(Trait.class.getCanonicalName()));
        if (claimed) {
            process(roundEnv);
            return true;
        } else {
            return false;
        }
    }

    private void abortWithError(String message, @Nullable Element element) {
        if (element == null) {
            messager.printMessage(Diagnostic.Kind.ERROR, message);
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, message, element);
        }
        throw new AbortProcessingException();
    }

    private void process(RoundEnvironment roundEnv) {
        try {
            for (Element element : roundEnv.getElementsAnnotatedWith(Trait.class)) {
                if (element.getKind() != ElementKind.CLASS) {
                    abortWithError("@Trait can only be applied to classes.", element);
                }
                processTrait((TypeElement) element);
            }

        } catch (AbortProcessingException ignored) {
        }
    }
}

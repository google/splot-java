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

import com.google.iot.m2m.annotation.Method;
import com.google.iot.m2m.annotation.Property;
import com.google.iot.m2m.base.*;
import com.squareup.javapoet.*;
import java.util.*;
import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import org.checkerframework.checker.nullness.qual.Nullable;

class LocalTraitClassBuilder {
    private static final AnnotationSpec GENERATED_BY =
            AnnotationSpec.builder(Generated.class)
                    .addMember("value", "\"$L\"", LocalTraitClassBuilder.class.getCanonicalName())
                    .build();

    private static final String JAVADOC_EXCLUDE = " {@hide} ";

    private static final TypeName sGenericDictionaryName =
            ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    TypeName.get(String.class),
                    TypeName.get(Object.class));

    private static final TypeVariableName sGenericName = TypeVariableName.get("T");
    private static final TypeVariableName sUnknownName = TypeVariableName.get("?");

    private static final ClassName sPropertyKeyName = ClassName.get(PropertyKey.class);
    private static final ClassName sMethodKeyName = ClassName.get(MethodKey.class);
    private static final ClassName sTypedKeyName = ClassName.get(TypedKey.class);

    private static final TypeName sGenericPropertyKeyName =
            ParameterizedTypeName.get(sPropertyKeyName, sGenericName);
    private static final TypeName sUnknownPropertyKeyName =
            ParameterizedTypeName.get(sPropertyKeyName, sUnknownName);
    private static final TypeName sGenericMethodKeyName =
            ParameterizedTypeName.get(sMethodKeyName, sGenericName);
    private static final TypeName sUnknownMethodKeyName =
            ParameterizedTypeName.get(sMethodKeyName, sUnknownName);
    private static final TypeName sUnknownTypedKeyName =
            ParameterizedTypeName.get(sTypedKeyName, sUnknownName);

    private static final TypeName sHashMapTypedKeyName =
            ParameterizedTypeName.get(
                    ClassName.get(HashMap.class),
                    ClassName.get(String.class),
                    sUnknownTypedKeyName);

    private static final TypeName sHashSetPropertyKeyName =
            ParameterizedTypeName.get(ClassName.get(HashSet.class), sUnknownPropertyKeyName);

    private static final TypeName sHashSetMethodKeyName =
            ParameterizedTypeName.get(ClassName.get(HashSet.class), sUnknownMethodKeyName);

    private final ClassName mTraitClass;
    private final TypeSpec.Builder mClassBuilder;

    private final MethodSpec.Builder mConstructorBuilder;
    private final CodeBlock.Builder mStaticConstructorBuilder;
    private final MethodSpec.Builder mInvokeMethodBuilder;

    private final MethodSpec.Builder mGetPropertyBuilder;
    private final MethodSpec.Builder mSetPropertyBuilder;
    private final MethodSpec.Builder mCanSavePropertyBuilder;
    private final MethodSpec.Builder mCanTransitionPropertyBuilder;
    private final MethodSpec.Builder mSanitizeValueForPropertyKeyBuilder;
    private final MethodSpec.Builder mGetSupportedPropertyKeysBuilder;
    private final MethodSpec.Builder mGetSupportedMethodKeysBuilder;
    private final MethodSpec.Builder mLookupKeyByNameBuilder;

    private final ProcessingEnvironment mProcessingEnvironment;

    private boolean mSupportsChildren = false;

    LocalTraitClassBuilder(ProcessingEnvironment processingEnv, ClassName traitClass) {
        mProcessingEnvironment = processingEnv;

        mTraitClass = traitClass;

        final String className = "Local" + traitClass.simpleName();

        mClassBuilder =
                TypeSpec.classBuilder(className)
                        .addModifiers(Modifier.ABSTRACT)
                        .addSuperinterface(ClassName.get(LocalTrait.class))
                        .addJavadoc(
                                "Low-level package-private superclass for {@link $T.AbstractLocalTrait}.\n",
                                traitClass)
                        .addJavadoc(
                                "You should subclass {@link $T.AbstractLocalTrait} instead of this class.\n",
                                traitClass)
                        .addAnnotation(GENERATED_BY);

        mClassBuilder.addField(
                FieldSpec.builder(ClassName.get(LocalTrait.Callback.class), "mCallback")
                        .addModifiers(Modifier.PRIVATE)
                        .initializer("null")
                        .build());

        mClassBuilder.addField(
                FieldSpec.builder(sHashMapTypedKeyName, "mTypedKeyLookup")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addJavadoc("Cache of all the typed keys supported by this trait.\n")
                        .initializer("new HashMap<>()")
                        .build());

        mClassBuilder.addField(
                FieldSpec.builder(sHashSetPropertyKeyName, "mSupportedProperties")
                        .addModifiers(Modifier.PRIVATE)
                        .addJavadoc(
                                "Cache of the property keys supported by this instance of the trait.\n")
                        .initializer("null")
                        .build());

        mClassBuilder.addField(
                FieldSpec.builder(sHashSetMethodKeyName, "mSupportedMethods")
                        .addModifiers(Modifier.PRIVATE)
                        .addJavadoc(
                                "Cache of the method keys supported by this instance of the trait.\n")
                        .initializer("null")
                        .build());

        mClassBuilder.addMethod(
                MethodSpec.methodBuilder("isMethodOverridden")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .returns(TypeName.BOOLEAN)
                        .addParameter(Class.class, "base")
                        .addParameter(Class.class, "subject")
                        .addParameter(String.class, "method")
                        .beginControlFlow("try")
                        .addStatement(
                                "return !subject.getDeclaredMethod(method).getDeclaringClass().equals(base)")
                        .endControlFlow()
                        .beginControlFlow("catch ($T ignored)", NoSuchMethodException.class)
                        .beginControlFlow(
                                "for ($T m : subject.getDeclaredMethods())",
                                java.lang.reflect.Method.class)
                        .beginControlFlow("if (m.getName().equals(method))")
                        .addStatement("return !m.getDeclaringClass().equals(base)")
                        .endControlFlow()
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("return false")
                        .build());

        mClassBuilder.addMethod(
                MethodSpec.methodBuilder("isMethodOverridden")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .returns(TypeName.BOOLEAN)
                        .addParameter(String.class, "method")
                        .addStatement(
                                "return isMethodOverridden($L.class, getClass(), method)",
                                className)
                        .build());

        mClassBuilder.addMethod(
                MethodSpec.methodBuilder("setCallback")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(Override.class)
                        .addParameter(LocalTrait.Callback.class, "cb")
                        .addStatement("mCallback = cb")
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .build());

        mConstructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

        mStaticConstructorBuilder = CodeBlock.builder();

        mInvokeMethodBuilder =
                MethodSpec.methodBuilder("invokeMethod")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addAnnotation(Nullable.class)
                        .addException(MethodException.class)
                        .addException(TechnologyException.class)
                        .addTypeVariable(sGenericName)
                        .addParameter(sGenericMethodKeyName, "key")
                        .addParameter(sGenericDictionaryName, "args")
                        .returns(sGenericName)
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("Object ret = null")
                        .addStatement("if (false) ret = null")
                        .addCode("else ");

        mGetPropertyBuilder =
                MethodSpec.methodBuilder("getValueForPropertyKey")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addAnnotation(Nullable.class)
                        .addException(PropertyWriteOnlyException.class)
                        .addException(PropertyNotFoundException.class)
                        .addException(TechnologyException.class)
                        .addTypeVariable(sGenericName)
                        .addParameter(sGenericPropertyKeyName, "key")
                        .returns(sGenericName)
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("Object ret = null")
                        .addStatement(
                                "if ($T.META_TRAIT_URI.equals(key)) ret = $T.TRAIT_URI",
                                mTraitClass,
                                mTraitClass)
                        .addCode("else ");

        mSetPropertyBuilder =
                MethodSpec.methodBuilder("setValueForPropertyKey")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addException(PropertyReadOnlyException.class)
                        .addException(PropertyNotFoundException.class)
                        .addException(InvalidPropertyValueException.class)
                        .addException(BadStateForPropertyValueException.class)
                        .addException(TechnologyException.class)
                        .addTypeVariable(sGenericName)
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addParameter(sGenericPropertyKeyName, "key")
                        .addParameter(
                                ParameterSpec.builder(sGenericName, "value")
                                        .addAnnotation(Nullable.class)
                                        .build())
                        .returns(TypeName.VOID);

        mCanSavePropertyBuilder =
                MethodSpec.methodBuilder("onCanSaveProperty")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(sUnknownPropertyKeyName, "key")
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .returns(TypeName.BOOLEAN)
                        .addStatement("boolean ret = false");

        mCanTransitionPropertyBuilder =
                MethodSpec.methodBuilder("onCanTransitionProperty")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(sUnknownPropertyKeyName, "key")
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .returns(TypeName.BOOLEAN)
                        .addStatement("boolean ret = false");

        mSanitizeValueForPropertyKeyBuilder =
                MethodSpec.methodBuilder("sanitizeValueForPropertyKey")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addAnnotation(Nullable.class)
                        .addException(PropertyOperationUnsupportedException.class)
                        .addException(PropertyNotFoundException.class)
                        .addException(InvalidPropertyValueException.class)
                        .addException(TechnologyException.class)
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addTypeVariable(sGenericName)
                        .addParameter(sGenericPropertyKeyName, "key")
                        .addParameter(
                                ParameterSpec.builder(sGenericName, "value")
                                        .addAnnotation(Nullable.class)
                                        .build())
                        .returns(sGenericName);

        mGetSupportedPropertyKeysBuilder =
                MethodSpec.methodBuilder("getSupportedPropertyKeys")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(
                                ParameterizedTypeName.get(
                                        ClassName.get(Set.class), sUnknownPropertyKeyName))
                        .beginControlFlow("if (mSupportedProperties == null)")
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("mSupportedProperties = new HashSet<>()")
                        .addStatement("mSupportedProperties.add($T.META_TRAIT_URI)", mTraitClass);

        mGetSupportedMethodKeysBuilder =
                MethodSpec.methodBuilder("getSupportedMethodKeys")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(
                                ParameterizedTypeName.get(
                                        ClassName.get(Set.class), sUnknownMethodKeyName))
                        .beginControlFlow("if (mSupportedMethods == null)")
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("mSupportedMethods = new HashSet<>()");

        mLookupKeyByNameBuilder =
                MethodSpec.methodBuilder("lookupKeyByName")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(String.class, "name")
                        .returns(sUnknownTypedKeyName)
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("return mTypedKeyLookup.get(name)");
    }

    public void setSupportsChildren(boolean x) {
        mSupportsChildren = x;
    }

    public static boolean isElementTraitMethod(VariableElement element) {
        return element.getAnnotation(Method.class) != null;
    }

    public static boolean isElementTraitProperty(VariableElement element) {
        return element.getAnnotation(Property.class) != null;
    }

    private void reportError(Element element, String error) {
        mProcessingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, error, element);
    }

    private boolean checkIfTraitMethodIsValid(VariableElement element) {
        if (element.getAnnotation(Method.class) == null) {
            reportError(element, "Method is missing @Method annotation.");
            return false;
        }

        final TypeName typedKeyConstantTypeName = TypeName.get(element.asType());

        if (!(typedKeyConstantTypeName instanceof ParameterizedTypeName)) {
            reportError(element, "Unexpected field type \"" + typedKeyConstantTypeName + "\"");
            return false;
        }

        ParameterizedTypeName paramTypedKeyConstantTypeName =
                ((ParameterizedTypeName) typedKeyConstantTypeName);

        if (!paramTypedKeyConstantTypeName.rawType.equals(sMethodKeyName)) {
            reportError(
                    element,
                    "Unexpected field type "
                            + paramTypedKeyConstantTypeName.rawType
                            + ", expected "
                            + sMethodKeyName);
            return false;
        }

        if (paramTypedKeyConstantTypeName.typeArguments.size() < 1) {
            reportError(element, "Unexpected field type \"" + typedKeyConstantTypeName + "\"");
            return false;
        }

        // TODO : Make sure that the name of the field is proper

        return true;
    }

    private boolean checkIfTraitPropertyIsValid(VariableElement element) {
        if (element.getAnnotation(Property.class) == null) {
            reportError(element, "Property is missing @Property annotation.");
            return false;
        }

        final TypeName typedKeyConstantTypeName = TypeName.get(element.asType());

        if (!(typedKeyConstantTypeName instanceof ParameterizedTypeName)) {
            reportError(element, "Unexpected field type \"" + typedKeyConstantTypeName + "\"");
            return false;
        }

        ParameterizedTypeName paramTypedKeyConstantTypeName =
                ((ParameterizedTypeName) typedKeyConstantTypeName);

        if (!paramTypedKeyConstantTypeName.rawType.equals(sPropertyKeyName)) {
            reportError(
                    element,
                    "Unexpected field type "
                            + paramTypedKeyConstantTypeName.rawType
                            + ", expected "
                            + sPropertyKeyName);
            return false;
        }

        if (paramTypedKeyConstantTypeName.typeArguments.size() < 1) {
            reportError(element, "Unexpected field type \"" + typedKeyConstantTypeName + "\"");
            return false;
        }

        // TODO : Make sure that the name of the field is proper

        return true;
    }

    // For example, converts "STAT_HAPPY_HELPER" to "happyHelper"
    private static String getNameFromTypedKeyConstantName(String typedKeyConstantName) {
        List<String> elements = NameMangle.decodeCapsAndUnderscores(typedKeyConstantName);
        elements.remove(0);
        return NameMangle.encodePartialCamelCase(elements);
    }

    // For example, converts "STAT_HAPPY_HELPER" to "HappyHelper"
    private static String fullCamelCaseNameFromTypedKeyConstantName(String typedKeyConstantName) {
        List<String> elements = NameMangle.decodeCapsAndUnderscores(typedKeyConstantName);
        elements.remove(0);
        return NameMangle.encodeFullCamelCase(elements);
    }

    public void addTraitMethod(VariableElement element) {
        if (!checkIfTraitMethodIsValid(element)) {
            return;
        }

        final Method info = element.getAnnotation(Method.class);
        final String keyConstantName = element.getSimpleName().toString();
        final String methodFullName =
                "onInvoke" + fullCamelCaseNameFromTypedKeyConstantName(keyConstantName);

        final ParameterizedTypeName keyTypeName =
                (ParameterizedTypeName) TypeName.get(element.asType());
        final TypeName returnTypeName = keyTypeName.typeArguments.get(0);

        mStaticConstructorBuilder.addStatement(
                "mTypedKeyLookup.put($T.$L.getName(), $T.$L)",
                mTraitClass,
                keyConstantName,
                mTraitClass,
                keyConstantName);

        MethodSpec.Builder methodBuilder =
                MethodSpec.methodBuilder(methodFullName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(sGenericDictionaryName, "args")
                        .addAnnotation(Nullable.class)
                        .addException(InvalidMethodArgumentsException.class)
                        .returns(returnTypeName);

        final boolean isRequired = (info.value() & Method.REQUIRED) == Method.REQUIRED;

        if (isRequired) {
            methodBuilder.addModifiers(Modifier.ABSTRACT);
        } else {
            methodBuilder.addStatement("return null");
        }

        mClassBuilder.addMethod(methodBuilder.build());

        mInvokeMethodBuilder
                .addStatement(
                        "if ($T.$L.equals(key)) ret = $L(args)",
                        mTraitClass,
                        keyConstantName,
                        methodFullName)
                .addCode("else ");

        mGetSupportedMethodKeysBuilder.addStatement(
                "mSupportedMethods.add($T.$L)", mTraitClass, keyConstantName);
    }

    public void addTraitProperty(VariableElement element) {
        if (!checkIfTraitPropertyIsValid(element)) {
            return;
        }

        final Property info = element.getAnnotation(Property.class);
        final String keyConstantName = element.getSimpleName().toString();

        if ("META_TRAIT_URI".equals(keyConstantName)) {
            // Skip the Trait URI property, since we handle that internally.
            return;
        }

        final String propertyFullName = fullCamelCaseNameFromTypedKeyConstantName(keyConstantName);

        final ParameterizedTypeName keyTypeName =
                (ParameterizedTypeName) TypeName.get(element.asType());
        final TypeName fieldTypeName = keyTypeName.typeArguments.get(0);

        final String getMethodName = "onGet" + propertyFullName;
        final String setMethodName = "onSet" + propertyFullName;
        final String sanitizeMethodName = "onSanitize" + propertyFullName;
        final String didChangeMethodName = "didChange" + propertyFullName;

        mStaticConstructorBuilder.addStatement(
                "mTypedKeyLookup.put($T.$L.getName(), $T.$L)",
                mTraitClass,
                keyConstantName,
                mTraitClass,
                keyConstantName);

        if ((info.value() & Property.GET) == Property.GET) {
            MethodSpec.Builder builder =
                    MethodSpec.methodBuilder(getMethodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addException(TechnologyException.class)
                            .addAnnotation(Nullable.class)
                            .addJavadoc(
                                    "Gets the value for {@link $T#$L}.\n",
                                    mTraitClass,
                                    keyConstantName)
                            .addJavadoc(
                                    "@return the current value of {@code "
                                            + propertyFullName
                                            + "}\n")
                            .returns(fieldTypeName);

            if ((info.value() & Property.GET_REQUIRED) != Property.GET_REQUIRED) {
                builder.addStatement("return null");

                if ((info.value() & Property.SET_REQUIRED) != Property.SET_REQUIRED) {
                    mGetSupportedPropertyKeysBuilder.addStatement(
                            "if (isMethodOverridden($S)) mSupportedProperties.add($T.$L)",
                            getMethodName,
                            mTraitClass,
                            keyConstantName);
                }
            } else {
                builder.addModifiers(Modifier.ABSTRACT);
            }

            if ((info.value() & Property.SET) == Property.SET) {
                builder.addJavadoc("@see #$L\n", setMethodName);
            }
            builder.addJavadoc("@see $T#$L\n", mTraitClass, keyConstantName);
            mClassBuilder.addMethod(builder.build());

            mGetPropertyBuilder
                    .addStatement(
                            "if ($T.$L.equals(key)) ret = $L()",
                            mTraitClass,
                            keyConstantName,
                            getMethodName)
                    .addCode("else ");
        }

        if ((info.value() & Property.SET) == Property.SET) {
            MethodSpec.Builder builder = MethodSpec.methodBuilder(setMethodName);
            builder.addModifiers(Modifier.PUBLIC)
                    .addException(PropertyReadOnlyException.class)
                    .addException(InvalidPropertyValueException.class)
                    .addException(BadStateForPropertyValueException.class)
                    .addException(TechnologyException.class)
                    .addParameter(
                            ParameterSpec.builder(fieldTypeName, "value")
                                    .addAnnotation(Nullable.class)
                                    .build())
                    .returns(TypeName.VOID)
                    .addJavadoc(
                            "Changes the value for {@link $T#$L}.\n", mTraitClass, keyConstantName);

            if ((info.value() & Property.CHANGE) == Property.CHANGE) {
                builder.addJavadoc("If the implementation ultimately changes the value of the\n")
                        .addJavadoc(
                                "property, it <em>must</em> call {@link #$L}\n",
                                didChangeMethodName)
                        .addJavadoc(
                                "to notify the higher layers that the value has indeed changed.\n")
                        .addJavadoc("@see #$L\n", didChangeMethodName);
            }

            mSanitizeValueForPropertyKeyBuilder.beginControlFlow(
                    "if ($T.$L.equals(key))", mTraitClass, keyConstantName);

            mSetPropertyBuilder.beginControlFlow(
                    "if ($T.$L.equals(key))", mTraitClass, keyConstantName);

            if ("java.lang.Boolean".equals(fieldTypeName.toString())) {
                // Boolean types don't get sanitization hooks.
                mSanitizeValueForPropertyKeyBuilder
                        .beginControlFlow("try")
                        .addStatement(
                                "return key.cast($T.$L.coerce(value))",
                                mTraitClass,
                                keyConstantName)
                        .endControlFlow()
                        .beginControlFlow(
                                "catch ($T x)", ClassName.get(InvalidValueException.class))
                        .addStatement(
                                "throw new $T(x)",
                                ClassName.get(InvalidPropertyValueException.class))
                        .endControlFlow();

                mSetPropertyBuilder
                        .beginControlFlow("try")
                        .addStatement(
                                "$L($T.$L.coerce(value))",
                                setMethodName,
                                mTraitClass,
                                keyConstantName)
                        .endControlFlow()
                        .beginControlFlow(
                                "catch ($T x)", ClassName.get(InvalidValueException.class))
                        .addStatement(
                                "throw new $T(x)",
                                ClassName.get(InvalidPropertyValueException.class))
                        .endControlFlow();
            } else {
                mClassBuilder.addMethod(
                        MethodSpec.methodBuilder(sanitizeMethodName)
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Nullable.class)
                                .addException(InvalidPropertyValueException.class)
                                .addException(TechnologyException.class)
                                .returns(fieldTypeName)
                                .addJavadoc(
                                        "Sanitizes value for {@link $T#$L}.\n",
                                        mTraitClass,
                                        keyConstantName)
                                .addJavadoc(
                                        "For example, this may be used to force the value to be\n")
                                .addJavadoc(
                                        "between 0.0 and 1.0. The default implementation simply\n")
                                .addJavadoc("returns {@code value} as-is.\n")
                                .addJavadoc("@param value the value to be sanitized\n")
                                .addJavadoc("@return the resulting sanitized value\n")
                                .addJavadoc("@see #$L\n", setMethodName)
                                .addJavadoc("@see $T#$L\n", mTraitClass, keyConstantName)
                                .addParameter(
                                        ParameterSpec.builder(fieldTypeName, "value")
                                                .addAnnotation(Nullable.class)
                                                .build())
                                .addStatement("return value")
                                .build());

                builder.addJavadoc("@see #$L\n", sanitizeMethodName);

                mSanitizeValueForPropertyKeyBuilder
                        .beginControlFlow("try")
                        .addStatement(
                                "return key.cast($L($T.$L.coerce(value)))",
                                sanitizeMethodName,
                                mTraitClass,
                                keyConstantName)
                        .endControlFlow()
                        .beginControlFlow(
                                "catch ($T x)", ClassName.get(InvalidValueException.class))
                        .addStatement(
                                "throw new $T(x)",
                                ClassName.get(InvalidPropertyValueException.class))
                        .endControlFlow();

                mSetPropertyBuilder
                        .beginControlFlow("try")
                        .addStatement(
                                "$L($T.$L.cast(value))",
                                setMethodName,
                                mTraitClass,
                                keyConstantName)
                        .endControlFlow()
                        .beginControlFlow("catch ($T x)", ClassName.get(ClassCastException.class))
                        .addStatement(
                                "throw new $T(x)",
                                ClassName.get(InvalidPropertyValueException.class))
                        .endControlFlow();
            }

            mSanitizeValueForPropertyKeyBuilder.endControlFlow().addCode("else ");

            mSetPropertyBuilder.endControlFlow().addCode("else ");

            if ((info.value() & Property.SET_REQUIRED) != Property.SET_REQUIRED) {
                builder.addStatement(
                        "throw new $T()", ClassName.get(PropertyReadOnlyException.class));
                builder.addJavadoc(
                        "\n@throws $T if not overridden.",
                        ClassName.get(PropertyReadOnlyException.class));

                if ((info.value() & Property.GET_REQUIRED) != Property.GET_REQUIRED) {
                    mGetSupportedPropertyKeysBuilder
                            .beginControlFlow("if (isMethodOverridden($S))", setMethodName)
                            .addStatement(
                                    "mSupportedProperties.add($T.$L)", mTraitClass, keyConstantName)
                            .endControlFlow();
                }

                if ((info.value() & Property.NO_SAVE) != Property.NO_SAVE) {
                    mCanSavePropertyBuilder
                            .addStatement(
                                    "if ($T.$L.equals(key) && isMethodOverridden($S)) ret = true",
                                    mTraitClass,
                                    keyConstantName,
                                    setMethodName)
                            .addCode("else ");
                }

                if ((info.value() & Property.NO_TRANSITION) != Property.NO_TRANSITION) {
                    mCanTransitionPropertyBuilder
                            .addStatement(
                                    "if ($T.$L.equals(key) && isMethodOverridden($S)) ret = true",
                                    mTraitClass,
                                    keyConstantName,
                                    setMethodName)
                            .addCode("else ");
                }
            } else {
                builder.addModifiers(Modifier.ABSTRACT);

                if ((info.value() & Property.NO_SAVE) != Property.NO_SAVE) {
                    mCanSavePropertyBuilder
                            .addStatement(
                                    "if ($T.$L.equals(key)) ret = true",
                                    mTraitClass,
                                    keyConstantName)
                            .addCode("else ");
                }

                if ((info.value() & Property.NO_TRANSITION) != Property.NO_TRANSITION) {
                    mCanTransitionPropertyBuilder
                            .addStatement(
                                    "if ($T.$L.equals(key)) ret = true",
                                    mTraitClass,
                                    keyConstantName)
                            .addCode("else ");
                }
            }

            if ((info.value() & Property.GET) == Property.GET) {
                builder.addJavadoc("@see #$L\n", getMethodName);
            }

            builder.addJavadoc("@see $T#$L\n", mTraitClass, keyConstantName);

            mClassBuilder.addMethod(builder.build());
        }

        if ((info.value() & Property.REQUIRED) != 0) {
            mGetSupportedPropertyKeysBuilder.addStatement(
                    "mSupportedProperties.add($T.$L)", mTraitClass, keyConstantName);
        }

        if ((info.value() & Property.CHANGE) == Property.CHANGE) {
            MethodSpec.Builder didChangeWithParam =
                    MethodSpec.methodBuilder(didChangeMethodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .returns(TypeName.VOID)
                            .addJavadoc(
                                    "Call whenever the value of {@link $T#$L} changes.\n",
                                    mTraitClass,
                                    keyConstantName)
                            .addJavadoc(
                                    "Note that this method is only for reporting that the value\n")
                            .addJavadoc(
                                    "has changed, it itself does not change the actual value of\n")
                            .addJavadoc("the property.\n")
                            .addJavadoc(
                                    "@param value the value that {@link $T#$L} has changed to\n",
                                    mTraitClass,
                                    keyConstantName)
                            .addJavadoc("@see $T#$L\n", mTraitClass, keyConstantName)
                            .addParameter(
                                    ParameterSpec.builder(fieldTypeName, "value")
                                            .addAnnotation(Nullable.class)
                                            .build())
                            .beginControlFlow("if (mCallback != null)")
                            .addStatement(
                                    "mCallback.onPropertyChanged(this, $T.$L, value)",
                                    mTraitClass,
                                    keyConstantName)
                            .endControlFlow();
            if ((info.value() & Property.SET) == Property.SET) {
                didChangeWithParam.addJavadoc("@see #$L\n", setMethodName);
            }
            mClassBuilder.addMethod(didChangeWithParam.build());
        }
    }

    TypeSpec build() {
        mClassBuilder.addMethod(
                MethodSpec.methodBuilder("getTraitId")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(Override.class)
                        .returns(TypeName.get(String.class))
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("return $T.TRAIT_ID", mTraitClass)
                        .build());

        mClassBuilder.addMethod(
                MethodSpec.methodBuilder("getTraitName")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(Override.class)
                        .returns(TypeName.get(String.class))
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("return $T.TRAIT_NAME", mTraitClass)
                        .build());

        mClassBuilder.addMethod(
                MethodSpec.methodBuilder("getTraitUri")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(Override.class)
                        .returns(TypeName.get(String.class))
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("return $T.TRAIT_URI", mTraitClass)
                        .build());

        mClassBuilder.addMethod(
                MethodSpec.methodBuilder("supportsChildren")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(Override.class)
                        .returns(TypeName.BOOLEAN)
                        .addJavadoc(JAVADOC_EXCLUDE)
                        .addStatement("return $T.TRAIT_SUPPORTS_CHILDREN", mTraitClass)
                        .build());

        mInvokeMethodBuilder
                .addStatement(
                        "throw new $T(\"Method not found\")",
                        ClassName.get(MethodNotFoundException.class))
                .addStatement("return key.cast(ret)");

        mGetPropertyBuilder
                .beginControlFlow("if (getSupportedPropertyKeys().contains(key))")
                .addStatement("throw new $T()", ClassName.get(PropertyWriteOnlyException.class))
                .endControlFlow()
                .addStatement("else throw new $T()", ClassName.get(PropertyNotFoundException.class))
                .addStatement("if (ret == null) return null")
                .addStatement("return key.cast(ret)");

        mSetPropertyBuilder
                .beginControlFlow("if (getSupportedPropertyKeys().contains(key))")
                .addStatement("throw new $T()", ClassName.get(PropertyReadOnlyException.class))
                .endControlFlow()
                .addStatement(
                        "else throw new $T()", ClassName.get(PropertyNotFoundException.class));

        mCanSavePropertyBuilder.addStatement("{}").addStatement("return ret");
        mCanTransitionPropertyBuilder.addStatement("{}").addStatement("return ret");

        mSanitizeValueForPropertyKeyBuilder
                .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings.class)
                                .addMember("value", "\"unchecked\"")
                                .build())
                .beginControlFlow("if (getSupportedPropertyKeys().contains(key))")
                .addStatement(
                        "throw new $T()",
                        ClassName.get(PropertyOperationUnsupportedException.class))
                .endControlFlow()
                .addStatement(
                        "else throw new $T()", ClassName.get(PropertyNotFoundException.class));

        mGetSupportedPropertyKeysBuilder
                .endControlFlow()
                .addStatement("return mSupportedProperties");

        mGetSupportedMethodKeysBuilder.endControlFlow().addStatement("return mSupportedMethods");

        if (mSupportsChildren) {
            mClassBuilder.addMethod(
                    MethodSpec.methodBuilder("didAddChild")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.FINAL)
                            .addParameter(Thing.class, "child")
                            .addJavadoc("Call when a child is added to this trait.")
                            .beginControlFlow("if (mCallback != null)")
                            .addStatement("mCallback.onChildAdded(this, child)")
                            .endControlFlow()
                            .build());

            mClassBuilder.addMethod(
                    MethodSpec.methodBuilder("didRemoveChild")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.FINAL)
                            .addParameter(Thing.class, "child")
                            .addJavadoc("Call when a child is added to this trait.")
                            .beginControlFlow("if (mCallback != null)")
                            .addStatement("mCallback.onChildRemoved(this, child)")
                            .endControlFlow()
                            .build());
        } else {
            String javadoc =
                    "This method cannot be overridden and always returns $L because "
                            + "this trait doesn't support children.";
            mClassBuilder.addMethod(
                    MethodSpec.methodBuilder("onCopyChildrenSet")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.FINAL)
                            .addAnnotation(Override.class)
                            .returns(
                                    ParameterizedTypeName.get(
                                            ClassName.get(Set.class),
                                            ClassName.get(Thing.class)))
                            .addJavadoc(javadoc, "{@code null}")
                            .addStatement("return null", ClassName.get(HashSet.class))
                            .build());

            mClassBuilder.addMethod(
                    MethodSpec.methodBuilder("onGetIdForChild")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.FINAL)
                            .addAnnotation(Override.class)
                            .addAnnotation(Nullable.class)
                            .addParameter(Thing.class, "child")
                            .addJavadoc(javadoc, "{@code null}")
                            .returns(ClassName.get(String.class))
                            .addStatement("return null")
                            .build());

            mClassBuilder.addMethod(
                    MethodSpec.methodBuilder("onGetChild")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.FINAL)
                            .addAnnotation(Override.class)
                            .addAnnotation(Nullable.class)
                            .addParameter(String.class, "ignored")
                            .addJavadoc(javadoc, "{@code null}")
                            .returns(TypeName.get(Thing.class))
                            .addStatement("return null")
                            .build());
        }

        return mClassBuilder
                .addMethod(mConstructorBuilder.build())
                .addMethod(mInvokeMethodBuilder.build())
                .addMethod(mGetPropertyBuilder.build())
                .addMethod(mSetPropertyBuilder.build())
                .addMethod(mCanSavePropertyBuilder.build())
                .addMethod(mCanTransitionPropertyBuilder.build())
                .addMethod(mSanitizeValueForPropertyKeyBuilder.build())
                .addMethod(mGetSupportedPropertyKeysBuilder.build())
                .addMethod(mGetSupportedMethodKeysBuilder.build())
                // .addMethod(mLookupKeyByNameBuilder.build())
                .build();
    }
}

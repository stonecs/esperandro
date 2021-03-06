package de.devland.esperandro.processor;/*
 * Copyright 2013 David Kunzler
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import com.squareup.java.JavaWriter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Putter {

    private Map<String, Element> preferenceKeys;

    public Putter() {
        preferenceKeys = new HashMap<String, Element>();
    }

    public boolean isPutter(ExecutableElement method) {
        boolean isPutter = false;
        List<? extends VariableElement> parameters = method.getParameters();
        TypeMirror returnType = method.getReturnType();
        TypeKind returnTypeKind = returnType.getKind();
        if (parameters != null && parameters.size() == 1 && returnTypeKind.equals(TypeKind.VOID) && PreferenceType
                .toPreferenceType(parameters.get(0).asType()) != PreferenceType.NONE) {
            isPutter = true;
        }
        return isPutter;
    }

    public boolean isPutter(Method method) {
        boolean isPutter = false;
        Type[] parameterTypes = method.getGenericParameterTypes();
        if (parameterTypes != null && parameterTypes.length == 1 && method.getReturnType().toString().equals("void")) {
            if (PreferenceType.toPreferenceType(parameterTypes[0]) != PreferenceType.NONE) {
                isPutter = true;
            }
        }

        return isPutter;
    }

    public void createPutterFromModel(ExecutableElement method, JavaWriter writer) throws IOException {
        String valueName = method.getSimpleName().toString();
        String value = valueName;
        preferenceKeys.put(valueName, method);
        TypeMirror parameterType = method.getParameters().get(0).asType();
        PreferenceType preferenceType = PreferenceType.toPreferenceType(parameterType);

        createPutter(writer, valueName, value, preferenceType);
    }


    public void createPutterFromReflection(Method method, Element topLevelInterface,
                                           JavaWriter writer) throws IOException {
        String valueName = method.getName();
        String value = valueName;
        preferenceKeys.put(valueName, topLevelInterface);
        Type parameterType = method.getGenericParameterTypes()[0];
        PreferenceType preferenceType = PreferenceType.toPreferenceType(parameterType);

        createPutter(writer, valueName, value, preferenceType);
    }

    private void createPutter(JavaWriter writer, String valueName, String value,
                              PreferenceType preferenceType) throws IOException {
        writer.emitAnnotation(Override.class);
        writer.beginMethod("void", valueName, Modifier.PUBLIC, preferenceType.getTypeName(), valueName);
        String statementPattern = "preferences.edit().put%s(\"%s\", %s).commit()";
        String methodSuffix = "";
        switch (preferenceType) {
            case INT:
                methodSuffix = "Int";
                break;
            case LONG:
                methodSuffix = "Long";
                break;
            case FLOAT:
                methodSuffix = "Float";
                break;
            case BOOLEAN:
                methodSuffix = "Boolean";
                break;
            case STRING:
                methodSuffix = "String";
                break;
            case STRINGSET:
                methodSuffix = "StringSet";
                break;
            case OBJECT:
                methodSuffix = "String";
                value = String.format("Esperandro.getSerializer().serialize(%s)", valueName);
                break;
        }

        String statement = String.format(statementPattern, methodSuffix, valueName, value);
        writer.emitStatement(statement);
        writer.endMethod();
        writer.emitEmptyLine();
    }

    public Map<String, Element> getPreferenceKeys() {
        return preferenceKeys;
    }


}

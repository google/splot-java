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

import java.util.LinkedList;
import java.util.List;

final class NameMangle {
    private NameMangle() {}

    static List<String> decodeCapsAndUnderscores(String name) {
        List<String> ret = new LinkedList<>();
        int lastCap = 0;
        for (String component : name.split("_")) {
            ret.add(component.toLowerCase());
        }
        return ret;
    }

    static List<String> decodeCamelCase(String name) {
        List<String> ret = new LinkedList<>();
        int lastCap = 0;

        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                ret.add(name.substring(lastCap, i).toLowerCase());
                lastCap = i;
            }
        }
        ret.add(name.substring(lastCap).toLowerCase());
        return ret;
    }

    static String encodeFullCamelCase(List<String> list) {
        StringBuilder sb = new StringBuilder();

        for (String str : list) {
            sb.append(Character.toUpperCase(str.charAt(0)));
            sb.append(str.substring(1));
        }

        return sb.toString();
    }

    static String encodePartialCamelCase(List<String> list) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;

        for (String str : list) {
            if (first) {
                sb.append(Character.toLowerCase(str.charAt(0)));
                first = false;
            } else {
                sb.append(Character.toUpperCase(str.charAt(0)));
            }
            sb.append(str.substring(1));
        }

        return sb.toString();
    }

    static String encodeAllCapsAndUnderscores(List<String> list) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;

        for (String str : list) {
            if (first) {
                first = false;
            } else {
                sb.append("_");
            }
            sb.append(str.toUpperCase());
        }

        return sb.toString();
    }
}

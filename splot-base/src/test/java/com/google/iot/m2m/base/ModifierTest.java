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
package com.google.iot.m2m.base;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModifierTest {

    @Test
    void convertToQuery() {
        assertEquals("inc&d=2.00&tt&all",
                Modifier.convertToQuery(
                        Modifier.increment(),
                        Modifier.duration(2),
                        Modifier.transitionTarget(),
                        Modifier.all()));

        assertEquals("tog", Modifier.convertToQuery(Modifier.toggle()));
        assertEquals("ins", Modifier.convertToQuery(Modifier.insert()));
        assertEquals("rem", Modifier.convertToQuery(Modifier.remove()));

        assertThrows(InvalidModifierListException.class,
                ()->Modifier.convertToQuery(
                        Modifier.increment(),
                        Modifier.duration(2),
                        Modifier.toggle(),
                        Modifier.transitionTarget(),
                        Modifier.all()));

        assertThrows(IllegalArgumentException.class,
                ()->Modifier.convertToQuery(
                        Modifier.increment(),
                        Modifier.duration(-2),
                        Modifier.transitionTarget(),
                        Modifier.all()));
    }

    @Test
    void convertFromQuery() {
        assertEquals(
                Arrays.asList(
                    Modifier.insert(),
                    Modifier.duration(2),
                    Modifier.transitionTarget(),
                    Modifier.all()),
                Arrays.asList(Modifier.convertFromQuery("ins&d=2.00&tt&all")));

        assertEquals(
                Arrays.asList(Modifier.remove()),
                Arrays.asList(Modifier.convertFromQuery("rem")));

        assertEquals(
                Arrays.asList(Modifier.toggle()),
                Arrays.asList(Modifier.convertFromQuery("tog")));

        assertEquals(
                Arrays.asList(Modifier.increment()),
                Arrays.asList(Modifier.convertFromQuery("inc")));

        assertThrows(InvalidModifierListException.class,
                ()->Modifier.convertFromQuery("inc&tog&d=2.00&tt&all"));

        assertThrows(InvalidModifierListException.class,
                ()->Modifier.convertFromQuery("inc&d=-2&tt&all"));

        assertThrows(InvalidModifierListException.class,
                ()->Modifier.convertFromQuery("d=dog"));
    }
}

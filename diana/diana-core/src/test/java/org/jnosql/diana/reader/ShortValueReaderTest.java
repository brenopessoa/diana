/*
 *
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 *
 */
package org.jnosql.diana.reader;

import jakarta.nosql.ValueReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ShortValueReaderTest {


    private ValueReader valueReader;

    @BeforeEach
    public void init() {
        valueReader = new ShortValueReader();
    }

    @Test
    public void shouldValidateCompatibility() {
        assertTrue(valueReader.isCompatible(Short.class));
        assertFalse(valueReader.isCompatible(AtomicBoolean.class));
        assertFalse(valueReader.isCompatible(Boolean.class));
    }

    @Test
    public void shouldConvert() {
        Integer number = 10;
        assertEquals(Short.valueOf((short) 10), valueReader.read(Integer.class, number));
        assertEquals(Short.valueOf((short) 10), valueReader.read(Integer.class, 10.00));
        assertEquals(Short.valueOf((short) 10), valueReader.read(Integer.class, "10"));
    }

}
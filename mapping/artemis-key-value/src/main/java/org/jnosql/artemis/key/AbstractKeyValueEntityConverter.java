/*
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
 */
package org.jnosql.artemis.key;

import org.jnosql.artemis.AttributeConverter;
import org.jnosql.artemis.Converters;
import org.jnosql.artemis.IdNotFoundException;
import org.jnosql.artemis.reflection.ClassMapping;
import org.jnosql.artemis.reflection.ClassMappings;
import org.jnosql.artemis.reflection.FieldMapping;
import org.jnosql.diana.api.Value;
import org.jnosql.diana.api.key.KeyValueEntity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Template method to {@link KeyValueEntityConverter}
 */
public abstract class AbstractKeyValueEntityConverter implements KeyValueEntityConverter {

    protected abstract ClassMappings getClassMappings();

    protected abstract Converters getConverters();

    @Override
    public KeyValueEntity<?> toKeyValue(Object entityInstance) {
        requireNonNull(entityInstance, "Object is required");
        Class<?> clazz = entityInstance.getClass();
        ClassMapping mapping = getClassMappings().get(clazz);

        FieldMapping key = getId(clazz, mapping);

        Object value = key.read(entityInstance);
        requireNonNull(value, String.format("The key field %s is required", key.getName()));

        return KeyValueEntity.of(value, entityInstance);
    }

    @Override
    public <T> T toEntity(Class<T> entityClass, KeyValueEntity<?> entity) {

        Value value = entity.getValue();
        T bean = value.get(entityClass);
        if (Objects.isNull(bean)) {
            return null;
        }

        FieldMapping key = getId(entityClass, getClassMappings().get(entityClass));
        if (key.getConverter().isPresent()) {
            AttributeConverter attributeConverter = getConverters().get(key.getConverter().get());
            Object keyConverted = attributeConverter.convertToEntityAttribute(entity.getKey());
            key.write(bean, keyConverted);
        } else {
            key.write(bean, entity.getKey(key.getNativeField().getType()));
        }

        return bean;
    }

    private FieldMapping getId(Class<?> clazz, ClassMapping mapping) {
        return mapping.getId().orElseThrow(() -> IdNotFoundException.newInstance(clazz));
    }
}

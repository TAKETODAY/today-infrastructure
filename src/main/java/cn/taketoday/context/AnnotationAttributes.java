/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author TODAY <br>
 *         2018-12-14 13:45
 * @since 2.1.1
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

    private static final String UNKNOWN = "unknown";

    private final Class<? extends Annotation> annotationType;

    private final String displayName;

    public AnnotationAttributes() {
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    public AnnotationAttributes(int initialCapacity) {
        super(initialCapacity, 1.0f);
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    public AnnotationAttributes(Class<? extends Annotation> annotationType, int initialCapacity) {
        super(initialCapacity, 1.0f);
        Objects.requireNonNull(annotationType, "'annotationType' must not be null");
        this.annotationType = annotationType;
        this.displayName = annotationType.getName();
    }

    public AnnotationAttributes(Map<String, Object> map) {
        super(map);
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    public AnnotationAttributes(AnnotationAttributes other) {
        super(other);
        this.annotationType = other.annotationType;
        this.displayName = other.displayName;
    }

    public AnnotationAttributes(Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(annotationType, "'annotationType' must not be null");

        this.annotationType = annotationType;
        this.displayName = annotationType.getName();
    }

    public Class<? extends Annotation> annotationType() {
        return this.annotationType;
    }

    public String getString(String attributeName) {
        return getAttribute(attributeName, String.class);
    }

    public String[] getStringArray(String attributeName) {
        return getAttribute(attributeName, String[].class);
    }

    public boolean getBoolean(String attributeName) {
        return getAttribute(attributeName, Boolean.class);
    }

    @SuppressWarnings("unchecked")
    public <N extends Number> N getNumber(String attributeName) {
        return (N) getAttribute(attributeName, Number.class);
    }

    @SuppressWarnings("unchecked")
    public <E extends Enum<?>> E getEnum(String attributeName) {
        return (E) getAttribute(attributeName, Enum.class);
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
        return (A[]) getAttribute(attributeName, Array.newInstance(annotationType, 0).getClass());
    }

    /**
     * Get the value of attribute name and cast to target type
     * 
     * @param attributeName
     *            The attribute name
     * @param expectedType
     *            target type
     * @return
     */
    public <T> T getAttribute(String attributeName, Class<T> expectedType) {

        Objects.requireNonNull(attributeName, "'attributeName' must not be null or empty");

        Object value = get(attributeName); // get value
        assertAttributePresence(attributeName, value);

        if (!expectedType.isInstance(value) && expectedType.isArray() && expectedType.getComponentType().isInstance(value)) {
            Object array = Array.newInstance(expectedType.getComponentType(), 1);
            Array.set(array, 0, value);
            value = array;
        }
        assertAttributeType(attributeName, value, expectedType);
        return expectedType.cast(value);
    }

    /**
     * @param attributeName
     * @param attributeValue
     */
    private void assertAttributePresence(String attributeName, Object attributeValue) {
        Objects.requireNonNull(attributeValue, String.format(//
                "Attribute '%s' not found in attributes for annotation [%s]",
                attributeName, this.displayName//
        ));
    }

    /**
     * 
     * @param attributeName
     * @param attributeValue
     * @param expectedType
     */
    private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
        if (!expectedType.isInstance(attributeValue)) {
            throw new IllegalArgumentException(String.format(//
                    "Attribute '%s' is of type [%s], but [%s] was expected in attributes for annotation [%s]",
                    attributeName, //
                    attributeValue.getClass().getName(), //
                    expectedType.getName(),
                    this.displayName//
            ));
        }
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        Object obj = get(key);
        if (obj == null) {
            obj = put(key, value);
        }
        return obj;
    }

    @Override
    public String toString() {
        final Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
        final StringBuilder sb = new StringBuilder("{");
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(valueToString(entry.getValue()));
            sb.append(entries.hasNext() ? ", " : "");
        }
        sb.append("}");
        return sb.toString();
    }

    private String valueToString(Object value) {
        if (value == this) {
            return "(this Map)";
        }
        if (value instanceof Object[]) {
            return Arrays.toString((Object[]) value);
        }
        return String.valueOf(value);
    }

    public static AnnotationAttributes fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        if (map instanceof AnnotationAttributes) {
            return (AnnotationAttributes) map;
        }
        return new AnnotationAttributes(map);
    }

    @Override
    public boolean equals(Object object) {

        if (object == this) {
            return true;
        }
        if (!(object instanceof AnnotationAttributes)) {
            return false;
        }
        final AnnotationAttributes other = (AnnotationAttributes) object;

        if (other.annotationType != annotationType || !displayName.equals(other.displayName)) {
            return false;
        }

        return super.equals(object);
    }

}

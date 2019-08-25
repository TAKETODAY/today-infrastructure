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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.jdbc.mapping;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.jdbc.annotation.Column;
import cn.taketoday.jdbc.mapping.result.ResultResolver;

/**
 * @author TODAY <br>
 *         2019-08-21 18:53
 */
public class ColumnMapping implements PropertyAccessor {

    // Field
    private final String name;
    private final String column;

    private int index;
    private int jdbcType;

    private final Field target;

    private final Class<?> type;

    private ResultResolver resolver;

    /** @since 2.3.7 */
    private final Type[] genericityClass;

    private final PropertyAccessor accessor;

    private static final List<ResultResolver> RESULT_RESOLVERS = new ArrayList<>();

    public ColumnMapping(Field field) throws Exception {

        this.target = field;
        this.type = field.getType();
        this.name = field.getName();

        String value = null;
        final Column column = ClassUtils.getAnnotation(Column.class, field);

        if (column != null) {
            value = column.value();
        }

        if (StringUtils.isEmpty(value)) {
            value = this.name;
        }
        this.column = value;

        this.resolver = obtainResolver();
        this.accessor = obtainAccessor(field);

        this.genericityClass = ClassUtils.getGenericityClass(type);
    }

    @Override
    public String toString() {

        return String.format(
                "{\n\t\"name\":\"%s\",\n\t\"column\":\"%s\",\n\t\"index\":\"%s\",\n\t\"jdbcType\":\"%s\",\n\t\"target\":\"%s\",\n\t\"type\":\"%s\",\n\t\"resolver\":\"%s\",\n\t\"accessor\":\"%s\"\n}",
                name, column, index, jdbcType, target, type, resolver, accessor);
    }

    // Getter
    // ------------------

    public int getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(int jdbcType) {
        this.jdbcType = jdbcType;
    }

    public ResultResolver getResolver() {
        return resolver;
    }

    public String getName() {
        return name;
    }

    public String getColumn() {
        return column;
    }

    public Class<?> getType() {
        return type;
    }

    public Field getTarget() {
        return target;
    }

    protected PropertyAccessor obtainAccessor(Field field) {

        final String name = field.getName();

        try {

            final BeanInfo beanInfo = Introspector.getBeanInfo(field.getDeclaringClass());

            final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

            if (ObjectUtils.isEmpty(propertyDescriptors)) {
                return new FieldBasedPropertyAccessor(ClassUtils.makeAccessible(field));
            }
            for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {

                if (propertyDescriptor.getName().equals(name) //
                        && propertyDescriptor.getPropertyType() == field.getType()) {

                    final Method writeMethod = propertyDescriptor.getWriteMethod();
                    final Method readMethod = propertyDescriptor.getReadMethod();

                    if (writeMethod != null && readMethod != null) {
                        return new MethodBasedPropertyAccessor(readMethod, writeMethod);
                    }
                    return new FieldBasedPropertyAccessor(ClassUtils.makeAccessible(field));
                }
            }
        }
        catch (IntrospectionException e) {
            LoggerFactory.getLogger(getClass()).warn("Use reflect to access this field: [{}]", field, e);
            return new FieldBasedPropertyAccessor(ClassUtils.makeAccessible(field));
        }
        LoggerFactory.getLogger(getClass()).error("Can't obtain an accessor to access this field: [{}]", field);
        return null;
    }

    // ResultResolver
    // ----------------

    /**
     * Get correspond result resolver, If there isn't a suitable resolver will be
     * throw {@link ConfigurationException}
     * 
     * @return A suitable {@link ResultResolver}
     */
    protected ResultResolver obtainResolver() throws ConfigurationException {

        for (final ResultResolver resolver : getResultResolvers()) {
            if (resolver.supports(this)) {
                return resolver;
            }
        }

        throw ExceptionUtils.newConfigurationException(null,
                "There isn't have a result resolver to resolve : [" + toString() + "]");
    }

    public static void addResolver(ResultResolver... resolvers) {
        Collections.addAll(getResultResolvers(), resolvers);
    }

    public static void addResolver(List<ResultResolver> resolvers) {
        getResultResolvers().addAll(resolvers);
    }

    public static List<ResultResolver> getResultResolvers() {
        return RESULT_RESOLVERS;
    }

    // ---------------------

    @Override
    public Object get(Object obj) {
        return accessor.get(obj);
    }

    @Override
    public void set(Object obj, Object value) {
        accessor.set(obj, value);
    }

    public Object resolveResult(ResultSet resultSet) throws SQLException {
        return resolver.resolveResult(resultSet, index);
    }

    // Some useful methods
    // -----------------------------

    public boolean isAssignableFrom(Class<?> testClass) {
        return testClass.isAssignableFrom(type);
    }

    public boolean is(Class<?> type) {
        return type == this.type;
    }

    public Type getGenericityClass(final int index) {

        final Type[] genericityClass = this.genericityClass;
        if (genericityClass != null && genericityClass.length > index) {
            return genericityClass[index];
        }
        return null;
    }

    public boolean isGenericPresent(final Type requiredType, final int index) {
        return requiredType.equals(getGenericityClass(index));
    }

    public boolean isGenericPresent(final Type requiredType) {

        if (genericityClass != null) {
            for (final Type type : genericityClass) {
                if (type.equals(requiredType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDeclaringPresent(final Class<? extends Annotation> annotationClass) {
        return getDeclaringClassAnnotation(annotationClass) != null;
    }

    public boolean isPresent(final Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    public <A extends Annotation> A getDeclaringClassAnnotation(final Class<A> annotation) {
        return getAnnotation(target.getDeclaringClass(), annotation);
    }

    public <A extends Annotation> A getAnnotation(final Class<A> annotation) {
        return getAnnotation(target, annotation);
    }

    public <A extends Annotation> A getAnnotation(final AnnotatedElement element, final Class<A> annotation) {
        return ClassUtils.getAnnotation(annotation, element);
    }
}

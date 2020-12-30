/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.reflect.FieldPropertyAccessor;
import cn.taketoday.context.reflect.MethodAccessorPropertyAccessor;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.jdbc.FieldColumnConverter;
import cn.taketoday.jdbc.annotation.Column;
import cn.taketoday.jdbc.annotation.EnumValue;
import cn.taketoday.jdbc.mapping.result.ResultResolver;

import static cn.taketoday.jdbc.mapping.result.DelegatingResultResolver.delegate;

/**
 * @author TODAY <br>
 * 2019-08-21 18:53
 */
public class ColumnMapping implements PropertyAccessor {

  private static final Logger log = LoggerFactory.getLogger(ColumnMapping.class);

  // Field
  private final String name;
  private final String column;

  /** Target field */
  private final Field target;

  private final Class<?> type;

  private ResultResolver resolver;

  private final Type[] genericityClass;

  private final PropertyAccessor accessor;

  private static final List<ResultResolver> RESULT_RESOLVERS = new ArrayList<>();

  public ColumnMapping(Field field, final FieldColumnConverter converter) throws ConfigurationException {

    this.target = field;
    this.type = field.getType();
    this.name = field.getName();

    final Column column = ClassUtils.getAnnotation(Column.class, field);
    String columnName = column == null ? converter.convert(name) : column.value();

    if (StringUtils.isEmpty(columnName)) {
      columnName = this.name;
    }

    this.column = columnName;
    this.accessor = obtainAccessor(field);
    this.genericityClass = ClassUtils.getGenericityClass(type);

    this.resolver = obtainResolver();
    log.debug("Create Column Mapping: [{}]", this);
  }

  // Getter
  // ------------------

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
        return new FieldPropertyAccessor(field);
      }
      for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {

        if (propertyDescriptor.getName().equals(name) //
                && propertyDescriptor.getPropertyType() == field.getType()) {

          final Method writeMethod = propertyDescriptor.getWriteMethod();
          final Method readMethod = propertyDescriptor.getReadMethod();

          if (writeMethod != null && readMethod != null) {
            return new MethodAccessorPropertyAccessor(writeMethod, readMethod);
          }
          return new FieldPropertyAccessor(field);
        }
      }
    }
    catch (IntrospectionException e) {
      LoggerFactory.getLogger(getClass()).warn("Use reflect to access this field: [{}]", field, e);
      return new FieldPropertyAccessor(field);
    }
    LoggerFactory.getLogger(getClass()).error("Can't obtain an accessor to access this field: [{}]", field);
    return null;
  }

  // ResultResolver
  // -------------------------------------------

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
    throw new ConfigurationException("There isn't have a result resolver to resolve : [" + toString() + "]");
  }

  public static void addResolver(ResultResolver... resolvers) {
    Collections.addAll(getResultResolvers(), resolvers);
  }

  public static void addResolver(List<ResultResolver> resolvers) {
    getResultResolvers().addAll(resolvers);
  }

  public static void setDefaultResolvers() {
    ArrayList<ResultResolver> resolvers = new ArrayList<>();
    addDefaultResolvers(resolvers);
    getResultResolvers().addAll(resolvers);
  }

  public static void addDefaultResolvers(List<ResultResolver> resolvers) {

    // Byte[] byte[] int long float double short byte boolean BigDecimal, BigInteger
    // ------------------------------------------------------------------------------

    resolvers.add(delegate(p -> p.is(byte[].class), ResultSet::getBytes));
    resolvers.add(delegate(p -> p.is(BigDecimal.class), ResultSet::getBigDecimal));
    resolvers.add(delegate(p -> p.is(int.class) || p.is(Integer.class), ResultSet::getInt));
    resolvers.add(delegate(p -> p.is(byte.class) || p.is(Byte.class), ResultSet::getByte));
    resolvers.add(delegate(p -> p.is(long.class) || p.is(Long.class), ResultSet::getLong));
    resolvers.add(delegate(p -> p.is(short.class) || p.is(Short.class), ResultSet::getShort));
    resolvers.add(delegate(p -> p.is(float.class) || p.is(Float.class), ResultSet::getFloat));
    resolvers.add(delegate(p -> p.is(double.class) || p.is(Double.class), ResultSet::getDouble));
    resolvers.add(delegate(p -> p.is(boolean.class) || p.is(Boolean.class), ResultSet::getBoolean));
    resolvers.add(delegate(p -> p.is(char.class) || p.is(Character.class), (rs, i) -> {
      final String v = rs.getString(i);
      return v == null ? null : Character.valueOf(v.charAt(0));
    }));
    resolvers.add(delegate(p -> p.is(BigInteger.class), (rs, i) -> {
      final BigDecimal b = rs.getBigDecimal(i);
      return b == null ? null : b.toBigInteger();
    }));
    resolvers.add(delegate(p -> p.is(Byte[].class), (rs, i) -> {
      final byte[] bytes = rs.getBytes(i);
      if (bytes == null) {
        return null;
      }
      final Byte[] ret = new Byte[bytes.length];
      for (int j = 0; j < bytes.length; j++) {
        ret[j] = bytes[j];
      }
      return ret;
    }));
    // String
    // -------------------------------------

    resolvers.add(delegate(p -> p.is(Clob.class), ResultSet::getClob));
    resolvers.add(delegate(p -> p.is(String.class), ResultSet::getString));
    resolvers.add(delegate(p -> p.is(StringBuffer.class), (rs, i) -> new StringBuffer(rs.getString(i))));
    resolvers.add(delegate(p -> p.is(StringBuilder.class), (rs, i) -> new StringBuilder(rs.getString(i))));

    // SQL API
    // -------------------------------------
    resolvers.add(delegate(p -> p.is(Blob.class), ResultSet::getBlob));
    resolvers.add(delegate(p -> p.is(Time.class), ResultSet::getTime));
    resolvers.add(delegate(p -> p.is(Timestamp.class), ResultSet::getTimestamp));
    resolvers.add(delegate(p -> p.is(Date.class) || p.is(java.sql.Date.class), ResultSet::getDate));

    resolvers.add(delegate(p -> p.is(InputStream.class), (rs, i) -> {
      final Blob b = rs.getBlob(i);
      return b == null ? null : b.getBinaryStream();
    }));
    resolvers.add(delegate(p -> p.is(Reader.class), (rs, i) -> {
      final Clob c = rs.getClob(i);
      return c == null ? null : c.getCharacterStream();
    }));

    // jdk 1.8 Date and time API
    // -------------------------------------
    resolvers.add(delegate(p -> p.is(Instant.class), (rs, i) -> {
      final Timestamp tp = rs.getTimestamp(i);
      return tp == null ? null : tp.toInstant();
    }));
    resolvers.add(delegate(p -> p.is(LocalDateTime.class), (rs, i) -> {
      final Timestamp tp = rs.getTimestamp(i);
      return tp == null ? null : tp.toLocalDateTime();
    }));
    resolvers.add(delegate(p -> p.is(LocalDate.class), (rs, i) -> {
      final java.sql.Date d = rs.getDate(i);
      return d == null ? null : d.toLocalDate();
    }));
    resolvers.add(delegate(p -> p.is(LocalTime.class), (rs, i) -> {
      final Time t = rs.getTime(i);
      return t == null ? null : t.toLocalTime();
    }));
    resolvers.add(delegate(p -> p.is(OffsetDateTime.class), (rs, i) -> {
      final Timestamp tp = rs.getTimestamp(i);
      return tp == null ? null : OffsetDateTime.ofInstant(tp.toInstant(), ZoneId.systemDefault());
    }));
    resolvers.add(delegate(p -> p.is(OffsetTime.class), (rs, i) -> {
      final Time t = rs.getTime(i);
      return t == null ? null : t.toLocalTime().atOffset(OffsetTime.now().getOffset());
    }));
    resolvers.add(delegate(p -> p.is(ZonedDateTime.class), (rs, i) -> {
      final Timestamp tp = rs.getTimestamp(i);
      return tp == null ? null : ZonedDateTime.ofInstant(tp.toInstant(), ZoneId.systemDefault());
    }));
    resolvers.add(delegate(p -> p.is(Year.class), (rs, i) -> {
      final int year = rs.getInt(i);
      return year == 0 ? null : Year.of(year);
    }));
    resolvers.add(delegate(p -> p.is(Month.class), (rs, i) -> {
      final int month = rs.getInt(i);
      return month == 0 ? null : Month.of(month);
    }));
    resolvers.add(delegate(p -> p.is(YearMonth.class), (rs, i) -> {
      final String value = rs.getString(i);
      return value == null ? null : YearMonth.parse(value);
    }));

    // TODO Enums
    resolvers.add(delegate(p -> p.getType().isEnum() && p.isPresent(EnumValue.class), (rs, i) -> {
      final String value = rs.getString(i);

      return value == null ? null : YearMonth.parse(value);
    }));
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

  public void resolveResult(Object obj, ResultSet resultSet) throws SQLException {
    set(obj, resolver.resolveResult(resultSet, column));
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

  @Override
  public String toString() {
    return String.format(
            "{\n\t\"name\":\"%s\",\n\t\"column\":\"%s\",\n\t\"target\":\"%s\",\n\t\"type\":\"%s\",\n\t\"resolver\":\"%s\",\n\t\"genericityClass\":\"%s\",\n\t\"accessor\":\"%s\"\n}",
            name, column, target, type, resolver, Arrays.toString(genericityClass), accessor);
  }

}

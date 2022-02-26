/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.jdbc.result;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.Column;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MapCache;
import cn.taketoday.util.StringUtils;

/**
 * Stores metadata for a POJO
 *
 * @author TODAY
 */
public class JdbcBeanMetadata implements Iterable<BeanProperty> {

  private final Map<String, String> columnMappings;

  private final boolean caseSensitive;
  private final boolean throwOnMappingFailure;
  private final boolean autoDeriveColumnNames;

  private final BeanMetadata beanMetadata;
  private HashMap<String, BeanProperty> beanProperties;

  public JdbcBeanMetadata(Class<?> clazz) {
    this.beanMetadata = BeanMetadata.from(clazz);
    this.caseSensitive = false;
    this.columnMappings = null;
    this.throwOnMappingFailure = false;
    this.autoDeriveColumnNames = false;
  }

  public JdbcBeanMetadata(
          Class<?> clazz,
          boolean caseSensitive,
          boolean autoDeriveColumnNames,
          Map<String, String> columnMappings,
          boolean throwOnMappingError
  ) {
    this.beanMetadata = BeanMetadata.from(clazz);
    this.caseSensitive = caseSensitive;
    this.columnMappings = columnMappings;
    this.throwOnMappingFailure = throwOnMappingError;
    this.autoDeriveColumnNames = autoDeriveColumnNames;
  }

  public Map<String, String> getColumnMappings() {
    return columnMappings;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public boolean isAutoDeriveColumnNames() {
    return autoDeriveColumnNames;
  }

  public BeanProperty getBeanProperty(final String propertyName) {
    String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();

    if (columnMappings != null && columnMappings.containsKey(name)) {
      name = columnMappings.get(name);
    }

    if (autoDeriveColumnNames) {
      name = StringUtils.underscoreToCamelCase(name);
      if (!this.caseSensitive) {
        name = name.toLowerCase();
      }
    }

    HashMap<String, BeanProperty> beanProperties = this.beanProperties;
    if (beanProperties == null) {
      beanProperties = CACHE.get(beanMetadata.getType(), beanMetadata);
      this.beanProperties = beanProperties;
    }
    return beanProperties.get(name);
  }

  public JdbcBeanMetadata withCaseSensitive(boolean caseSensitive) {
    return new JdbcBeanMetadata(
            beanMetadata.getType(),
            caseSensitive,
            autoDeriveColumnNames,
            columnMappings,
            throwOnMappingFailure
    );
  }

  public JdbcBeanMetadata withPropertyType(Class<?> propertyType) {
    return new JdbcBeanMetadata(
            propertyType,
            caseSensitive,
            autoDeriveColumnNames,
            columnMappings,
            throwOnMappingFailure
    );
  }

  public JdbcBeanMetadata withAutoDeriveColumnNames(boolean autoDeriveColumnNames) {
    return new JdbcBeanMetadata(
            beanMetadata.getType(),
            caseSensitive,
            autoDeriveColumnNames,
            columnMappings,
            throwOnMappingFailure
    );
  }

  public boolean isThrowOnMappingFailure() {
    return throwOnMappingFailure;
  }

  //

  public Class<?> getType() { return beanMetadata.getType(); }

  public BeanInstantiator getInstantiator() { return beanMetadata.getInstantiator(); }

  public Object newInstance() { return beanMetadata.newInstance(); }

  public Object newInstance(@Nullable Object[] args) { return beanMetadata.newInstance(args); }

  public PropertyAccessor getPropertyAccessor(String propertyName) { return beanMetadata.getPropertyAccessor(propertyName); }

  public BeanProperty obtainBeanProperty(String propertyName) { return beanMetadata.obtainBeanProperty(propertyName); }

  public void setProperty(Object root, String propertyName, Object value) { beanMetadata.setProperty(root, propertyName, value); }

  public Object getProperty(Object root, String propertyName) { return beanMetadata.getProperty(root, propertyName); }

  public Class<?> getPropertyClass(String propertyName) { return beanMetadata.getPropertyType(propertyName); }

  public HashMap<String, BeanProperty> getBeanProperties() { return beanMetadata.getBeanProperties(); }

  public ArrayList<BeanProperty> beanProperties() { return beanMetadata.beanProperties(); }

  public int getPropertySize() { return beanMetadata.getPropertySize(); }

  public boolean containsProperty(String name) { return beanMetadata.containsProperty(name); }

  public HashMap<String, BeanProperty> createBeanProperties() { return beanMetadata.createBeanProperties(); }

  public Iterator<BeanProperty> iterator() { return beanMetadata.iterator(); }

  public void forEach(Consumer<? super BeanProperty> action) { beanMetadata.forEach(action); }

  public Spliterator<BeanProperty> spliterator() { return beanMetadata.spliterator(); }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof JdbcBeanMetadata that))
      return false;
    return caseSensitive == that.caseSensitive
            && throwOnMappingFailure == that.throwOnMappingFailure
            && autoDeriveColumnNames == that.autoDeriveColumnNames
            && Objects.equals(beanMetadata, that.beanMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseSensitive, throwOnMappingFailure, autoDeriveColumnNames, beanMetadata);
  }

  /**
   * get alias property-name
   *
   * @param property {@link Field}
   */
  static String getPropertyName(BeanProperty property) {
    String propertyName = getAnnotatedPropertyName(property);
    if (propertyName == null) {
      propertyName = property.getName();
    }
    return propertyName;
  }

  @Nullable
  static String getAnnotatedPropertyName(AnnotatedElement propertyElement) {
    // just alias name, cannot override its getter,setter
    MergedAnnotation<Column> annotation = MergedAnnotations.from(propertyElement).get(Column.class);
    if (annotation.isPresent()) {
      String name = annotation.getStringValue();
      if (StringUtils.isNotEmpty(name)) {
        return name;
      }
    }
    return null;
  }

  private static final MapCache<Class<?>, HashMap<String, BeanProperty>, BeanMetadata> CACHE = new MapCache<>(
          new ConcurrentReferenceHashMap<>()) {

    @Override
    protected HashMap<String, BeanProperty> createValue(Class<?> key, BeanMetadata beanMetadata) {
      if (beanMetadata == null) {
        throw new InvalidDataAccessApiUsageException("beanMetadata is required");
      }
      HashMap<String, BeanProperty> beanPropertyMap = new HashMap<>();
      for (BeanProperty property : beanMetadata) {
        String propertyName_ = getPropertyName(property);
        beanPropertyMap.put(propertyName_, property);
      }
      return beanPropertyMap;
    }
  };

}

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.persistence.Column;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MapCache;
import cn.taketoday.util.StringUtils;

/**
 * Stores metadata for a POJO
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class JdbcBeanMetadata implements Iterable<BeanProperty> {
  private static final Cache caseSensitiveFalse = new Cache();
  private static final Cache caseSensitiveTrue = new Cache();

  public final boolean caseSensitive;
  public final boolean throwOnMappingFailure;
  public final boolean autoDeriveColumnNames;

  public final BeanMetadata beanMetadata;
  private HashMap<String, BeanProperty> beanProperties;

  public JdbcBeanMetadata(Class<?> clazz) {
    this.beanMetadata = BeanMetadata.from(clazz);
    this.caseSensitive = false;
    this.throwOnMappingFailure = false;
    this.autoDeriveColumnNames = false;
  }

  public JdbcBeanMetadata(Class<?> clazz, boolean caseSensitive, boolean autoDeriveColumnNames, boolean throwOnMappingError) {
    this.caseSensitive = caseSensitive;
    this.beanMetadata = BeanMetadata.from(clazz);
    this.throwOnMappingFailure = throwOnMappingError;
    this.autoDeriveColumnNames = autoDeriveColumnNames;
  }

  public BeanProperty getBeanProperty(String colName, @Nullable Map<String, String> columnMappings) {
    if (columnMappings != null) {
      // find in columnMappings
      String propertyName = columnMappings.get(caseSensitive ? colName : colName.toLowerCase());
      if (propertyName != null) {
        // find
        BeanProperty beanProperty = getProperty(propertyName);
        if (beanProperty != null) {
          return beanProperty;
        }
      }
    }

    // try direct
    BeanProperty beanProperty = getProperty(colName);
    if (beanProperty != null) {
      return beanProperty;
    }

    // fallback
    if (autoDeriveColumnNames) {
      String propertyName = StringUtils.underscoreToCamelCase(colName);
      return getProperty(propertyName);
    }

    return null;
  }

  private BeanProperty getProperty(String propertyName) {
    HashMap<String, BeanProperty> beanProperties = this.beanProperties;
    if (beanProperties == null) {
      beanProperties = (caseSensitive ? caseSensitiveTrue : caseSensitiveFalse).get(beanMetadata.getType(), this);
      this.beanProperties = beanProperties;
    }

    return beanProperties.get(caseSensitive ? propertyName : propertyName.toLowerCase());
  }

  //

  public Class<?> getObjectType() { return beanMetadata.getType(); }

  public BeanInstantiator getInstantiator() { return beanMetadata.getInstantiator(); }

  public Object newInstance() { return beanMetadata.newInstance(); }

  public void setProperty(Object root, String propertyName, Object value) { beanMetadata.setProperty(root, propertyName, value); }

  public Object getProperty(Object root, String propertyName) { return beanMetadata.getProperty(root, propertyName); }

  @Override
  public Iterator<BeanProperty> iterator() { return beanMetadata.iterator(); }

  @Override
  public void forEach(Consumer<? super BeanProperty> action) { beanMetadata.forEach(action); }

  @Override
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

  static class Cache extends MapCache<Class<?>, HashMap<String, BeanProperty>, JdbcBeanMetadata> {

    public Cache() {
      super(new ConcurrentReferenceHashMap<>());
    }

    @Override
    protected HashMap<String, BeanProperty> createValue(Class<?> key, JdbcBeanMetadata beanMetadata) {
      if (beanMetadata == null) {
        throw new InvalidDataAccessApiUsageException("beanMetadata is required");
      }
      boolean caseSensitive = beanMetadata.caseSensitive;
      HashMap<String, BeanProperty> beanPropertyMap = new HashMap<>();
      for (BeanProperty property : beanMetadata) {
        String propertyName_ = getPropertyName(property);
        if (caseSensitive) {
          beanPropertyMap.put(propertyName_, property);
        }
        else {
          beanPropertyMap.put(propertyName_.toLowerCase(), property);
        }
      }
      return beanPropertyMap;
    }
  }

}

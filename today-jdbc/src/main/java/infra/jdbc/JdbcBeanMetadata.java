/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.persistence.Column;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.MapCache;
import infra.util.StringUtils;

/**
 * Stores metadata for a POJO
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class JdbcBeanMetadata {

  private static final Cache caseSensitiveFalse = new Cache();

  private static final Cache caseSensitiveTrue = new Cache();

  private final boolean caseSensitive;

  private final boolean autoDeriveColumnNames;

  final boolean throwOnMappingFailure;

  private final BeanMetadata beanMetadata;

  @Nullable
  private HashMap<String, BeanProperty> beanProperties;

  public JdbcBeanMetadata(Class<?> clazz, boolean caseSensitive, boolean autoDeriveColumnNames, boolean throwOnMappingError) {
    this.caseSensitive = caseSensitive;
    this.beanMetadata = BeanMetadata.forClass(clazz);
    this.throwOnMappingFailure = throwOnMappingError;
    this.autoDeriveColumnNames = autoDeriveColumnNames;
  }

  @Nullable
  public BeanProperty getBeanProperty(String colName, @Nullable Map<String, String> columnMappings) {
    if (columnMappings != null) {
      // find in columnMappings
      String propertyName = columnMappings.get(caseSensitive ? colName : colName.toLowerCase(Locale.ROOT));
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

    return beanProperties.get(caseSensitive ? propertyName : propertyName.toLowerCase(Locale.ROOT));
  }

  //

  public Class<?> getObjectType() {
    return beanMetadata.getType();
  }

  public Object newInstance() {
    return beanMetadata.newInstance();
  }

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
    protected HashMap<String, BeanProperty> createValue(Class<?> key, JdbcBeanMetadata params) {
      boolean caseSensitive = params.caseSensitive;
      HashMap<String, BeanProperty> beanPropertyMap = new HashMap<>();
      for (BeanProperty property : params.beanMetadata) {
        String propertyName_ = getPropertyName(property);
        if (caseSensitive) {
          beanPropertyMap.put(propertyName_, property);
        }
        else {
          beanPropertyMap.put(propertyName_.toLowerCase(Locale.ROOT), property);
        }
      }
      return beanPropertyMap;
    }
  }

}

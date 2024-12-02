/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.persistence;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.style.ToStringBuilder;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:43
 */
public class EntityMetadata {
  public final String tableName;

  public final BeanMetadata root;

  public final Class<?> entityClass;

  @Nullable
  public final String idColumnName;

  @Nullable
  public final EntityProperty idProperty;

  public final BeanProperty[] beanProperties;

  public final String[] columnNames;
  public final EntityProperty[] entityProperties;

  public final String[] columnNamesExcludeId;
  public final EntityProperty[] entityPropertiesExcludeId;

  // is auto generated Id
  public final boolean autoGeneratedId;

  @Nullable
  private MergedAnnotations annotations;

  private final HashMap<String, EntityProperty> propertyMap;

  protected EntityMetadata(BeanMetadata root, Class<?> entityClass, @Nullable EntityProperty idProperty, String tableName,
          List<BeanProperty> beanProperties, List<String> columnNames, List<EntityProperty> entityProperties) {
    this.root = root;
    this.tableName = tableName;
    this.idProperty = idProperty;
    this.entityClass = entityClass;
    this.propertyMap = mapProperties(entityProperties);
    this.idColumnName = idProperty != null ? idProperty.columnName : null;
    this.columnNames = StringUtils.toStringArray(columnNames);
    this.beanProperties = beanProperties.toArray(new BeanProperty[0]);
    this.entityProperties = entityProperties.toArray(new EntityProperty[0]);

    if (idProperty != null) {
      entityProperties.remove(idProperty);
      columnNames.remove(idProperty.columnName);
    }

    this.autoGeneratedId = determineGeneratedId(idProperty);
    this.columnNamesExcludeId = StringUtils.toStringArray(columnNames);
    this.entityPropertiesExcludeId = entityProperties.toArray(new EntityProperty[0]);
  }

  private HashMap<String, EntityProperty> mapProperties(List<EntityProperty> entityProperties) {
    HashMap<String, EntityProperty> propertyMap = new HashMap<>();
    for (EntityProperty property : entityProperties) {
      propertyMap.put(property.property.getName(), property);
    }
    return propertyMap;
  }

  private boolean determineGeneratedId(@Nullable EntityProperty idProperty) {
    if (idProperty != null) {
      return MergedAnnotations.from(idProperty.property, idProperty.property.getAnnotations())
              .isPresent(GeneratedId.class);
    }
    return false;
  }

  /**
   * obtain id property
   *
   * @throws IllegalEntityException id property not found
   */
  public EntityProperty idProperty() throws IllegalEntityException {
    EntityProperty idProperty = this.idProperty;
    if (idProperty == null) {
      throw new IllegalEntityException("ID property is required");
    }
    return idProperty;
  }

  /**
   * FInd property
   */
  @Nullable
  public EntityProperty findProperty(String name) {
    return propertyMap.get(name);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("tableName", tableName)
            .append("columnNames", columnNames)
            .append("entityClass", entityClass)
            .append("idProperty", idProperty)
            .append("beanProperties", beanProperties)
            .append("entityProperties", entityProperties)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    return this == o
            || (o instanceof EntityMetadata that
            && Objects.equals(tableName, that.tableName)
            && Objects.equals(idProperty, that.idProperty)
            && Arrays.equals(columnNames, that.columnNames)
            && Objects.equals(entityClass, that.entityClass)
            && Arrays.equals(beanProperties, that.beanProperties)
            && Arrays.equals(entityProperties, that.entityProperties));
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(tableName, entityClass, idProperty);
    result = 31 * result + Arrays.hashCode(beanProperties);
    result = 31 * result + Arrays.hashCode(columnNames);
    result = 31 * result + Arrays.hashCode(entityProperties);
    return result;
  }

  public MergedAnnotations getAnnotations() {
    MergedAnnotations annotations = this.annotations;
    if (annotations == null) {
      annotations = MergedAnnotations.from(entityClass);
      this.annotations = annotations;
    }
    return annotations;
  }

  public <A extends Annotation> MergedAnnotation<A> getAnnotation(Class<A> annType) {
    return getAnnotations().get(annType);
  }

  public <A extends Annotation> boolean isPresent(Class<A> annType) {
    return getAnnotations().isPresent(annType);
  }

}

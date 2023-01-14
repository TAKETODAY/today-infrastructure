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

package cn.taketoday.jdbc.sql;

import java.util.ArrayList;
import java.util.Set;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Default EntityHolderFactory
 * <p>
 * <ul>
 * <li> {@link TableNameGenerator} to generate table name
 * <li> {@link IdPropertyDiscover} to find the ID property
 * <li> {@link ColumnNameDiscover} to find column name
 * <li> {@link PropertyFilter} to filter the property
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyFilter
 * @see TableNameGenerator
 * @see IdPropertyDiscover
 * @see ColumnNameDiscover
 * @since 4.0 2022/9/5 11:38
 */
public class DefaultEntityMetadataFactory extends EntityMetadataFactory {

  private PropertyFilter propertyFilter = PropertyFilter.filteredNames(Set.of("class"));

  private TableNameGenerator tableNameGenerator = TableNameGenerator.forTableAnnotation()
          .and(TableNameGenerator.defaultStrategy());

  private IdPropertyDiscover idPropertyDiscover = IdPropertyDiscover.forIdAnnotation()
          .and(IdPropertyDiscover.forPropertyName(IdPropertyDiscover.DEFAULT_ID_PROPERTY));

  private ColumnNameDiscover columnNameDiscover = ColumnNameDiscover.forColumnAnnotation()
          .and(ColumnNameDiscover.camelCaseToUnderscore());

  private TypeHandlerRegistry typeHandlerRegistry = TypeHandlerRegistry.getSharedInstance();

  /**
   * set the ColumnNameDiscover to find the column name
   *
   * @param columnNameDiscover ColumnNameDiscover
   */
  public void setColumnNameDiscover(ColumnNameDiscover columnNameDiscover) {
    Assert.notNull(columnNameDiscover, "columnNameDiscover is required");
    this.columnNameDiscover = columnNameDiscover;
  }

  /**
   * set the IdPropertyDiscover to determine the ID property
   *
   * @param idPropertyDiscover a new IdPropertyDiscover
   */
  public void setIdPropertyDiscover(IdPropertyDiscover idPropertyDiscover) {
    Assert.notNull(idPropertyDiscover, "idPropertyDiscover is required");
    this.idPropertyDiscover = idPropertyDiscover;
  }

  /**
   * set the PropertyFilter to filter the property
   *
   * @param propertyFilter a PropertyFilter
   */
  public void setPropertyFilter(PropertyFilter propertyFilter) {
    Assert.notNull(propertyFilter, "propertyFilter is required");
    this.propertyFilter = propertyFilter;
  }

  /**
   * set the TableNameGenerator to generate the table name for the class
   *
   * @param tableNameGenerator a TableNameGenerator
   */
  public void setTableNameGenerator(TableNameGenerator tableNameGenerator) {
    Assert.notNull(tableNameGenerator, "tableNameGenerator is required");
    this.tableNameGenerator = tableNameGenerator;
  }

  /**
   * Set the TypeHandlerRegistry to find {@link TypeHandler} for the {@link BeanProperty}
   *
   * @param typeHandlerRegistry TypeHandlerRegistry
   */
  public void setTypeHandlerRegistry(TypeHandlerRegistry typeHandlerRegistry) {
    Assert.notNull(typeHandlerRegistry, "typeHandlerRegistry is required");
    this.typeHandlerRegistry = typeHandlerRegistry;
  }

  @Override
  public EntityMetadata createEntityMetadata(Class<?> entityClass) {
    String tableName = tableNameGenerator.generateTableName(entityClass);
    if (tableName == null) {
      throw new IllegalEntityException("Cannot determine table name for entity: " + entityClass);
    }

    BeanMetadata metadata = BeanMetadata.from(entityClass);
    ArrayList<String> columnNames = new ArrayList<>();
    ArrayList<BeanProperty> beanProperties = new ArrayList<>();
    ArrayList<EntityProperty> propertyHandlers = new ArrayList<>();

    BeanProperty idProperty = null;
    String idColumnName = null;
    for (BeanProperty property : metadata) {
      if (isFiltered(property)) {
        continue;
      }

      String columnName = columnNameDiscover.getColumnName(property);
      if (columnName == null) {
        throw new IllegalEntityException(
                "Cannot determine column name for property: " +
                        ClassUtils.getShortName(property.getDeclaringClass()) + "#" + property.getName());
      }

      columnNames.add(columnName);
      beanProperties.add(property);
      propertyHandlers.add(createEntityProperty(property, columnName));

      if (idPropertyDiscover.isIdProperty(property)) {
        if (idProperty != null) {
          throw new IllegalEntityException("Only one Id property supported, entity: " + entityClass);
        }
        idProperty = property;
        idColumnName = columnName;
      }
    }
    if (idProperty == null) {
      // TODO id can be null
      throw new IllegalEntityException("Cannot determine ID property for entity: " + entityClass);
    }

    return new EntityMetadata(metadata, entityClass,
            idColumnName, createEntityProperty(idProperty, idColumnName),
            tableName, beanProperties.toArray(new BeanProperty[0]),
            StringUtils.toStringArray(columnNames), propertyHandlers.toArray(new EntityProperty[0]));
  }

  private EntityProperty createEntityProperty(BeanProperty property, String columnName) {
    return new EntityProperty(property, columnName, typeHandlerRegistry.getTypeHandler(property));
  }

  private boolean isFiltered(BeanProperty property) {
    return isInnerClass(property) || propertyFilter.isFiltered(property);
  }

  private static boolean isInnerClass(BeanProperty property) {
    return property.getName().equals("this$0");
  }

}

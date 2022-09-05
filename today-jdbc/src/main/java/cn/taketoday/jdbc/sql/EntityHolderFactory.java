/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:28
 */
public class EntityHolderFactory {

  private final PropertyFilter propertyFilter = PropertyFilter.filteredNames(Set.of("class"));

  private final TableNameGenerator tableNameGenerator = TableNameGenerator.forTableAnnotation()
          .and(TableNameGenerator.defaultStrategy());

  private final IdPropertyDiscover idPropertyDiscover = IdPropertyDiscover.forIdAnnotation()
          .and(IdPropertyDiscover.forPropertyName("id"));

  private final ColumnNameDiscover columnNameDiscover = ColumnNameDiscover.forColumnAnnotation()
          .and(ColumnNameDiscover.camelCaseToUnderscore());

  public EntityHolder createEntityHolder(Class<?> entityClass) {
    String tableName = tableNameGenerator.generateTableName(entityClass);
    if (tableName == null) {
      throw new IllegalStateException("Cannot determine table name for entity: " + entityClass);
    }

    BeanMetadata metadata = BeanMetadata.from(entityClass);
    ArrayList<String> columnNames = new ArrayList<>();
    ArrayList<BeanProperty> beanProperties = new ArrayList<>();

    BeanProperty idProperty = null;
    for (BeanProperty property : metadata) {
      if (isFiltered(property)) {
        continue;
      }

      String columnName = columnNameDiscover.getColumnName(property);
      if (columnName == null) {
        throw new IllegalStateException("Cannot determine column name for property: " + property.getField());
      }

      columnNames.add(columnName);
      beanProperties.add(property);

      if (idPropertyDiscover.isIdProperty(property)) {
        if (idProperty != null) {
          throw new IllegalStateException("Only one Id property supported, entityClass: " + entityClass);
        }
        idProperty = property;
      }
    }

    Assert.state(idProperty != null, "Cannot determine ID property");
    return new EntityHolder(entityClass, idProperty, tableName, beanProperties.toArray(new BeanProperty[0]),
            StringUtils.toStringArray(columnNames));
  }

  private boolean isFiltered(BeanProperty property) {
    return propertyFilter.isFiltered(property);
  }

}

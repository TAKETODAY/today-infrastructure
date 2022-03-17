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

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.type.TypeHandler;

/**
 * @author TODAY 2021/1/7 22:50
 */
public final class TypeHandlerPropertyAccessor extends JdbcPropertyAccessor {
  private final TypeHandler<?> typeHandler;
  private final BeanProperty beanProperty;

  public TypeHandlerPropertyAccessor(TypeHandler<?> typeHandler, BeanProperty beanProperty) {
    this.typeHandler = typeHandler;
    this.beanProperty = beanProperty;
  }

  @Override
  public Object get(Object obj) {
    return beanProperty.getValue(obj);
  }

  @Override
  public void set(Object obj, ResultSet resultSet, int columnIndex) throws SQLException {
    final Object result = getResult(resultSet, columnIndex);
    beanProperty.setValue(obj, result);
  }

  /**
   * Get result from {@link ResultSet}.
   * <p>
   * Obtain from {@link TypeHandler}, if it fails, use the default acquisition method
   * </p>
   *
   * @param resultSet Target result set
   * @return data object
   * @throws SQLException If {@link ResultSet#getObject(int)} failed
   */
  private Object getResult(ResultSet resultSet, int columnIndex) throws SQLException {
    try {
      return typeHandler.getResult(resultSet, columnIndex);
    }
    catch (SQLException e) {
      // maybe data conversion error
      return resultSet.getObject(columnIndex);
    }
  }
}

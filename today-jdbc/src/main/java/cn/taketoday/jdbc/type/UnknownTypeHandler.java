/*
 * Copyright 2017 - 2023 the original author or authors.
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
package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * @author Clinton Begin
 * @author TODAY
 */
public class UnknownTypeHandler extends BaseTypeHandler<Object> {

  private boolean useColumnLabel = true;
  private final TypeHandlerManager registry;

  /**
   * The constructor that pass the type handler registry.
   *
   * @param typeHandlerManager a type handler registry
   */
  public UnknownTypeHandler(TypeHandlerManager typeHandlerManager) {
    this.registry = typeHandlerManager;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void setNonNullParameter(PreparedStatement ps, int i, Object parameter) throws SQLException {
    TypeHandler handler = resolveTypeHandler(parameter);
    handler.setParameter(ps, i, parameter);
  }

  @Override
  public Object getResult(ResultSet rs, String columnName) throws SQLException {
    TypeHandler<?> handler = resolveTypeHandler(rs, columnName);
    return handler.getResult(rs, columnName);
  }

  @Override
  public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
    TypeHandler<?> handler = resolveTypeHandler(rs.getMetaData(), columnIndex);
    if (handler == null || handler instanceof UnknownTypeHandler) {
      handler = ObjectTypeHandler.sharedInstance;
    }
    return handler.getResult(rs, columnIndex);
  }

  @Override
  public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getObject(columnIndex);
  }

  protected TypeHandler<?> resolveTypeHandler(Object parameter) {
    if (parameter == null) {
      return ObjectTypeHandler.sharedInstance;
    }
    TypeHandler<?> handler = registry.getTypeHandler(parameter.getClass());
    // check if handler is null (issue #270)
    if (handler == null || handler instanceof UnknownTypeHandler) {
      handler = ObjectTypeHandler.sharedInstance;
    }
    return handler;
  }

  private TypeHandler<?> resolveTypeHandler(final ResultSet rs, final String column) {
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int count = rsmd.getColumnCount();
      boolean useColumnLabel = isUseColumnLabel();
      TypeHandler<?> handler = null;
      for (int columnIdx = 1; columnIdx <= count; columnIdx++) {
        String name = useColumnLabel ? rsmd.getColumnLabel(columnIdx) : rsmd.getColumnName(columnIdx);
        if (column.equals(name)) {
          handler = resolveTypeHandler(rsmd, columnIdx);
          break;
        }
      }
      if (handler == null || handler instanceof UnknownTypeHandler) {
        handler = ObjectTypeHandler.sharedInstance;
      }
      return handler;
    }
    catch (SQLException e) {
      throw new TypeException("Error determining JDBC type for column " + column + ".  Cause: " + e, e);
    }
  }

  @Nullable
  protected TypeHandler<?> resolveTypeHandler(ResultSetMetaData rsmd, Integer columnIndex) {
    Class<?> javaType = safeGetClassForColumn(rsmd, columnIndex);
    if (javaType != null) {
      return registry.getTypeHandler(javaType);
    }
    return null;
  }

  @Nullable
  private Class<?> safeGetClassForColumn(ResultSetMetaData rsmd, Integer columnIndex) {
    try {
      return ClassUtils.load(rsmd.getColumnClassName(columnIndex));
    }
    catch (Exception e) {
      return null;
    }
  }

  public boolean isUseColumnLabel() {
    return useColumnLabel;
  }

  public void setUseColumnLabel(boolean useColumnLabel) {
    this.useColumnLabel = useColumnLabel;
  }
}

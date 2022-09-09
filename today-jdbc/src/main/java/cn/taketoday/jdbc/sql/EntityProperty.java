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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/7 22:46
 */
public class EntityProperty {
  public final String columnName;

  public final BeanProperty property;
  public final TypeHandler<Object> typeHandler;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public EntityProperty(BeanProperty property, String columnName, TypeHandler typeHandler) {
    this.property = property;
    this.columnName = columnName;
    this.typeHandler = typeHandler;
  }

  /**
   * get property of this {@code entity}
   *
   * @param entity entity object
   * @return property value
   */
  public Object getValue(Object entity) {
    return property.getValue(entity);
  }

  public void setValue(Object entity, @Nullable Object propertyValue) {
    property.setDirectly(entity, propertyValue);
  }

  /**
   * Set property-value of input {@code entity} to {@link PreparedStatement}
   *
   * @param ps PreparedStatement
   * @param parameterIndex index of SQL parameter
   * @param entity java entity object
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  public void setTo(PreparedStatement ps, int parameterIndex, Object entity) throws SQLException {
    Object propertyValue = property.getValue(entity);
    typeHandler.setParameter(ps, parameterIndex, propertyValue);
  }

  /**
   * <p>Sets the value of the designated parameter using the given object.
   *
   * <p>The JDBC specification specifies a standard mapping from
   * Java {@code Object} types to SQL types.  The given argument
   * will be converted to the corresponding SQL type before being
   * sent to the database.
   *
   * <p>Note that this method may be used to pass database-
   * specific abstract data types, by using a driver-specific Java
   * type.
   *
   * If the object is of a class implementing the interface {@code SQLData},
   * the JDBC driver should call the method {@code SQLData.writeSQL}
   * to write it to the SQL data stream.
   * If, on the other hand, the object is of a class implementing
   * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
   * {@code Struct}, {@code java.net.URL}, {@code RowId}, {@code SQLXML}
   * or {@code Array}, the driver should pass it to the database as a
   * value of the corresponding SQL type.
   * <P>
   * <b>Note:</b> Not all databases allow for a non-typed Null to be sent to
   * the backend. For maximum portability, the {@code setNull} or the
   * {@code setObject(int parameterIndex, Object x, int sqlType)}
   * method should be used
   * instead of {@code setObject(int parameterIndex, Object x)}.
   * <p>
   * <b>Note:</b> This method throws an exception if there is an ambiguity, for example, if the
   * object is of a class implementing more than one of the interfaces named above.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param parameter the object containing the input parameter value
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  public void setParameter(PreparedStatement ps, int parameterIndex, Object parameter) throws SQLException {
    typeHandler.setParameter(ps, parameterIndex, parameter);
  }

  /**
   * @param rs ResultSet
   * @param columnIndex the first column is 1, the second is 2, ...
   * @throws SQLException if a database access error occurs or this method is
   * called on a closed result set
   */
  @Nullable
  public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
    return typeHandler.getResult(rs, columnIndex);
  }

  public void setProperty(Object entity, ResultSet rs, int columnIndex) throws SQLException {
    Object propertyValue = typeHandler.getResult(rs, columnIndex);
    property.setDirectly(entity, propertyValue);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("property", property)
            .append("typeHandler", typeHandler)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof EntityProperty that))
      return false;
    return Objects.equals(property, that.property)
            && Objects.equals(typeHandler, that.typeHandler);
  }

  @Override
  public int hashCode() {
    return Objects.hash(property, typeHandler);
  }

}

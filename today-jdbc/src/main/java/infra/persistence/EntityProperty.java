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

package infra.persistence;

import java.lang.annotation.Annotation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import infra.beans.BeanProperty;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.style.ToStringBuilder;
import infra.jdbc.type.TypeHandler;
import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/7 22:46
 */
public class EntityProperty {

  public final String columnName;

  public final boolean isIdProperty;

  public final BeanProperty property;

  public final TypeHandler<Object> typeHandler;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  EntityProperty(BeanProperty property, String columnName, TypeHandler typeHandler, boolean isIdProperty) {
    this.property = property;
    this.columnName = columnName;
    this.typeHandler = typeHandler;
    this.isIdProperty = isIdProperty;
  }

  /**
   * get property of this {@code entity}
   *
   * @param entity entity object
   * @return property value
   */
  @Nullable
  public Object getValue(Object entity) {
    return property.getValue(entity);
  }

  /**
   * Set the property value to entity
   *
   * @param entity entity instance
   * @param propertyValue value
   */
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
   * <p>
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
   * @param arg the object containing the input parameter value
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable Object arg) throws SQLException {
    typeHandler.setParameter(ps, parameterIndex, arg);
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

  /**
   * Set property
   */
  public void setProperty(Object entity, ResultSet rs, int columnIndex) throws SQLException {
    Object propertyValue = getResult(rs, columnIndex);
    property.setDirectly(entity, propertyValue);
  }

  public MergedAnnotations getAnnotations() {
    return property.mergedAnnotations();
  }

  public <A extends Annotation> MergedAnnotation<A> getAnnotation(Class<A> annType) {
    return getAnnotations().get(annType);
  }

  public <A extends Annotation> boolean isPresent(Class<A> annType) {
    return getAnnotations().isPresent(annType);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("property", property)
            .append("columnName", columnName)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    return this == o
            || (o instanceof EntityProperty that
            && Objects.equals(property, that.property)
            && Objects.equals(typeHandler, that.typeHandler));
  }

  @Override
  public int hashCode() {
    return Objects.hash(property, typeHandler);
  }

}

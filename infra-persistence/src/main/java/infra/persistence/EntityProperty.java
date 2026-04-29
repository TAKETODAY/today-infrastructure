/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.persistence;

import org.jspecify.annotations.Nullable;

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

/**
 * Represents a property of an entity mapped to a database column.
 * <p>
 * This class encapsulates the metadata and behavior for reading from and writing to
 * a specific column in a database table, linking it to a corresponding Java bean property.
 * It handles type conversion via {@link TypeHandler} and supports annotation inspection.
 *
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
   * Retrieves the value of this property from the given entity.
   *
   * @param entity the entity instance from which to retrieve the property value
   * @return the property value, or {@code null} if the property value is null
   */
  public @Nullable Object getValue(Object entity) {
    return property.getValue(entity);
  }

  /**
   * Sets the value of this property on the given entity.
   *
   * @param entity the entity instance on which to set the property value
   * @param propertyValue the value to set, may be {@code null}
   */
  public void setValue(Object entity, @Nullable Object propertyValue) {
    property.setDirectly(entity, propertyValue);
  }

  /**
   * Sets the value of this property from the given entity as a parameter in the
   * specified {@link PreparedStatement}.
   * <p>
   * This method retrieves the property value from the entity and uses the associated
   * {@link TypeHandler} to set it on the prepared statement at the specified parameter index.
   *
   * @param ps the {@link PreparedStatement} to set the parameter on
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param entity the entity instance from which to retrieve the property value
   * @throws SQLException if a database access error occurs, this method is called on a closed
   * {@code PreparedStatement}, the parameter index does not correspond to a parameter marker,
   * or the type of the given object is ambiguous
   */
  public void setTo(PreparedStatement ps, int parameterIndex, Object entity) throws SQLException {
    Object propertyValue = property.getValue(entity);
    typeHandler.setParameter(ps, parameterIndex, propertyValue);
  }

  /**
   * Sets the value of the designated parameter in the given {@link PreparedStatement}
   * using the provided argument.
   * <p>
   * This method delegates to the associated {@link TypeHandler} to convert the Java object
   * into the appropriate SQL type and set it on the prepared statement. The JDBC specification
   * defines standard mappings from Java {@code Object} types to SQL types.
   *
   * @param ps the {@link PreparedStatement} to set the parameter on
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param arg the object containing the input parameter value, may be {@code null}
   * @throws SQLException if a database access error occurs, this method is called on a closed
   * {@code PreparedStatement}, the parameter index does not correspond to a parameter marker,
   * or the type of the given object is ambiguous
   */
  public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable Object arg) throws SQLException {
    typeHandler.setParameter(ps, parameterIndex, arg);
  }

  /**
   * Retrieves the value from the {@link ResultSet} at the specified column index
   * and converts it using the associated {@link TypeHandler}.
   *
   * @param rs the {@link ResultSet} to retrieve data from
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the converted property value, or {@code null} if the SQL value is NULL
   * @throws SQLException if a database access error occurs or this method is
   * called on a closed result set
   */
  public @Nullable Object getResult(ResultSet rs, int columnIndex) throws SQLException {
    return typeHandler.getResult(rs, columnIndex);
  }

  /**
   * Retrieves the value from the {@link ResultSet} and sets it to the corresponding
   * property of the given entity instance.
   *
   * @param entity the entity instance to set the property value to
   * @param rs the {@link ResultSet} to retrieve data from
   * @param columnIndex the first column is 1, the second is 2, ...
   * @throws SQLException if a database access error occurs or this method is
   * called on a closed result set
   */
  public void setProperty(Object entity, ResultSet rs, int columnIndex) throws SQLException {
    Object propertyValue = getResult(rs, columnIndex);
    property.setDirectly(entity, propertyValue);
  }

  /**
   * Returns the merged annotations present on the underlying bean property.
   *
   * @return the merged annotations
   */
  public MergedAnnotations getAnnotations() {
    return property.mergedAnnotations();
  }

  /**
   * Returns the merged annotation of the specified type present on the underlying bean property.
   *
   * @param annType the annotation type to look for
   * @param <A> the annotation type
   * @return the merged annotation, never {@code null}
   */
  public <A extends Annotation> MergedAnnotation<A> getAnnotation(Class<A> annType) {
    return getAnnotations().get(annType);
  }

  /**
   * Checks if an annotation of the specified type is present on the underlying bean property.
   *
   * @param annType the annotation type to check for
   * @param <A> the annotation type
   * @return {@code true} if the annotation is present, {@code false} otherwise
   */
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
  public boolean equals(@Nullable Object o) {
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

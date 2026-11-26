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

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import infra.beans.BeanProperty;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionFailedException;
import infra.core.conversion.ConversionService;
import infra.jdbc.type.TypeHandler;
import infra.jdbc.type.WrappedTypeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ObjectPropertySetterTests {

  private final ResultSet resultSet = mock(ResultSet.class);

  private final ConversionService conversionService = mock(ConversionService.class);

  @Test
  void setsWritableProperty() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerReturning("value");
    Target target = new Target();
    ObjectPropertySetter setter = setter(null, Target.class, "name", typeHandler);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.name).isEqualTo("value");
  }

  @Test
  void setsNestedWritableProperty() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerReturning("nested value");
    Target target = new Target();
    NestedPropertyAccessor accessor = new NestedPropertyAccessor(Target.class, "nested.name");
    ObjectPropertySetter setter = setter(accessor, accessor.getLeafProperty(), typeHandler);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.nested).isNotNull();
    assertThat(target.nested.name).isEqualTo("nested value");
  }

  @Test
  void invokesPrimitiveNullHandlerForNullValue() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerReturning(null);
    PrimitiveTypeNullHandler nullHandler = mock(PrimitiveTypeNullHandler.class);
    Target target = new Target();
    BeanProperty property = BeanProperty.valueOf(Target.class, "count");
    ObjectPropertySetter setter = new ObjectPropertySetter(null, property, this.conversionService,
            typeHandler, nullHandler);

    setter.setTo(target, this.resultSet, 1);

    verify(nullHandler).handleNull(property, target);
  }

  @Test
  void invokesPrimitiveNullHandlerForNestedNullValue() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerReturning(null);
    PrimitiveTypeNullHandler nullHandler = mock(PrimitiveTypeNullHandler.class);
    Target target = new Target();
    NestedPropertyAccessor accessor = new NestedPropertyAccessor(Target.class, "nested.count");
    BeanProperty property = accessor.getLeafProperty();
    ObjectPropertySetter setter = new ObjectPropertySetter(accessor, property, this.conversionService,
            typeHandler, nullHandler);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.nested).isNotNull();
    verify(nullHandler).handleNull(property, target.nested);
  }

  @Test
  void leavesPrimitiveUnchangedWhenNullHandlerIsMissing() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerReturning(null);
    Target target = new Target();
    ObjectPropertySetter setter = setter(null, Target.class, "count", typeHandler);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.count).isZero();
  }

  @Test
  void appliesWrappedTypeHandlerToExistingValue() throws SQLException {
    WrappedValue wrappedValue = new WrappedValue();
    WrappedTypeHandler<WrappedValue> typeHandler = mock(WrappedTypeHandler.class);
    Target target = new Target(wrappedValue);
    BeanProperty property = BeanProperty.valueOf(Target.class, "fixedWrapped");
    assertThat(property.isWriteable()).isFalse();
    ObjectPropertySetter setter = new ObjectPropertySetter(null, property, this.conversionService,
            typeHandler, null);

    setter.setTo(target, this.resultSet, 1);

    verify(typeHandler).applyResult(wrappedValue, this.resultSet, 1);
  }

  @Test
  void rejectsWrappedTypeHandlerWhenValueIsNull() throws SQLException {
    WrappedTypeHandler<WrappedValue> typeHandler = mock(WrappedTypeHandler.class);
    Target target = new Target();
    BeanProperty property = BeanProperty.valueOf(Target.class, "fixedWrapped");
    assertThat(property.isWriteable()).isFalse();
    ObjectPropertySetter setter = new ObjectPropertySetter(null, property, this.conversionService,
            typeHandler, null);

    assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> setter.setTo(target, this.resultSet, 1))
            .withMessage("Not writable entity property its value is required");
  }

  @Test
  void rejectsNonWritablePropertyWithoutWrappedHandler() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerReturning("ignored");
    Target target = new Target();
    BeanProperty property = BeanProperty.valueOf(Target.class, "fixed");
    ObjectPropertySetter setter = new ObjectPropertySetter(null, property, this.conversionService,
            typeHandler, null);

    assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> setter.setTo(target, this.resultSet, 1))
            .withMessageContaining("is not writable");
  }

  @Test
  void usesResultSetObjectWhenTypeHandlerFailsAndValueIsNull() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerFailing();
    given(this.resultSet.getObject(1)).willReturn(null);
    Target target = new Target();
    ObjectPropertySetter setter = setter(null, Target.class, "name", typeHandler);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.name).isNull();
  }

  @Test
  void usesResultSetObjectWhenItMatchesPropertyType() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerFailing();
    given(this.resultSet.getObject(1)).willReturn("database value");
    Target target = new Target();
    ObjectPropertySetter setter = setter(null, Target.class, "name", typeHandler);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.name).isEqualTo("database value");
  }

  @Test
  void convertsResultSetObjectWhenTypeHandlerFails() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerFailing();
    given(this.resultSet.getObject(1)).willReturn(42);
    given(this.conversionService.convert(42, BeanProperty.valueOf(Target.class, "name").getTypeDescriptor()))
            .willReturn("converted value");
    Target target = new Target();
    ObjectPropertySetter setter = setter(null, Target.class, "name", typeHandler);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.name).isEqualTo("converted value");
  }

  @Test
  void treatsPrimitiveConversionFailureWithNullCauseAsNull() throws SQLException {
    TypeHandler<?> typeHandler = typeHandlerFailing();
    given(this.resultSet.getObject(1)).willReturn("not a number");
    BeanProperty property = BeanProperty.valueOf(Target.class, "count");
    ConversionFailedException failure = new ConversionFailedException(TypeDescriptor.valueOf(String.class),
            property.getTypeDescriptor(), "not a number",
            new IllegalArgumentException("A null value cannot be assigned to a primitive type"));
    given(this.conversionService.convert("not a number", property.getTypeDescriptor())).willThrow(failure);
    Target target = new Target();
    ObjectPropertySetter setter = new ObjectPropertySetter(null, property, this.conversionService,
            typeHandler, null);

    setter.setTo(target, this.resultSet, 1);

    assertThat(target.count).isZero();
  }

  @Test
  void rethrowsSqlExceptionAndSuppressesConversionFailure() throws SQLException {
    SQLException sqlException = new SQLException("cannot read value");
    TypeHandler<?> typeHandler = mock(TypeHandler.class);
    given(typeHandler.getResult(this.resultSet, 1)).willThrow(sqlException);
    given(this.resultSet.getObject(1)).willReturn(42);
    BeanProperty property = BeanProperty.valueOf(Target.class, "name");
    ConversionFailedException failure = new ConversionFailedException(TypeDescriptor.valueOf(Integer.class),
            property.getTypeDescriptor(), 42, new IllegalArgumentException("conversion failed"));
    given(this.conversionService.convert(42, property.getTypeDescriptor())).willThrow(failure);
    Target target = new Target();
    ObjectPropertySetter setter = new ObjectPropertySetter(null, property, this.conversionService,
            typeHandler, null);

    assertThatExceptionOfType(SQLException.class)
            .isThrownBy(() -> setter.setTo(target, this.resultSet, 1))
            .isSameAs(sqlException)
            .satisfies(exception -> assertThat(exception.getSuppressed()).containsExactly(failure));
  }

  private ObjectPropertySetter setter(@Nullable NestedPropertyAccessor accessor, Class<?> targetType,
          String propertyName, TypeHandler<?> typeHandler) {
    return setter(accessor, BeanProperty.valueOf(targetType, propertyName), typeHandler);
  }

  private ObjectPropertySetter setter(@Nullable NestedPropertyAccessor accessor, BeanProperty property,
          TypeHandler<?> typeHandler) {
    return new ObjectPropertySetter(accessor, property, this.conversionService, typeHandler, null);
  }

  private TypeHandler<?> typeHandlerReturning(@Nullable Object value) throws SQLException {
    TypeHandler<?> typeHandler = mock(TypeHandler.class);
    given(typeHandler.getResult(this.resultSet, 1)).willReturn(value);
    return typeHandler;
  }

  private TypeHandler<?> typeHandlerFailing() throws SQLException {
    TypeHandler<?> typeHandler = mock(TypeHandler.class);
    given(typeHandler.getResult(this.resultSet, 1)).willThrow(new SQLException("cannot read value"));
    return typeHandler;
  }

  static class Target {

    public String name;

    public int count;

    public Nested nested;

    public final String fixed = "fixed";

    public final WrappedValue fixedWrapped;

    Target() {
      this(null);
    }

    Target(WrappedValue fixedWrapped) {
      this.fixedWrapped = fixedWrapped;
    }
  }

  static class Nested {

    public String name;

    public int count;
  }

  static class WrappedValue {
  }

}

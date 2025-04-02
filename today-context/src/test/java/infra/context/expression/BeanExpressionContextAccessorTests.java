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

package infra.context.expression;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanExpressionContext;
import infra.expression.AccessException;
import infra.expression.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 15:03
 */
class BeanExpressionContextAccessorTests {

  @Test
  void canReadExistingProperty() throws AccessException {
    BeanExpressionContext expressionContext = mock(BeanExpressionContext.class);
    when(expressionContext.containsObject("testProperty")).thenReturn(true);

    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();

    assertThat(accessor.canRead(null, expressionContext, "testProperty")).isTrue();
  }

  @Test
  void canReadWithNonBeanExpressionContextReturnsFalse() throws AccessException {
    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    assertThat(accessor.canRead(null, new Object(), "testProperty")).isFalse();
  }

  @Test
  void readReturnsPropertyValue() throws AccessException {
    Object expectedValue = new Object();
    BeanExpressionContext expressionContext = mock(BeanExpressionContext.class);
    when(expressionContext.getObject("testProperty")).thenReturn(expectedValue);

    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    TypedValue result = accessor.read(null, expressionContext, "testProperty");

    assertThat(result.getValue()).isSameAs(expectedValue);
  }

  @Test
  void readWithNonBeanExpressionContextThrowsException() {
    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();

    assertThatIllegalStateException()
            .isThrownBy(() -> accessor.read(null, new Object(), "testProperty"))
            .withMessage("Target must be of type BeanExpressionContext");
  }

  @Test
  void cannotWriteProperties() throws AccessException {
    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    BeanExpressionContext expressionContext = mock(BeanExpressionContext.class);

    assertThat(accessor.canWrite(null, expressionContext, "testProperty")).isFalse();
  }

  @Test
  void writeThrowsAccessException() {
    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    BeanExpressionContext expressionContext = mock(BeanExpressionContext.class);

    assertThatExceptionOfType(AccessException.class)
            .isThrownBy(() -> accessor.write(null, expressionContext, "testProperty", "value"))
            .withMessage("Beans in a BeanFactory are read-only");
  }

  @Test
  void specificTargetClassesReturnsBeanExpressionContext() {
    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    assertThat(accessor.getSpecificTargetClasses()).contains(BeanExpressionContext.class);
  }

  @Test
  void readWhenObjectNotExistsReturnsNull() throws AccessException {
    BeanExpressionContext expressionContext = mock(BeanExpressionContext.class);
    when(expressionContext.getObject("nonexistent")).thenReturn(null);

    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    TypedValue result = accessor.read(null, expressionContext, "nonexistent");

    assertThat(result.getValue()).isNull();
  }

  @Test
  void canReadWhenObjectNotExistsReturnsFalse() throws AccessException {
    BeanExpressionContext expressionContext = mock(BeanExpressionContext.class);
    when(expressionContext.containsObject("nonexistent")).thenReturn(false);

    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();

    assertThat(accessor.canRead(null, expressionContext, "nonexistent")).isFalse();
  }

  @Test
  void canWriteWithNullTarget() throws AccessException {
    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    assertThat(accessor.canWrite(null, null, "property")).isFalse();
  }

  @Test
  void writeWithNullTarget() {
    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    assertThatExceptionOfType(AccessException.class)
            .isThrownBy(() -> accessor.write(null, null, "property", "value"))
            .withMessage("Beans in a BeanFactory are read-only");
  }

  @Test
  void canReadNullName() throws AccessException {
    BeanExpressionContext expressionContext = mock(BeanExpressionContext.class);
    when(expressionContext.containsObject(null)).thenReturn(false);

    BeanExpressionContextAccessor accessor = new BeanExpressionContextAccessor();
    assertThat(accessor.canRead(null, expressionContext, null)).isFalse();
  }
}
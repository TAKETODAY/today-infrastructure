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
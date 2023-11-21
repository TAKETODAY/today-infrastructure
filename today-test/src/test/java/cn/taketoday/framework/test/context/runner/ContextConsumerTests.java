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

package cn.taketoday.framework.test.context.runner;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import cn.taketoday.context.ApplicationContext;

import java.util.function.IntPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ContextConsumer}.
 *
 * @author Stephane Nicoll
 */
class ContextConsumerTests {

  @Test
  void andThenInvokeInOrder() throws Throwable {
    IntPredicate predicate = mock(IntPredicate.class);
    given(predicate.test(42)).willReturn(true);
    given(predicate.test(24)).willReturn(false);
    ContextConsumer<ApplicationContext> firstConsumer = (context) -> assertThat(predicate.test(42)).isTrue();
    ContextConsumer<ApplicationContext> secondConsumer = (context) -> assertThat(predicate.test(24)).isFalse();
    firstConsumer.andThen(secondConsumer).accept(mock(ApplicationContext.class));
    InOrder ordered = inOrder(predicate);
    ordered.verify(predicate).test(42);
    ordered.verify(predicate).test(24);
    ordered.verifyNoMoreInteractions();
  }

  @Test
  void andThenNoInvokedIfThisFails() {
    IntPredicate predicate = mock(IntPredicate.class);
    given(predicate.test(42)).willReturn(true);
    given(predicate.test(24)).willReturn(false);
    ContextConsumer<ApplicationContext> firstConsumer = (context) -> assertThat(predicate.test(42)).isFalse();
    ContextConsumer<ApplicationContext> secondConsumer = (context) -> assertThat(predicate.test(24)).isFalse();
    assertThatThrownBy(() -> firstConsumer.andThen(secondConsumer).accept(mock(ApplicationContext.class)))
            .isInstanceOf(AssertionError.class);
    then(predicate).should().test(42);
    then(predicate).shouldHaveNoMoreInteractions();
  }

  @Test
  void andThenWithNull() {
    ContextConsumer<?> consumer = (context) -> {
    };
    assertThatIllegalArgumentException().isThrownBy(() -> consumer.andThen(null))
            .withMessage("After is required");
  }

}

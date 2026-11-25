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

package infra.beans.support;

import org.junit.jupiter.api.Test;

import infra.beans.BeanProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class BeanPropertyCopyStrategyTests {

  @Test
  void andOnlyEvaluatesNextStrategyWhenFirstAllowsCopy() {
    var first = mock(BeanPropertyCopyStrategy.class, CALLS_REAL_METHODS);
    var next = mock(BeanPropertyCopyStrategy.class);
    var source = new Object();
    var property = mock(BeanProperty.class);
    given(first.shouldCopy(source, property, "value")).willReturn(true);
    given(next.shouldCopy(source, property, "value")).willReturn(true);

    var combined = first.and(next);
    assertThat(combined.shouldCopy(source, property, null)).isFalse();
    verifyNoInteractions(next);
    assertThat(combined.shouldCopy(source, property, "value")).isTrue();
    verify(next).shouldCopy(source, property, "value");
  }

  @Test
  void orReturnsTrueWithoutEvaluatingNextStrategyWhenFirstAllowsCopy() {
    var first = mock(BeanPropertyCopyStrategy.class, CALLS_REAL_METHODS);
    var next = mock(BeanPropertyCopyStrategy.class);
    var source = new Object();
    var property = mock(BeanProperty.class);
    given(first.shouldCopy(source, property, "value")).willReturn(true);

    assertThat(first.or(next).shouldCopy(source, property, "value")).isTrue();
    verifyNoInteractions(next);
  }

  @Test
  void orDelegatesToNextStrategyWhenFirstRejectsCopy() {
    var first = mock(BeanPropertyCopyStrategy.class, CALLS_REAL_METHODS);
    var next = mock(BeanPropertyCopyStrategy.class);
    var source = new Object();
    var property = mock(BeanProperty.class);
    given(next.shouldCopy(source, property, "value")).willReturn(true);

    var combined = first.or(next);
    assertThat(combined.shouldCopy(source, property, "value")).isTrue();
    assertThat(combined.shouldCopy(source, property, null)).isFalse();
    verify(next).shouldCopy(source, property, "value");
    verify(next).shouldCopy(source, property, null);
  }

  @Test
  void rejectsNullStrategies() {
    var strategy = mock(BeanPropertyCopyStrategy.class, CALLS_REAL_METHODS);
    assertThatIllegalArgumentException().isThrownBy(() -> strategy.and(null));
    assertThatIllegalArgumentException().isThrownBy(() -> strategy.or(null));
  }

}

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

package infra.aop;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/20 22:46
 */
class TrueMethodMatcherTests {

  @Test
  void trueMethodMatcher() throws NoSuchMethodException {
    Method trueMethodMatcher = getClass().getDeclaredMethod("trueMethodMatcher");
    assertThat(TrueMethodMatcher.TRUE).isSameAs(TrueMethodMatcher.INSTANCE);
    assertThat(TrueMethodMatcher.INSTANCE.matches(trueMethodMatcher, int.class)).isTrue();
    assertThat(TrueMethodMatcher.INSTANCE.matches(trueMethodMatcher, long.class)).isTrue();
    assertThat(TrueMethodMatcher.INSTANCE.matches(trueMethodMatcher, double.class)).isTrue();
    assertThat(TrueMethodMatcher.INSTANCE.toString()).isEqualTo("MethodMatcher.TRUE");

    MethodInvocation methodInvocation = mock(MethodInvocation.class);

    assertThatThrownBy(() ->
            TrueMethodMatcher.INSTANCE.matches(methodInvocation))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Runtime is unsupported");

  }
}
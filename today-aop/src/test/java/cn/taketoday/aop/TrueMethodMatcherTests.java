/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop;

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
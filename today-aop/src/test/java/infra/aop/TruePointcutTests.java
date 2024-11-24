/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/20 22:53
 */
class TruePointcutTests {

  @Test
  void trueMethodMatcher() {
    assertThat(TruePointcut.TRUE).isSameAs(TruePointcut.INSTANCE);
    assertThat(TruePointcut.INSTANCE.getMethodMatcher()).isSameAs(MethodMatcher.TRUE).isSameAs(TrueMethodMatcher.INSTANCE);
    assertThat(TruePointcut.INSTANCE.getClassFilter()).isSameAs(ClassFilter.TRUE).isSameAs(TrueClassFilter.INSTANCE);
    assertThat(TruePointcut.INSTANCE.toString()).isEqualTo("Pointcut.TRUE");
  }
}
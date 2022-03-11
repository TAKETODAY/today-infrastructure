/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;

import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 21:35
 */
class AnnotatedElementKeyTests {

  private Method method;

  @BeforeEach
  void setUpMethod(TestInfo testInfo) {
    this.method = ReflectionUtils.findMethod(getClass(), testInfo.getTestMethod().get().getName());
  }

  @Test
  void sameInstanceEquals() {
    AnnotatedElementKey instance = new AnnotatedElementKey(this.method, getClass());

    assertKeyEquals(instance, instance);
  }

  @Test
  void equals() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, getClass());

    assertKeyEquals(first, second);
  }

  @Test
  void equalsNoTarget() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, null);
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, null);

    assertKeyEquals(first, second);
  }

  @Test
  void noTargetClassNotEquals() {
    AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
    AnnotatedElementKey second = new AnnotatedElementKey(this.method, null);

    assertThat(first.equals(second)).isFalse();
  }

  private void assertKeyEquals(AnnotatedElementKey first, AnnotatedElementKey second) {
    assertThat(second).isEqualTo(first);
    assertThat(second.hashCode()).isEqualTo(first.hashCode());
  }

}

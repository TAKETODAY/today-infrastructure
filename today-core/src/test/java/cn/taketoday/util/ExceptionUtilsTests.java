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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/19 23:02
 */
class ExceptionUtilsTests {

  @Test
  void sneakyThrow() {
    Exception exception = new Exception();
    assertThatThrownBy(() -> {
      ExceptionUtils.sneakyThrow(() -> {
        throw exception;
      });

    }).isSameAs(exception);

    assertThat(ExceptionUtils.sneakyThrow(() -> {
      return "";
    })).isEmpty();

  }

  @Test
  void unwrapIfNecessary() {
    Exception exception = new Exception();
    assertThat(ExceptionUtils.unwrapIfNecessary(exception)).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new InvocationTargetException(exception))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new UndeclaredThrowableException(exception))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new InvocationTargetException(new InvocationTargetException(exception)))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new InvocationTargetException(new UndeclaredThrowableException(exception)))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new UndeclaredThrowableException(new InvocationTargetException(exception)))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new UndeclaredThrowableException(new UndeclaredThrowableException(exception)))).isSameAs(exception);

    //
    assertThat(ExceptionUtils.unwrapIfNecessary(new IllegalStateException(new UndeclaredThrowableException(exception)))).isInstanceOf(IllegalStateException.class);
    assertThat(ExceptionUtils.unwrapIfNecessary(new IllegalStateException(new InvocationTargetException(exception)))).isInstanceOf(IllegalStateException.class);
  }

}
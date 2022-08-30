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

package cn.taketoday.util.function;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ThrowingFunction}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class ThrowingFunctionTests {

  @Test
  void applyWhenThrowingUncheckedExceptionThrowsOriginal() {
    ThrowingFunction<Object, Object> function = this::throwIllegalArgumentException;
    assertThatIllegalArgumentException().isThrownBy(() -> function.apply(this));
  }

  @Test
  void applyWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
    ThrowingFunction<Object, Object> function = this::throwIOException;
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
            () -> function.apply(this)).withCauseInstanceOf(IOException.class);
  }

  @Test
  void applyWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
    ThrowingFunction<Object, Object> function = this::throwIllegalArgumentException;
    assertThatIllegalArgumentException().isThrownBy(
            () -> function.apply(this, IllegalStateException::new));
  }

  @Test
  void applyWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
    ThrowingFunction<Object, Object> function = this::throwIOException;
    assertThatIllegalStateException().isThrownBy(() -> function.apply(this,
            IllegalStateException::new)).withCauseInstanceOf(IOException.class);
  }

  @Test
  void throwingModifiesThrownException() {
    ThrowingFunction<Object, Object> function = this::throwIOException;
    ThrowingFunction<Object, Object> modified = function.throwing(
            IllegalStateException::new);
    assertThatIllegalStateException().isThrownBy(
            () -> modified.apply(this)).withCauseInstanceOf(IOException.class);
  }

  @Test
  void ofModifiesThrowException() {
    ThrowingFunction<Object, Object> function = ThrowingFunction.of(
            this::throwIOException, IllegalStateException::new);
    assertThatIllegalStateException().isThrownBy(
            () -> function.apply(this)).withCauseInstanceOf(IOException.class);
  }

  private Object throwIOException(Object o) throws IOException {
    throw new IOException();
  }

  private Object throwIllegalArgumentException(Object o) throws IOException {
    throw new IllegalArgumentException();
  }

}

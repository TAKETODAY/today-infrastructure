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
 * Tests for {@link ThrowingConsumer}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ThrowingConsumerTests {

  @Test
  void applyWhenThrowingUncheckedExceptionThrowsOriginal() {
    ThrowingConsumer<Object> consumer = this::throwIllegalArgumentException;
    assertThatIllegalArgumentException().isThrownBy(() -> consumer.accept(this));
  }

  @Test
  void applyWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
    ThrowingConsumer<Object> consumer = this::throwIOException;
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
            () -> consumer.accept(this)).withCauseInstanceOf(IOException.class);
  }

  @Test
  void applyWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
    ThrowingConsumer<Object> consumer = this::throwIllegalArgumentException;
    assertThatIllegalArgumentException().isThrownBy(
            () -> consumer.accept(this, IllegalStateException::new));
  }

  @Test
  void applyWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
    ThrowingConsumer<Object> consumer = this::throwIOException;
    assertThatIllegalStateException().isThrownBy(() -> consumer.accept(this,
            IllegalStateException::new)).withCauseInstanceOf(IOException.class);
  }

  @Test
  void throwingModifiesThrownException() {
    ThrowingConsumer<Object> consumer = this::throwIOException;
    ThrowingConsumer<Object> modified = consumer.throwing(
            IllegalStateException::new);
    assertThatIllegalStateException().isThrownBy(
            () -> modified.accept(this)).withCauseInstanceOf(IOException.class);
  }

  @Test
  void ofModifiesThrowException() {
    ThrowingConsumer<Object> consumer = ThrowingConsumer.of(this::throwIOException,
            IllegalStateException::new);
    assertThatIllegalStateException().isThrownBy(
            () -> consumer.accept(this)).withCauseInstanceOf(IOException.class);
  }

  private void throwIOException(Object o) throws IOException {
    throw new IOException();
  }

  private void throwIllegalArgumentException(Object o) throws IOException {
    throw new IllegalArgumentException();
  }

}

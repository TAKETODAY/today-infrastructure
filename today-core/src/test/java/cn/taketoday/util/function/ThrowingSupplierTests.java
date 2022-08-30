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
 * Tests for {@link ThrowingSupplier}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class ThrowingSupplierTests {

  @Test
  void getWhenThrowingUncheckedExceptionThrowsOriginal() {
    ThrowingSupplier<Object> supplier = this::throwIllegalArgumentException;
    assertThatIllegalArgumentException().isThrownBy(supplier::get);
  }

  @Test
  void getWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
    ThrowingSupplier<Object> supplier = this::throwIOException;
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
            supplier::get).withCauseInstanceOf(IOException.class);
  }

  @Test
  void getWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
    ThrowingSupplier<Object> supplier = this::throwIllegalArgumentException;
    assertThatIllegalArgumentException().isThrownBy(
            () -> supplier.get(IllegalStateException::new));
  }

  @Test
  void getWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
    ThrowingSupplier<Object> supplier = this::throwIOException;
    assertThatIllegalStateException().isThrownBy(
            () -> supplier.get(IllegalStateException::new)).withCauseInstanceOf(
            IOException.class);
  }

  @Test
  void throwingModifiesThrownException() {
    ThrowingSupplier<Object> supplier = this::throwIOException;
    ThrowingSupplier<Object> modified = supplier.throwing(
            IllegalStateException::new);
    assertThatIllegalStateException().isThrownBy(
            () -> modified.get()).withCauseInstanceOf(IOException.class);
  }

  @Test
  void ofModifiesThrowException() {
    ThrowingSupplier<Object> supplier = ThrowingSupplier.of(
            this::throwIOException, IllegalStateException::new);
    assertThatIllegalStateException().isThrownBy(
            () -> supplier.get()).withCauseInstanceOf(IOException.class);
  }

  private Object throwIOException() throws IOException {
    throw new IOException();
  }

  private Object throwIllegalArgumentException() throws IOException {
    throw new IllegalArgumentException();
  }

}

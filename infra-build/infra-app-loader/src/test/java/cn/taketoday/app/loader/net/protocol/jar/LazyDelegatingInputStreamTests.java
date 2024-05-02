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

package cn.taketoday.app.loader.net.protocol.jar;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LazyDelegatingInputStream}.
 *
 * @author Phillip Webb
 */
class LazyDelegatingInputStreamTests {

  private InputStream delegate = mock(InputStream.class);

  private TestLazyDelegatingInputStream inputStream = new TestLazyDelegatingInputStream();

  @Test
  void noOperationsDoesNotGetDelegateInputStream() {
    then(this.delegate).shouldHaveNoInteractions();
  }

  @Test
  void readDelegatesToInputStream() throws Exception {
    this.inputStream.read();
    then(this.delegate).should().read();
  }

  @Test
  void readWithByteArrayDelegatesToInputStream() throws Exception {
    byte[] bytes = new byte[1];
    this.inputStream.read(bytes);
    then(this.delegate).should().read(bytes);
  }

  @Test
  void readWithByteArrayAndOffsetAndLenDelegatesToInputStream() throws Exception {
    byte[] bytes = new byte[1];
    this.inputStream.read(bytes, 0, 1);
    then(this.delegate).should().read(bytes, 0, 1);
  }

  @Test
  void skipDelegatesToInputStream() throws Exception {
    this.inputStream.skip(10);
    then(this.delegate).should().skip(10);
  }

  @Test
  void availableDelegatesToInputStream() throws Exception {
    this.inputStream.available();
    then(this.delegate).should().available();
  }

  @Test
  void markSupportedDelegatesToInputStream() {
    this.inputStream.markSupported();
    then(this.delegate).should().markSupported();
  }

  @Test
  void markDelegatesToInputStream() {
    this.inputStream.mark(10);
    then(this.delegate).should().mark(10);
  }

  @Test
  void resetDelegatesToInputStream() throws Exception {
    this.inputStream.reset();
    then(this.delegate).should().reset();
  }

  @Test
  void closeWhenDelegateNotCreatedDoesNothing() throws Exception {
    this.inputStream.close();
    then(this.delegate).shouldHaveNoInteractions();
  }

  @Test
  void closeDelegatesToInputStream() throws Exception {
    this.inputStream.available();
    this.inputStream.close();
    then(this.delegate).should().close();
  }

  @Test
  void getDelegateInputStreamIsOnlyCalledOnce() throws Exception {
    this.inputStream.available();
    this.inputStream.mark(10);
    this.inputStream.read();
    assertThat(this.inputStream.count).isOne();
  }

  private final class TestLazyDelegatingInputStream extends LazyDelegatingInputStream {

    private int count;

    @Override
    protected InputStream getDelegateInputStream() throws IOException {
      this.count++;
      return LazyDelegatingInputStreamTests.this.delegate;
    }

  }

}

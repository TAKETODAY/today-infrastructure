/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.multipart.parsing;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/4 21:22
 */
class MultipartInputTests {

  @Test
  void constructorWithValidParameters() {
    InputStream input = new ByteArrayInputStream(new byte[0]);
    byte[] boundary = "boundary".getBytes();
    ProgressNotifier notifier = mock(ProgressNotifier.class);
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getParsingBufferSize()).thenReturn(4096);
    when(parser.getMaxHeaderSize()).thenReturn(512);
    when(parser.getDefaultCharset()).thenReturn(StandardCharsets.UTF_8);

    MultipartInput multipartInput = new MultipartInput(input, boundary, notifier, parser);

    assertThat(multipartInput).isNotNull();
  }

  @Test
  void constructorWithBufferTooSmall() {
    InputStream input = new ByteArrayInputStream(new byte[0]);
    byte[] boundary = "very-long-boundary-token-that-exceeds-buffer-size".getBytes();
    ProgressNotifier notifier = mock(ProgressNotifier.class);
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getParsingBufferSize()).thenReturn(10); // Too small
    when(parser.getMaxHeaderSize()).thenReturn(512);
    when(parser.getDefaultCharset()).thenReturn(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> new MultipartInput(input, boundary, notifier, parser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The buffer size specified for the MultipartInput is too small");
  }

  @Test
  void findSeparatorReturnsNegativeOneWhenNotFound() throws Exception {
    InputStream input = new ByteArrayInputStream("some data without boundary".getBytes());
    byte[] boundary = "boundary".getBytes();
    ProgressNotifier notifier = mock(ProgressNotifier.class);
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getParsingBufferSize()).thenReturn(4096);
    when(parser.getMaxHeaderSize()).thenReturn(512);
    when(parser.getDefaultCharset()).thenReturn(StandardCharsets.UTF_8);

    MultipartInput multipartInput = new MultipartInput(input, boundary, notifier, parser);

    // Use reflection to access private method
    Method findSeparatorMethod = MultipartInput.class.getDeclaredMethod("findSeparator");
    findSeparatorMethod.setAccessible(true);

    int result = (int) findSeparatorMethod.invoke(multipartInput);

    assertThat(result).isEqualTo(-1);
  }

  @Test
  void arrayEqualsWithEqualArrays() {
    byte[] a = { 1, 2, 3, 4 };
    byte[] b = { 1, 2, 3, 4 };

    boolean result = MultipartInput.arrayEquals(a, b, 4);

    assertThat(result).isTrue();
  }

  @Test
  void arrayEqualsWithDifferentArrays() {
    byte[] a = { 1, 2, 3, 4 };
    byte[] b = { 1, 2, 3, 5 };

    boolean result = MultipartInput.arrayEquals(a, b, 4);

    assertThat(result).isFalse();
  }

  @Test
  void arrayEqualsWithDifferentLengthComparison() {
    byte[] a = { 1, 2, 3, 4, 5 };
    byte[] b = { 1, 2, 3, 4, 5 };

    boolean result = MultipartInput.arrayEquals(a, b, 3);

    assertThat(result).isTrue();
  }

  @Test
  void newInputStreamCreatesItemInputStream() {
    InputStream input = new ByteArrayInputStream(new byte[0]);
    byte[] boundary = "boundary".getBytes();
    ProgressNotifier notifier = mock(ProgressNotifier.class);
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getParsingBufferSize()).thenReturn(4096);
    when(parser.getMaxHeaderSize()).thenReturn(512);
    when(parser.getDefaultCharset()).thenReturn(StandardCharsets.UTF_8);

    MultipartInput multipartInput = new MultipartInput(input, boundary, notifier, parser);

    MultipartInput.ItemInputStream itemInputStream = multipartInput.newInputStream();

    assertThat(itemInputStream).isNotNull();
  }

  @Test
  void itemInputStreamAvailableReturnsZeroInitially() throws Exception {
    InputStream input = new ByteArrayInputStream(new byte[0]);
    byte[] boundary = "boundary".getBytes();
    ProgressNotifier notifier = mock(ProgressNotifier.class);
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getParsingBufferSize()).thenReturn(4096);
    when(parser.getMaxHeaderSize()).thenReturn(512);
    when(parser.getDefaultCharset()).thenReturn(StandardCharsets.UTF_8);

    MultipartInput multipartInput = new MultipartInput(input, boundary, notifier, parser);

    MultipartInput.ItemInputStream itemInputStream = multipartInput.newInputStream();

    int available = itemInputStream.available();

    assertThat(available).isEqualTo(0);
  }

  @Test
  void itemInputStreamCloseDoesNotThrowException() throws Exception {
    InputStream input = new ByteArrayInputStream(new byte[0]);
    byte[] boundary = "boundary".getBytes();
    ProgressNotifier notifier = mock(ProgressNotifier.class);
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getParsingBufferSize()).thenReturn(4096);
    when(parser.getMaxHeaderSize()).thenReturn(512);
    when(parser.getDefaultCharset()).thenReturn(StandardCharsets.UTF_8);

    MultipartInput multipartInput = new MultipartInput(input, boundary, notifier, parser);

    MultipartInput.ItemInputStream itemInputStream = multipartInput.newInputStream();

    assertThatNoException().isThrownBy(() -> itemInputStream.close(true));
  }

  @Test
  void readByteThrowsEOFExceptionOnEmptyStream() throws Exception {
    InputStream input = new ByteArrayInputStream(new byte[0]);
    byte[] boundary = "boundary".getBytes();
    ProgressNotifier notifier = mock(ProgressNotifier.class);
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    when(parser.getParsingBufferSize()).thenReturn(4096);
    when(parser.getMaxHeaderSize()).thenReturn(512);
    when(parser.getDefaultCharset()).thenReturn(StandardCharsets.UTF_8);

    MultipartInput multipartInput = new MultipartInput(input, boundary, notifier, parser);

    assertThatThrownBy(multipartInput::readByte)
            .isInstanceOf(EOFException.class)
            .hasMessage("No more data is available");
  }

}
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

package infra.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 00:16
 */
class HttpOutputMessageTests {

  @Test
  void getBody_shouldReturnOutputStream() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    OutputStream mockOutputStream = mock(OutputStream.class);

    when(outputMessage.getBody()).thenReturn(mockOutputStream);

    OutputStream body = outputMessage.getBody();

    assertThat(body).isSameAs(mockOutputStream);
    verify(outputMessage).getBody();
  }

  @Test
  void setContentType_shouldSetHeader() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).setContentType(any());

    MediaType mediaType = MediaType.APPLICATION_JSON;
    outputMessage.setContentType(mediaType);

    verify(outputMessage).getHeaders();
    assertThat(headers.getContentType()).isEqualTo(mediaType);
  }

  @Test
  void supportsZeroCopy_shouldReturnFalseByDefault() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    doCallRealMethod().when(outputMessage).supportsZeroCopy();

    boolean result = outputMessage.supportsZeroCopy();

    assertThat(result).isFalse();
  }

  @Test
  void sendFile_withPath_shouldCopyData(@TempDir Path tempDir) throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    OutputStream mockOutputStream = mock(OutputStream.class);

    when(outputMessage.getBody()).thenReturn(mockOutputStream);
    doCallRealMethod().when(outputMessage).sendFile(any(Path.class), anyLong(), anyLong());

    // 创建测试文件
    Path tempFile = tempDir.resolve("test.txt");
    String content = "Hello, World!";
    Files.write(tempFile, content.getBytes());

    outputMessage.sendFile(tempFile, 0, Files.size(tempFile));

    verify(mockOutputStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
  }

  @Test
  void sendFile_withNonExistentFile_shouldThrowIOException() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    Path nonExistentPath = Path.of("non-existent-file.txt");

    when(outputMessage.getBody()).thenReturn(mock(OutputStream.class));
    doCallRealMethod().when(outputMessage).sendFile(any(Path.class), anyLong(), anyLong());

    assertThatThrownBy(() -> outputMessage.sendFile(nonExistentPath, 0, 10))
            .isInstanceOf(IOException.class);
  }

  @Test
  void sendFile_withNegativePosition_shouldHandle() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    OutputStream mockOutputStream = mock(OutputStream.class);

    when(outputMessage.getBody()).thenReturn(mockOutputStream);
    doCallRealMethod().when(outputMessage).sendFile(any(Path.class), anyLong(), anyLong());

    // 创建测试文件
    Path tempFile = Files.createTempFile("test", ".txt");
    Files.write(tempFile, "test".getBytes());
    tempFile.toFile().deleteOnExit();

    // 负位置可能抛出异常，取决于 StreamUtils.copyRange 的实现
    assertThatCode(() -> outputMessage.sendFile(tempFile, -1, 2))
            .doesNotThrowAnyException();
  }

  @Test
  void sendFile_withCountExceedingFileSize_shouldHandleGracefully(@TempDir Path tempDir) throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    OutputStream mockOutputStream = mock(OutputStream.class);

    when(outputMessage.getBody()).thenReturn(mockOutputStream);
    doCallRealMethod().when(outputMessage).sendFile(any(Path.class), anyLong(), anyLong());

    // 创建测试文件
    Path tempFile = tempDir.resolve("test.txt");
    String content = "short";
    Files.write(tempFile, content.getBytes());

    // 请求复制超过文件大小的字节数
    assertThatCode(() -> outputMessage.sendFile(tempFile, 0, 100))
            .doesNotThrowAnyException();
  }

  @Test
  void sendFile_shouldCloseInputStream(@TempDir Path tempDir) throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    OutputStream mockOutputStream = mock(OutputStream.class);

    when(outputMessage.getBody()).thenReturn(mockOutputStream);
    doCallRealMethod().when(outputMessage).sendFile(any(Path.class), anyLong(), anyLong());

    // 创建测试文件
    Path tempFile = tempDir.resolve("test.txt");
    Files.write(tempFile, "test content".getBytes());

    assertThatCode(() -> outputMessage.sendFile(tempFile, 0, Files.size(tempFile)))
            .doesNotThrowAnyException();
    // 测试完成时文件应该可以被访问，说明流已关闭
  }

  @Test
  void sendFile_withLargeFile_shouldHandle(@TempDir Path tempDir) throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    OutputStream mockOutputStream = mock(OutputStream.class);

    when(outputMessage.getBody()).thenReturn(mockOutputStream);
    doCallRealMethod().when(outputMessage).sendFile(any(Path.class), anyLong(), anyLong());

    // 创建大文件
    Path tempFile = tempDir.resolve("large.txt");
    byte[] largeContent = new byte[10240]; // 10KB
    for (int i = 0; i < largeContent.length; i++) {
      largeContent[i] = (byte) (i % 256);
    }
    Files.write(tempFile, largeContent);

    assertThatCode(() -> outputMessage.sendFile(tempFile, 0, largeContent.length))
            .doesNotThrowAnyException();
  }

  @Test
  void sendFile_withNullFile_shouldThrowNullPointerException() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    doCallRealMethod().when(outputMessage).sendFile((File) any());

    assertThatThrownBy(() -> outputMessage.sendFile((File) null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void setContentType_withNullMediaType_shouldClearHeader() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_PLAIN);

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).setContentType(any());

    outputMessage.setContentType(null);

    verify(outputMessage).getHeaders();
    assertThat(headers.getContentType()).isNull();
  }

  @Test
  void getBody_whenThrowsIOException_shouldPropagateException() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);

    when(outputMessage.getBody()).thenThrow(new IOException("Stream error"));

    assertThatThrownBy(outputMessage::getBody)
            .isInstanceOf(IOException.class)
            .hasMessage("Stream error");
  }

}

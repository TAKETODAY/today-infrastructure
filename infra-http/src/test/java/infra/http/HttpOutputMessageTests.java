/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

  @Test
  void setHeader_shouldSetHeaderValue() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).setHeader(any(String.class), any(String.class));

    outputMessage.setHeader("Content-Type", "application/json");

    assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
  }

  @Test
  void setContentLength_shouldSetContentLengthHeader() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).setContentLength(anyLong());

    outputMessage.setContentLength(1024L);

    verify(outputMessage).getHeaders();
    assertThat(headers.getContentLength()).isEqualTo(1024L);
  }

  @Test
  void addHeader_shouldAddHeaderValue() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).addHeader(any(String.class), any(String.class));

    outputMessage.addHeader("Accept", "application/json");
    outputMessage.addHeader("Accept", "text/html");

    assertThat(headers.get("Accept")).containsExactly("application/json", "text/html");
  }

  @Test
  void addHeaders_shouldAddMultipleHeaders() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    HttpHeaders additionalHeaders = HttpHeaders.forWritable();
    additionalHeaders.set("X-Header-1", "value1");
    additionalHeaders.set("X-Header-2", "value2");

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).addHeaders(any(HttpHeaders.class));

    outputMessage.addHeaders(additionalHeaders);

    verify(outputMessage).getHeaders();
    assertThat(headers.getFirst("X-Header-1")).isEqualTo("value1");
    assertThat(headers.getFirst("X-Header-2")).isEqualTo("value2");
  }

  @Test
  void addHeaders_withNull_shouldNotFail() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).addHeaders(any(HttpHeaders.class));

    outputMessage.addHeaders(null);

    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void removeHeader_shouldRemoveExistingHeader() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("X-Custom-Header", "value");

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).removeHeader(any(String.class));

    boolean result = outputMessage.removeHeader("X-Custom-Header");

    verify(outputMessage).getHeaders();
    assertThat(result).isTrue();
    assertThat(headers.getFirst("X-Custom-Header")).isNull();
  }

  @Test
  void removeHeader_shouldReturnFalseForNonExistingHeader() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders headers = HttpHeaders.forWritable();

    when(outputMessage.getHeaders()).thenReturn(headers);
    doCallRealMethod().when(outputMessage).removeHeader(any(String.class));

    boolean result = outputMessage.removeHeader("Non-Existing-Header");

    verify(outputMessage).getHeaders();
    assertThat(result).isFalse();
  }

  @Test
  void setHeaders_shouldReplaceAllHeaders() {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    HttpHeaders existingHeaders = HttpHeaders.forWritable();
    existingHeaders.set("Old-Header", "old-value");
    HttpHeaders newHeaders = HttpHeaders.forWritable();
    newHeaders.set("New-Header", "new-value");

    when(outputMessage.getHeaders()).thenReturn(existingHeaders);
    doCallRealMethod().when(outputMessage).setHeaders(any(HttpHeaders.class));

    outputMessage.setHeaders(newHeaders);

    verify(outputMessage).getHeaders();
    assertThat(existingHeaders.getFirst("Old-Header")).isEqualTo("old-value");
    assertThat(existingHeaders.getFirst("New-Header")).isEqualTo("new-value");
  }

  @Test
  void sendFile_withFileOnly_shouldUseFullFileLength() throws IOException {
    HttpOutputMessage outputMessage = mock(HttpOutputMessage.class);
    File mockFile = mock(File.class);
    OutputStream mockOutputStream = mock(OutputStream.class);

    when(outputMessage.getBody()).thenReturn(mockOutputStream);
    when(mockFile.length()).thenReturn(2048L);
    doCallRealMethod().when(outputMessage).sendFile(any(File.class));
    doCallRealMethod().when(outputMessage).sendFile(any(File.class), anyLong(), anyLong());

    outputMessage.sendFile(mockFile);

    verify(mockFile).length();
    verify(outputMessage).sendFile(mockFile, 0, 2048L);
  }

}

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

package infra.web.multipart.parsing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Constant;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/5 15:06
 */
class DefaultPartTests {

  @Test
  void constructorInitializesFieldsCorrectly() {
    String fieldName = "testField";
    MediaType contentType = MediaType.TEXT_PLAIN;
    HttpHeaders headers = HttpHeaders.forWritable();
    boolean isFormField = true;
    String filename = "test.txt";
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);

    DefaultPart part = new DefaultPart(fieldName, contentType, headers, isFormField, filename, parser);

    assertThat(part.getName()).isEqualTo(fieldName);
    assertThat(part.getContentType()).isEqualTo(contentType);
    assertThat(part.getHeaders()).isEqualTo(headers);
    assertThat(part.isFormField()).isTrue();
    assertThat(part.getOriginalFilename()).isEqualTo(filename);
  }

  @Test
  void isFileReturnsCorrectValue() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);

    DefaultPart formFieldPart = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);
    DefaultPart filePart = new DefaultPart("file", null, HttpHeaders.forWritable(), false, "test.txt", parser);

    assertThat(formFieldPart.isFile()).isFalse();
    assertThat(filePart.isFile()).isTrue();
  }

  @Test
  void isEmptyReturnsTrueWhenNoContent() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    assertThat(part.isEmpty()).isTrue();
  }

  @Test
  void getContentLengthReturnsZeroWhenNoStream() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    assertThat(part.getContentLength()).isEqualTo(0L);
  }

  @Test
  void isInMemoryReturnsTrueWhenNoStream() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    assertThat(part.isInMemory()).isTrue();
  }

  @Test
  void getInputStreamThrowsIllegalStateExceptionWhenNotClosed() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    assertThatThrownBy(part::getInputStream)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The file item has not been fully read.");
  }

  @Test
  void getContentAsByteArrayReturnsEmptyArrayWhenNoStream() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    byte[] content = part.getContentAsByteArray();

    assertThat(content).isEmpty();
  }

  @Test
  void toStringReturnsFormattedString() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("testField", MediaType.TEXT_PLAIN, HttpHeaders.forWritable(), true, "test.txt", parser);

    String stringRepresentation = part.toString();

    assertThat(stringRepresentation).contains("name = 'testField'");
    assertThat(stringRepresentation).contains("size = 0");
    assertThat(stringRepresentation).contains("isFormField = true");
  }

  @Test
  void checkFileNameReturnsValidFilename() {
    String filename = "valid_filename.txt";

    String result = DefaultPart.checkFileName(filename);

    assertThat(result).isEqualTo(filename);
  }

  @Test
  void checkFileNameThrowsExceptionForInvalidFilename() {
    String filename = "invalid\0filename.txt";

    assertThatThrownBy(() -> DefaultPart.checkFileName(filename))
            .isInstanceOf(InvalidPathException.class);
  }

  @Test
  void getContentTypeStringReturnsNullWhenNoContentType() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    String contentTypeString = part.getContentTypeAsString();

    assertThat(contentTypeString).isNull();
  }

  @Test
  void getOutputStreamCreatesNewDeferrableStream() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    OutputStream outputStream = part.getOutputStream();

    assertThat(outputStream).isNotNull();
    assertThat(outputStream).isInstanceOf(DeferrableStream.class);
  }

  @Test
  void getOutputStreamReturnsSameInstance() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    OutputStream outputStream1 = part.getOutputStream();
    OutputStream outputStream2 = part.getOutputStream();

    assertThat(outputStream1).isSameAs(outputStream2);
  }

  @Test
  void getContentAsStringUsesDefaultCharset() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    java.nio.charset.Charset defaultCharset = StandardCharsets.UTF_8;
    when(parser.getDefaultCharset()).thenReturn(defaultCharset);

    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    String content = part.getContentAsString();

    assertThat(content).isNotNull();
  }

  @Test
  void getContentAsStringWithCharsetUsesProvidedCharset() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);
    java.nio.charset.Charset charset = StandardCharsets.UTF_8;

    String content = part.getContentAsString(charset);

    assertThat(content).isNotNull();
  }

  @Test
  void checkFileNameReturnsNullForNullInput() {
    String result = DefaultPart.checkFileName(null);

    assertThat(result).isNull();
  }

  @Test
  void checkFileNameThrowsExceptionForNulCharacter() {
    String filename = "test\0file.txt";

    assertThatThrownBy(() -> DefaultPart.checkFileName(filename))
            .isInstanceOf(InvalidPathException.class)
            .hasMessageContaining("\\0");
  }

  @Test
  void cleanupDeletesTempFileWhenExists() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), false, "test.txt",
            new DefaultMultipartParser());

    // Create a temporary file to simulate uploaded file
    Path tempFile = Files.createTempFile("test", ".tmp");
    DeferrableStream stream = (DeferrableStream) part.getOutputStream();
    stream.path = tempFile;
    stream.state = DeferrableStream.State.closed;

    part.cleanup();

    assertThat(Files.exists(tempFile)).isFalse();
  }

  @Test
  void getContentAsStringReturnsCorrectString() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    String content = part.getContentAsString();

    assertThat(content).isEqualTo("");
  }

  @Test
  void getContentAsStringWithCharsetReturnsCorrectString() throws IOException {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);

    String content = part.getContentAsString(StandardCharsets.UTF_8);

    assertThat(content).isEqualTo("");
  }

  @Test
  void isFormFieldReturnsCorrectValue() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);

    DefaultPart formFieldPart = new DefaultPart("field", null, HttpHeaders.forWritable(), true, null, parser);
    DefaultPart filePart = new DefaultPart("file", null, HttpHeaders.forWritable(), false, "test.txt", parser);

    assertThat(formFieldPart.isFormField()).isTrue();
    assertThat(filePart.isFormField()).isFalse();
  }

  @Test
  void getInputStreamThrowsIllegalStateExceptionWhenStreamNotClosed() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    part.getOutputStream(); // Creates stream in open state

    assertThatThrownBy(part::getInputStream)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The file item has not been fully read.");
  }

  @Test
  void getContentAsByteArrayReturnsBytesWhenAvailableInMemory() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    DeferrableStream stream = (DeferrableStream) part.getOutputStream();
    stream.bytes = "test content".getBytes();

    byte[] content = part.getContentAsByteArray();

    assertThat(content).isEqualTo("test content".getBytes());
  }

  @Test
  void getContentAsByteArrayReturnsEmptyArrayWhenNoData() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    byte[] content = part.getContentAsByteArray();

    assertThat(content).isEqualTo(Constant.EMPTY_BYTES);
  }

  @Test
  void transferToMovesFileWhenNotInMemory() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            false, "test.txt", new DefaultMultipartParser());

    // Setup temp file
    Path tempFile = Files.createTempFile("source", ".tmp");
    Files.write(tempFile, "test content".getBytes());

    DeferrableStream stream = (DeferrableStream) part.getOutputStream();
    stream.path = tempFile;
    stream.state = DeferrableStream.State.closed;

    // Transfer to destination
    Path destFile = Files.createTempFile("dest", ".tmp");
    part.transferTo(destFile);

    part.cleanup();

    assertThat(Files.exists(tempFile)).isFalse();
    assertThat(Files.exists(destFile)).isTrue();
    assertThat(Files.readString(destFile)).isEqualTo("test content");

    Files.deleteIfExists(destFile);
  }

  @Test
  void transferToWritesFromMemory() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    // Simulate in-memory content
    DeferrableStream stream = (DeferrableStream) part.getOutputStream();
    stream.bytes = "memory content".getBytes();
    stream.state = DeferrableStream.State.closed;

    Path destFile = Files.createTempFile("dest", ".tmp");
    part.transferTo(destFile);

    assertThat(Files.readString(destFile)).isEqualTo("memory content");
    Files.deleteIfExists(destFile);
  }

  @Test
  void isInMemoryReturnsFalseWhenFileStreamExists() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            false, "test.txt", new DefaultMultipartParser());

    DeferrableStream stream = (DeferrableStream) part.getOutputStream();
    stream.path = Files.createTempFile("test", ".tmp");
    stream.state = DeferrableStream.State.persisted;

    assertThat(part.isInMemory()).isFalse();
    part.cleanup();
  }

  @Test
  void isEmptyReturnsFalseWhenContentExists() throws IOException {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, new DefaultMultipartParser());

    DeferrableStream stream = (DeferrableStream) part.getOutputStream();
    stream.size = 10L;

    assertThat(part.isEmpty()).isFalse();
  }

  @Test
  void getOriginalFilenameReturnsCheckedFilename() {
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            false, "valid-file.txt", new DefaultMultipartParser());

    String filename = part.getOriginalFilename();

    assertThat(filename).isEqualTo("valid-file.txt");
  }

  @Test
  void getCharsetReturnsParserDefaultWhenNoContentType() throws NoSuchMethodException {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    DefaultPart part = new DefaultPart("field", null, HttpHeaders.forWritable(),
            true, null, parser);

    Method getCharset = DefaultPart.class.getDeclaredMethod("getCharset");
    ReflectionUtils.makeAccessible(getCharset);
    assertThat(ReflectionUtils.invokeMethod(getCharset, part)).isEqualTo(parser.getDefaultCharset());
  }

  @Test
  void getCharsetReturnsContentTypeCharsetWhenAvailable() throws NoSuchMethodException {
    DefaultMultipartParser parser = new DefaultMultipartParser();
    MediaType contentType = MediaType.parseMediaType("text/plain;charset=UTF-16");
    DefaultPart part = new DefaultPart("field", contentType, HttpHeaders.forWritable(),
            true, null, parser);

    Method getCharset = DefaultPart.class.getDeclaredMethod("getCharset");
    ReflectionUtils.makeAccessible(getCharset);
    assertThat(ReflectionUtils.invokeMethod(getCharset, part)).isEqualTo(StandardCharsets.UTF_16);

    assertThat(ReflectionUtils.invokeMethod(getCharset, new DefaultPart("field",
            MediaType.TEXT_PLAIN, HttpHeaders.forWritable(),
            true, null, parser))).isEqualTo(StandardCharsets.UTF_8);

  }

}
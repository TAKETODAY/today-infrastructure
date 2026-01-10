/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.testfixture;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.FileCopyUtils;
import infra.web.multipart.Part;

/**
 * Mock implementation of the {@link Part}
 * interface.
 *
 * @author Juergen Hoeller
 * @author Eric Crampton
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 17:04
 */
@SuppressWarnings("serial")
public class MockMultipartFile implements Part {

  private final String name;

  private final String originalFilename;

  @Nullable
  private final String contentType;

  private final byte[] content;
  protected HttpHeaders headers;

  /**
   * Create a new MockMultipartFile with the given content.
   *
   * @param name the name of the file
   * @param content the content of the file
   */
  public MockMultipartFile(String name, byte @Nullable [] content) {
    this(name, "", null, content);
  }

  /**
   * Create a new MockMultipartFile with the given content.
   *
   * @param name the name of the file
   * @param contentStream the content of the file as stream
   * @throws IOException if reading from the stream failed
   */
  public MockMultipartFile(String name, InputStream contentStream) throws IOException {
    this(name, "", null, FileCopyUtils.copyToByteArray(contentStream));
  }

  /**
   * Create a new MockMultipartFile with the given content.
   *
   * @param name the name of the file
   * @param originalFilename the original filename (as on the client's machine)
   * @param contentType the content type (if known)
   * @param content the content of the file
   */
  public MockMultipartFile(
          String name, @Nullable String originalFilename, @Nullable String contentType, byte @Nullable [] content) {

    Assert.hasLength(name, "Name must not be empty");
    this.name = name;
    this.originalFilename = (originalFilename != null ? originalFilename : "");
    this.contentType = contentType;
    this.content = (content != null ? content : new byte[0]);
  }

  /**
   * Create a new MockMultipartFile with the given content.
   *
   * @param name the name of the file
   * @param originalFilename the original filename (as on the client's machine)
   * @param contentType the content type (if known)
   * @param contentStream the content of the file as stream
   * @throws IOException if reading from the stream failed
   */
  public MockMultipartFile(
          String name, @Nullable String originalFilename, @Nullable String contentType, InputStream contentStream)
          throws IOException {

    this(name, originalFilename, contentType, FileCopyUtils.copyToByteArray(contentStream));
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getContentAsString() {
    return new String(content);
  }

  @Override
  public boolean isInMemory() {
    return true;
  }

  @Override
  public boolean isFormField() {
    return false;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  public void setHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public HttpHeaders getHeaders() {
    HttpHeaders headers = this.headers;
    if (headers == null) {
      headers = createHttpHeaders();
      this.headers = headers;
    }
    return headers;
  }

  protected DefaultHttpHeaders createHttpHeaders() {
    DefaultHttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(getContentType());
    return headers;
  }

  @Override
  public String getOriginalFilename() {
    return originalFilename;
  }

  @Override
  @Nullable
  public MediaType getContentType() {
    return MediaType.parseMediaType(contentType);
  }

  @Override
  public boolean isEmpty() {
    return (this.content.length == 0);
  }

  @Override
  public long getContentLength() {
    return this.content.length;
  }

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    return this.content;
  }

  @Override
  public String getContentAsString(@Nullable Charset charset) throws IOException {
    return new String(content, charset == null ? StandardCharsets.UTF_8 : charset);
  }

  @Override
  public void cleanup() throws IOException { }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(this.content);
  }

  @Override
  public long transferTo(File dest) throws IOException, IllegalStateException {
    FileCopyUtils.copy(this.content, dest);
    return getContentLength();
  }

  public long transferTo(Path dest) throws IOException, IllegalStateException {
    try (var channel = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
      return transferTo(channel, 0, getContentLength());
    }
  }

  @Override
  public long transferTo(FileChannel dest, long position) throws IOException {
    return dest.transferFrom(readableChannel(), position, getContentLength());
  }

  public long transferTo(FileChannel dest, long position, long count) throws IOException {
    return dest.transferFrom(readableChannel(), position, count);
  }

}

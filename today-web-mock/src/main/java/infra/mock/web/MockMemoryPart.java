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

package infra.mock.web;

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

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.FileCopyUtils;
import infra.web.multipart.Part;

/**
 * Mock implementation of {@link Part}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HttpMockRequestImpl#addPart
 * @see MockMemoryFilePart
 * @since 4.0
 */
public class MockMemoryPart implements Part {

  private final String name;

  @Nullable
  private final String filename;

  private final byte[] content;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  /**
   * Constructor for a part with byte[] content only.
   *
   * @see #getHeaders()
   */
  public MockMemoryPart(String name, byte @Nullable [] content) {
    this(name, null, content);
  }

  /**
   * Constructor for a part with a filename and byte[] content.
   *
   * @see #getHeaders()
   */
  public MockMemoryPart(String name, @Nullable String filename, byte @Nullable [] content) {
    Assert.hasLength(name, "'name' must not be empty");
    this.name = name;
    this.filename = filename;
    this.content = (content != null ? content : Constant.EMPTY_BYTES);
    this.headers.setContentDispositionFormData(name, filename);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  @Nullable
  public String getOriginalFilename() {
    return this.filename;
  }

  @Override
  public void transferTo(File dest) throws IllegalStateException, IOException {
    FileCopyUtils.copy(content, dest);
  }

  @Override
  public @Nullable MediaType getContentType() {
    return headers.getContentType();
  }

  @Override
  public long getContentLength() {
    return this.content.length;
  }

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    return content;
  }

  @Override
  public String getContentAsString() throws IOException {
    return getContentAsString(StandardCharsets.UTF_8);
  }

  @Override
  public String getContentAsString(@Nullable Charset charset) throws IOException {
    return new String(content, charset == null ? StandardCharsets.UTF_8 : charset);
  }

  @Override
  public boolean isInMemory() {
    return true;
  }

  @Override
  public boolean isEmpty() {
    return content.length == 0;
  }

  @Override
  public boolean isFormField() {
    return filename == null;
  }

  @Override
  public boolean isFile() {
    return !isFormField();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(this.content);
  }

  public long transferTo(Path dest) throws IOException, IllegalStateException {
    try (var channel = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
      return transferTo(channel, 0, getContentLength());
    }
  }

  public long transferTo(FileChannel out, long position, long count) throws IOException {
    return out.transferFrom(readableChannel(), position, count);
  }

  @Override
  public void cleanup() throws IOException {
  }

  /**
   * Return the {@link HttpHeaders} backing header related accessor methods,
   * allowing for populating selected header entries.
   */
  @Override
  public final HttpHeaders getHeaders() {
    return this.headers;
  }

}

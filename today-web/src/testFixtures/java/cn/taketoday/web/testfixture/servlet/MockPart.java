/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.testfixture.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.http.Part;

/**
 * Mock implementation of {@code jakarta.servlet.http.Part}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see MockHttpServletRequest#addPart
 * @see MockMultipartFile
 * @since 4.0
 */
public class MockPart implements Part {

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
  public MockPart(String name, @Nullable byte[] content) {
    this(name, null, content);
  }

  /**
   * Constructor for a part with a filename and byte[] content.
   *
   * @see #getHeaders()
   */
  public MockPart(String name, @Nullable String filename, @Nullable byte[] content) {
    Assert.hasLength(name, "'name' must not be empty");
    this.name = name;
    this.filename = filename;
    this.content = (content != null ? content : new byte[0]);
    this.headers.setContentDispositionFormData(name, filename);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  @Nullable
  public String getSubmittedFileName() {
    return this.filename;
  }

  @Override
  @Nullable
  public String getContentType() {
    MediaType contentType = this.headers.getContentType();
    return (contentType != null ? contentType.toString() : null);
  }

  @Override
  public long getSize() {
    return this.content.length;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(this.content);
  }

  @Override
  public void write(String fileName) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public String getHeader(String name) {
    return this.headers.getFirst(name);
  }

  @Override
  public Collection<String> getHeaders(String name) {
    Collection<String> headerValues = this.headers.get(name);
    return (headerValues != null ? headerValues : Collections.emptyList());
  }

  @Override
  public Collection<String> getHeaderNames() {
    return this.headers.keySet();
  }

  /**
   * Return the {@link HttpHeaders} backing header related accessor methods,
   * allowing for populating selected header entries.
   */
  public final HttpHeaders getHeaders() {
    return this.headers;
  }

}

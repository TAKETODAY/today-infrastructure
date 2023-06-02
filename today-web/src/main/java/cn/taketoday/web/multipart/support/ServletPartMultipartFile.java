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

package cn.taketoday.web.multipart.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.web.multipart.MultipartFile;
import jakarta.servlet.http.Part;

/**
 * MultipartFile adapter, wrapping a Servlet Part object.
 *
 * @author TODAY
 * @since 2018-06-28 22:40:32
 */
final class ServletPartMultipartFile extends AbstractMultipartFile implements MultipartFile {

  private final Part part;
  private final String filename;

  public ServletPartMultipartFile(Part part) {
    this.part = part;
    this.filename = part.getSubmittedFileName();
  }

  public ServletPartMultipartFile(Part part, String filename) {
    this.part = part;
    this.filename = filename;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return part.getInputStream();
  }

  @Override
  public String getContentType() {
    return part.getContentType();
  }

  @Override
  public long getSize() {
    return part.getSize();
  }

  /**
   * Gets the name of this part
   *
   * @return The name of this part as a <tt>String</tt>
   */
  @Override
  public String getName() {
    return part.getName();
  }

  @Override
  protected DefaultHttpHeaders createHttpHeaders() {
    return ServletPartFormData.createHeaders(part);
  }

  /**
   * Return the original filename in the client's filesystem.
   */
  @Override
  public String getOriginalFilename() {
    return this.filename;
  }

  @Override
  protected void saveInternal(File dest) throws IOException {
    part.write(dest.getPath());
    if (dest.isAbsolute() && !dest.exists()) {
      // Servlet Part.write is not guaranteed to support absolute file paths:
      // may translate the given path to a relative location within a temp dir
      // (e.g. on Jetty whereas Tomcat and Undertow detect absolute paths).
      // At least we offloaded the file from memory storage; it'll get deleted
      // from the temp dir eventually in any case. And for our user's purposes,
      // we can manually copy it to the requested location as a fallback.
      FileCopyUtils.copy(part.getInputStream(), Files.newOutputStream(dest.toPath()));
    }
  }

  @Override
  public void transferTo(Path dest) throws IOException, IllegalStateException {
    FileCopyUtils.copy(part.getInputStream(), Files.newOutputStream(dest));
  }

  @Override
  public boolean isEmpty() {
    return part.getSize() == 0;
  }

  @Override
  protected byte[] doGetBytes() throws IOException {
    return FileCopyUtils.copyToByteArray(part.getInputStream());
  }

  @Override
  public int hashCode() {
    return Objects.hash(part);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj //
            || (obj instanceof ServletPartMultipartFile && Objects.equals(part, ((ServletPartMultipartFile) obj).part));
  }

  @Override
  public Part getOriginalResource() {
    return part;
  }

  @Override
  public void delete() throws IOException {
    part.delete();
  }

}

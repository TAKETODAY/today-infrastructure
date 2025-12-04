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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.FileCopyUtils;
import infra.web.multipart.Part;

/**
 * MultipartFile adapter, wrapping a Servlet Part object.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-06-28 22:40:32
 */
final class MockMultipartFile implements Part {

  private final infra.mock.api.http.Part part;

  private final String filename;

  public MockMultipartFile(infra.mock.api.http.Part part, @Nullable String filename) {
    this.part = part;
    this.filename = filename == null ? part.getSubmittedFileName() : filename;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return part.getInputStream();
  }

  @Override
  public MediaType getContentType() {
    return MediaType.parseMediaType(part.getContentType());
  }

  @Override
  public long getContentLength() {
    return part.getSize();
  }

  @Override
  public String getContentAsString(Charset charset) throws IOException {
    return new String(getContentAsByteArray(), charset);
  }

  @Override
  public boolean isInMemory() {
    return true;
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
  public HttpHeaders getHeaders() {
    return createHeaders(part);
  }

  static DefaultHttpHeaders createHeaders(infra.mock.api.http.Part part) {
    DefaultHttpHeaders headers = HttpHeaders.forWritable();
    for (String headerName : part.getHeaderNames()) {
      headers.addAll(headerName, part.getHeaders(headerName));
    }

    if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
      headers.setOrRemove(HttpHeaders.CONTENT_TYPE, part.getContentType());
    }
    return headers;
  }

  /**
   * Return the original filename in the client's filesystem.
   */
  @Override
  public String getOriginalFilename() {
    return this.filename;
  }

  @Override
  public void transferTo(File dest) throws IOException {
    // fix #3 Upload file not found exception
    File parentFile = dest.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
    /*
     * The uploaded file is being stored on disk
     * in a temporary location so move it to the
     * desired file.
     */
    if (dest.exists()) {
      Files.delete(dest.toPath());
    }
    saveInternal(dest);
  }

  /**
   * Saves the internal representation of this multipart file to the specified destination.
   * This method is intended to be implemented by subclasses to provide specific
   * behavior for saving the file data to a given location. It may involve writing
   * cached bytes or transferring data from a temporary storage to the target file.
   *
   * <p>This method is typically invoked by higher-level operations such as
   * {@link #transferTo(File)} to handle the actual file-saving logic.
   *
   * @param dest the target file where the internal data should be saved. Must not be null.
   * @throws IOException if an I/O error occurs during the save operation, such as issues
   * with writing to the file or accessing the internal data.
   */
  void saveInternal(File dest) throws IOException {
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
  public long transferTo(Path dest) throws IOException, IllegalStateException {
    return FileCopyUtils.copy(part.getInputStream(), Files.newOutputStream(dest));
  }

  @Override
  public long transferTo(OutputStream out) throws IOException {
    return part.getInputStream().transferTo(out);
  }

  @Override
  public void acceptWithException(OutputStream out) throws Exception {
    part.getInputStream().transferTo(out);
  }

  @Override
  public boolean isEmpty() {
    return part.getSize() == 0;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    return FileCopyUtils.copyToByteArray(part.getInputStream());
  }

  @Override
  public String getContentAsString() throws IOException {
    return new String(getContentAsByteArray(), StandardCharsets.UTF_8);
  }

  @Override
  public boolean isFormField() {
    return false;
  }

  @Override
  public void cleanup() throws IOException {
    part.delete();
  }

  @Override
  public int hashCode() {
    return Objects.hash(part);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
            || (obj instanceof MockMultipartFile && Objects.equals(part, ((MockMultipartFile) obj).part));
  }

}

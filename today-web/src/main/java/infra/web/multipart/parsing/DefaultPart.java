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

import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.util.Constants;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.web.multipart.Part;
import infra.web.multipart.parsing.DeferrableStream.State;

/**
 * The default implementation of the {@link infra.web.multipart.Part} interface.
 *
 * <p>After retrieving an instance of this class from a {@link DefaultMultipartParser} instance
 * you may either request all contents of file at once using {@link #getContentAsByteArray()} or request an
 * {@link InputStream InputStream} with {@link #getInputStream()} and process the file without attempting to load it into memory, which may come handy
 * with large files.</p>
 *
 * <p><em>State model</em>: Instances of {@link DefaultPart} are subject to a carefully designed state model.
 * Depending on the so-called {@link DefaultMultipartParser#getThreshold() threshold}, either of the three models are possible:</p>
 * <ol>
 *   <li><em>threshold = 0</em>
 *     Uploaded data is never kept in memory. (Same as threshold=-1.) However, the temporary file is
 *     only created, if data was uploaded. Or, in other words: The uploaded file will never be
 *     empty.
 *     {@link #isInMemory()} will return true, if no data was uploaded, otherwise it will be false.
 *     In the former case {@link #getPath()} will return null, but in the latter case it returns
 *     the path of an existing, non-empty file.</li>
 *   <li><em>threshold &gt; 0</em>
 *     Uploaded data will be kept in memory, if the size is below the threshold. If the size
 *     is equal to, or above the threshold, then a temporary file has been created, and all
 *     uploaded data has been transferred to that file.
 *     {@link #isInMemory()} returns true, if the size of the uploaded data is below the threshold.
 *     If so, {@link #getPath()} returns null. Otherwise, {@link #isInMemory()} returns false,
 *     and {@link #getPath()} returns the path of an existing, temporary file. The size
 *     of the temporary file is equal to, or above the threshold.</li>
 * </ol>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class DefaultPart implements Part {

  /**
   * The name of the form field as provided by the browser.
   */
  private final String fieldName;

  /**
   * Whether or not this item is a simple form field.
   */
  private final boolean isFormField;

  /**
   * The file items headers.
   */
  private final HttpHeaders headers;

  private final DefaultMultipartParser parser;

  /**
   * The content type passed by the browser, or {@code null} if not defined.
   */
  private final @Nullable String contentType;

  /**
   * The original file name in the user's file system.
   */
  private final @Nullable String filename;

  /**
   * Output stream for this item.
   */
  private @Nullable DeferrableStream deferrableStream;

  /**
   * Constructs a new instance.
   */
  DefaultPart(String fieldName, @Nullable String contentType, HttpHeaders headers,
          boolean isFormField, @Nullable String filename, DefaultMultipartParser parser) {
    this.fieldName = fieldName;
    this.contentType = contentType;
    this.isFormField = isFormField;
    this.filename = filename;
    this.headers = headers;
    this.parser = parser;
  }

  /**
   * Deletes the underlying storage for a file item, including deleting any associated temporary disk file. This method can be used to ensure that this is
   * done at an earlier time, thus preserving system resources.
   *
   * @throws IOException if an error occurs.
   */
  @Override
  public void cleanup() throws IOException {
    if (deferrableStream != null) {
      final Path path = deferrableStream.getPath();
      if (path != null) {
        Files.deleteIfExists(path);
      }
    }
  }

  /**
   * Gets the contents of the file as an array of bytes. If the contents of the file were not yet cached in memory, they will be loaded from the disk storage
   * and cached.
   *
   * @return The contents of the file as an array of bytes or {@code null} if the data cannot be read.
   * @throws IOException if an I/O error occurs.
   * @throws OutOfMemoryError See {@link Files#readAllBytes(Path)}: If an array of the required size cannot be allocated, for example the file is larger
   * than {@code 2GB}. If so, you should use {@link #getInputStream()}.
   * @see #getInputStream()
   */
  @Override
  public byte[] getContentAsByteArray() throws IOException {
    if (deferrableStream != null) {
      final byte[] bytes = deferrableStream.getBytes();
      if (bytes != null) {
        return bytes;
      }
      final Path path = deferrableStream.getPath();
      if (path != null && deferrableStream.getState() == State.closed) {
        return Files.readAllBytes(path);
      }
    }
    return Constants.EMPTY_BYTE_ARRAY;
  }

  /**
   * Gets the content charset passed by the agent or {@code null} if not defined.
   *
   * @return The content charset passed by the agent or {@code null} if not defined.
   */
  public Charset getCharset() {
    final var parser = new ParameterParser();
    parser.setLowerCaseNames(true);
    // Parameter parser can handle null input
    final var params = parser.parse(getContentType(), ';');
    return Charsets.toCharset(params.get("charset"), this.parser.getDefaultCharset());
  }

  /**
   * Gets the content type passed by the agent or {@code null} if not defined.
   *
   * @return The content type passed by the agent or {@code null} if not defined.
   */
  @Override
  public @Nullable String getContentType() {
    return contentType;
  }

  /**
   * Gets the name of the field in the multipart form corresponding to this file item.
   *
   * @return The name of the form field.
   */
  @Override
  public String getName() {
    return fieldName;
  }

  /**
   * Gets the file item headers.
   *
   * @return The file items headers.
   */
  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }

  /**
   * Gets an {@link InputStream InputStream} that can be used to retrieve the contents of the file.
   *
   * @return An {@link InputStream InputStream} that can be used to retrieve the contents of the file.
   * @throws IOException if an error occurs.
   */
  @Override
  public InputStream getInputStream() throws IOException {
    if (deferrableStream != null && deferrableStream.getState() == State.closed) {
      return deferrableStream.getInputStream();
    }
    throw new IllegalStateException("The file item has not been fully read.");
  }

  /**
   * Gets the original file name in the client's file system.
   *
   * @return The original file name in the client's file system.
   * @throws InvalidPathException The file name contains a NUL character, which might be an indicator of a security attack. If you intend to use the file name
   * anyways, catch the exception and use {@link InvalidPathException#getInput()}.
   */
  @Override
  public @Nullable String getOriginalFilename() {
    return checkFileName(filename);
  }

  /**
   * Gets an {@link OutputStream OutputStream} that can be used for storing the contents of the file.
   *
   * @return An {@link OutputStream OutputStream} that can be used for storing the contents of the file.
   */
  OutputStream getOutputStream() throws IOException {
    if (deferrableStream == null) {
      deferrableStream = new DeferrableStream(parser);
    }
    return deferrableStream;
  }

  /**
   * Gets the {@link Path} for the {@code FileItem}'s data's temporary location on the disk. Note that for {@code FileItem}s that have their data stored in
   * memory, this method will return {@code null}. When handling large files, you can use {@link Files#move(Path, Path, CopyOption...)} to move the file to a
   * new location without copying the data, if the source and destination locations reside within the same logical volume.
   *
   * @return The data file, or {@code null} if the data is stored in memory.
   */
  private @Nullable Path getPath() {
    return deferrableStream == null ? null : deferrableStream.getPath();
  }

  /**
   * Returns the contents of the file as a {@link Reader}, using the specified
   * {@link #getCharset()}. If the contents are not yet available, returns null.
   * This is the case, for example, if the underlying output stream has not yet
   * been closed.
   *
   * @return The contents of the file as a {@link Reader}
   * @throws UnsupportedEncodingException The character set, which is
   * specified in the files "content-type" header, is invalid.
   * @throws IOException An I/O error occurred, while the
   * underlying {@link #getInputStream() input stream} was created.
   */
  @Override
  public Reader getReader() throws IOException, UnsupportedEncodingException {
    final var parser = new ParameterParser();
    parser.setLowerCaseNames(true);
    // Parameter parser can handle null input
    final var params = parser.parse(getContentType(), ';');
    final Charset cs = Charsets.toCharset(params.get("charset"), this.parser.getDefaultCharset());
    return getReader(cs);
  }

  /**
   * Gets the size of the file.
   *
   * @return The size of the file, in bytes.
   */
  @Override
  public long getContentLength() {
    return deferrableStream == null ? 0L : deferrableStream.getSize();
  }

  @Override
  public String getContentAsString() throws IOException {
    return new String(getContentAsByteArray(), getCharset());
  }

  @Override
  public String getContentAsString(@Nullable Charset charset) throws IOException {
    return new String(getContentAsByteArray(), Objects.requireNonNullElse(charset, parser.getDefaultCharset()));
  }

  @Override
  public boolean isFormField() {
    return isFormField;
  }

  @Override
  public boolean isFile() {
    return !isFormField;
  }

  @Override
  public boolean isInMemory() {
    return deferrableStream == null || deferrableStream.isInMemory();
  }

  @Override
  public boolean isEmpty() {
    return getContentLength() == 0L;
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    transferTo(dest.toPath());
  }

  /**
   * Writes an uploaded item to disk.
   * <p>
   * The client code is not concerned with whether or not the item is stored in memory, or on disk in a temporary location. They just want to write the
   * uploaded item to a file.
   * </p>
   * <p>
   * This implementation first attempts to rename the uploaded item to the specified destination file, if the item was originally written to disk. Otherwise,
   * the data will be copied to the specified file.
   * </p>
   * <p>
   * This method is only guaranteed to work <em>once</em>, the first time it is invoked for a particular item. This is because, in the event that the method
   * renames a temporary file, that file will no longer be available to copy or rename again at a later time.
   * </p>
   *
   * @param file The {@code File} into which the uploaded item should be stored.
   * @throws IOException if an error occurs.
   */
  @Override
  public long transferTo(final Path file) throws IOException {
    if (isInMemory()) {
      try (var fout = Files.newOutputStream(file)) {
        fout.write(getContentAsByteArray());
      }
      catch (final IOException e) {
        throw new IOException("Unexpected output data", e);
      }
    }
    else {
      final Path outputFile = getPath();
      if (outputFile == null) {
        // For whatever reason we cannot write the file to disk.
        throw new FileUploadException("Cannot write uploaded file to disk.");
      }
      // The uploaded file is being stored on disk in a temporary location so move it to the desired file.
      Files.move(outputFile, file, StandardCopyOption.REPLACE_EXISTING);
    }
    return getContentLength();
  }

  @Override
  public long transferTo(FileChannel out, long position, long count) throws IOException {
    if (isInMemory()) {
      return out.write(ByteBuffer.wrap(getContentAsByteArray(), 0, Math.toIntExact(count)), position);
    }

    Path path = getPath();
    Assert.state(path != null, "The temp file not found.");
    try (var src = FileChannel.open(path)) {
      return out.transferFrom(src, position, count);
    }
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object.
   */
  @Override
  public String toString() {
    return String.format("name=%s, StoreLocation=%s, size=%s bytes, isFormField=%s, FieldName=%s",
            getName(), getPath(), this.getContentLength(), isFormField(), this.getName());
  }

  /**
   * Tests if the file name is valid. For example, if it contains a NUL characters, it's invalid. If the file name is valid, it will be returned without any
   * modifications. Otherwise, throw an {@link InvalidPathException}.
   *
   * @param fileName The file name to check
   * @return Unmodified file name, if valid.
   * @throws InvalidPathException The file name is invalid.
   */
  static @Nullable String checkFileName(final @Nullable String fileName) {
    if (fileName != null) {
      // Specific NUL check to build a better exception message.
      final var indexOf0 = fileName.indexOf(0);
      if (indexOf0 != -1) {
        final var sb = new StringBuilder();
        for (var i = 0; i < fileName.length(); i++) {
          final var c = fileName.charAt(i);
          if (c == 0) {
            sb.append("\\0");
          }
          else {
            sb.append(c);
          }
        }
        throw new InvalidPathException(fileName, sb.toString(), indexOf0);
      }
      // Throws InvalidPathException on invalid file names
      Paths.get(fileName);
    }
    return fileName;
  }

}

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

package infra.web.multipart;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import infra.core.io.InputStreamSource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.MediaType;

/**
 * Representation for a part in a "multipart/*" request.
 *
 * <p>The origin of a multipart request may be a browser form in which case each
 * part is either a {@code FormField} or a {@code MultipartFile}.
 *
 * <p>Multipart requests may also be used outside a browser for data of any
 * content type (e.g. JSON, PDF, etc).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578 (multipart/form-data)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183 (Content-Disposition)</a>
 * @see <a href="https://www.w3.org/TR/html5/forms.html#multipart-form-data">HTML5 (multipart forms)</a>
 * @since 4.0 2022/4/28 22:04
 */
public interface Part extends InputStreamSource, HttpInputMessage {

  /**
   * Gets the name of this part.
   *
   * @return The name of this part as a {@code String}
   */
  String getName();

  /**
   * Return the headers for the specified part of the multipart request.
   * <p>If the underlying implementation supports access to part headers,
   * then all headers are returned. Otherwise, e.g. for a file upload, the
   * returned headers may expose a 'Content-Type' if available.
   */
  @Override
  HttpHeaders getHeaders();

  /**
   * Returns the value of the specified mime header as a <code>String</code>. If the Part did not include a header of the
   * specified name, this method returns <code>null</code>. If there are multiple headers with the same name, this method
   * returns the first header in the part. The header name is case insensitive. You can use this method with any request
   * header.
   *
   * @param name a <code>String</code> specifying the header name
   * @return a <code>String</code> containing the value of the requested header, or <code>null</code> if the part does not
   * have a header of that name
   * @since 5.0
   */
  default @Nullable String getHeader(String name) {
    return getHeaders().getFirst(name);
  }

  /**
   * Gets the values of the Part header with the given name.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>Part</code>.
   *
   * <p>
   * Part header names are case-insensitive.
   *
   * @param name the header name whose values to return
   * @return a (possibly empty) <code>Collection</code> of the values of the header with the given name
   * @since 5.0
   */
  default Collection<String> getHeaders(String name) {
    Collection<String> headerValues = getHeaders().get(name);
    return headerValues != null ? headerValues : Collections.emptyList();
  }

  /**
   * Get the header names provided for this part.
   *
   * @return a Collection of all the header names provided for this part.
   * @since 5.0
   */
  default Collection<String> getHeaderNames() {
    return getHeaders().keySet();
  }

  /**
   * Return the body of the message as an input stream.
   *
   * @return the input stream body (never {@code null})
   * @throws IOException in case of I/O errors
   * @since 5.0
   */
  @Override
  default InputStream getBody() throws IOException {
    return getInputStream();
  }

  /**
   * Return the content type of the part.
   *
   * @return the content type, or {@code null} if not defined
   * (or no part has been chosen in the multipart form)
   */
  @Nullable
  MediaType getContentType();

  /**
   * Get the content type of this part.
   *
   * @return the content type of this part, or {@code null} if not defined
   * @since 5.0
   */
  @Nullable
  default String getContentTypeString() {
    MediaType contentType = getContentType();
    return contentType != null ? contentType.toString() : null;
  }

  /**
   * Determine the content length for this Part.
   *
   * @since 5.0
   */
  long getContentLength();

  /**
   * Returns the contents of this part as an array of bytes.
   * <p>
   * Note: this method will allocate a lot of memory,
   * if the data is currently stored on the file system.
   *
   * @return the contents of this part as an array of bytes.
   * @throws OutOfMemoryError See {@link Files#readAllBytes(Path)}:
   * If an array of the required size cannot be allocated, for example the file is larger
   * than {@code 2GB}. If so, you should use {@link #getInputStream()}.
   */
  byte[] getContentAsByteArray() throws IOException;

  /**
   * Returns the contents of this part as a string, using the UTF-8.
   *
   * @return the contents of this resource as a {@code String}
   * @throws OutOfMemoryError See {@link Files#readAllBytes(Path)}:
   * If a string of the required size cannot be allocated, for example the
   * file is larger than {@code 2GB}. If so, you should use {@link #getReader()}.
   * @since 5.0
   */
  String getContentAsString() throws IOException;

  /**
   * Returns the contents of this part as a string, using the specified
   * charset.
   *
   * @param charset the charset to use for decoding
   * @return the contents of this resource as a {@code String}
   * @throws OutOfMemoryError See {@link Files#readAllBytes(Path)}:
   * If a string of the required size cannot be allocated, for example the
   * file is larger than {@code 2GB}. If so, you should use {@link #getReader()}.
   * @throws IOException if an I/O error occurs
   * @since 5.0
   */
  String getContentAsString(@Nullable Charset charset) throws IOException;

  /**
   * Tests a hint as to whether or not the part contents will be read from memory.
   *
   * @return {@code true} if the part contents will be read from memory; {@code false} otherwise.
   * @since 5.0
   */
  boolean isInMemory();

  /**
   * Return whether the part content is empty, that is, either no file has
   * been chosen in the multipart form or the chosen file has no content.
   *
   * @since 5.0
   */
  boolean isEmpty();

  /**
   * Determines whether or not a {@code Multipart} instance represents
   * a simple form field.
   *
   * @return {@code true} if the instance represents a simple form
   * field; {@code false} if it represents an uploaded file.
   */
  boolean isFormField();

  /**
   * Determine whether this part represents a file part.
   *
   * @since 5.0
   */
  boolean isFile();

  /**
   * Return a Resource representation of this Part. This can be used
   * as input to the {@code RestTemplate} or the {@code WebClient} to expose
   * content length and the filename along with the InputStream.
   *
   * @return this Part adapted to the Resource contract
   * @see #isFile
   * @since 5.0
   */
  default Resource getResource() {
    return new PartResource(this);
  }

  /**
   * Gets the original file name in the client's file system, as provided by
   * the browser (or other client software). In most cases, this will be the base
   * file name, without path information. However, some clients, such as the
   * Opera browser, do include path information.
   *
   * @return the original filename, or the empty String if no file has been chosen
   * in the multipart form, or {@code null} if not defined or not available
   * @throws InvalidPathException The file name contains a NUL character, which might
   * be an indicator of a security attack. If you intend to use the file name anyways,
   * catch the exception and use {@link InvalidPathException#getInput()}.
   * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
   * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
   * @see #isFile
   * @since 5.0
   */
  @Nullable
  String getOriginalFilename();

  /**
   * Transfer the received file to the given destination file.
   * <p>This may either move the file in the filesystem, copy the file in the
   * filesystem, or save memory-held contents to the destination file. If the
   * destination file already exists, it will be deleted first.
   * <p>If the target file has been moved in the filesystem, this operation
   * cannot be invoked again afterwards. Therefore, call this method just once
   * in order to work with any storage mechanism.
   * <p><b>NOTE:</b> Depending on the underlying provider, temporary storage
   * may be container-dependent, including the base directory for relative
   * destinations specified here (e.g. with Web multipart handling).
   * For absolute destinations, the target file may get renamed/moved from its
   * temporary location or newly copied, even if a temporary copy already exists.
   *
   * @param dest the destination file (typically absolute)
   * @throws IOException in case of reading or writing errors
   * @throws IllegalStateException if the file has already been moved
   * in the filesystem and is not available anymore for another transfer
   */
  void transferTo(File dest) throws IOException, IllegalStateException;

  /**
   * Transfer the received file to the given destination file.
   * <p>The default implementation simply copies the file input stream.
   *
   * @throws IllegalArgumentException If the preconditions on the parameters do not hold
   * @throws NonReadableChannelException If the source channel was not opened for reading
   * @throws NonWritableChannelException If this channel was not opened for writing
   * @throws ClosedChannelException If either this channel or the source channel is closed
   * @throws AsynchronousCloseException If another thread closes either channel
   * while the transfer is in progress
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the
   * transfer is in progress, thereby closing both channels and
   * setting the current thread's interrupt status
   * @throws IOException If some other I/ O error occurs
   * @throws IllegalStateException if the file has already been moved
   * in the filesystem and is not available anymore for another transfer
   * @see #getInputStream()
   * @see #transferTo(File)
   * @since 4.0
   */
  long transferTo(Path dest) throws IOException, IllegalStateException;

  /**
   * Transfers this file into this channel's file from the given readable byte
   * channel.
   *
   * @param out FileChannel
   * @param position The position within the file at which the transfer is to begin; must be non-negative
   * @param count The maximum number of bytes to be transferred; must be non-negative
   * @return The number of bytes, possibly zero, that were actually transferred
   * @throws IllegalArgumentException If the preconditions on the parameters do not hold
   * @throws NonReadableChannelException If the source channel was not opened for reading
   * @throws NonWritableChannelException If this channel was not opened for writing
   * @throws ClosedChannelException If either this channel or the source channel is closed
   * @throws AsynchronousCloseException If another thread closes either channel
   * while the transfer is in progress
   * @throws ClosedByInterruptException If another thread interrupts the current thread while the
   * transfer is in progress, thereby closing both channels and
   * setting the current thread's interrupt status
   * @throws IOException If some other I/ O error occurs
   * @since 5.0
   */
  long transferTo(FileChannel out, long position, long count) throws IOException;

  /**
   * Deletes the underlying storage for a file item, including deleting any
   * associated temporary disk file.
   *
   * @throws IOException if an error occurs.
   */
  void cleanup() throws IOException;

}

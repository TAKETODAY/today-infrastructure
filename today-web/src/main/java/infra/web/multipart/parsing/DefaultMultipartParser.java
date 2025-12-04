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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import infra.core.ApplicationTemp;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import infra.util.StreamUtils;
import infra.web.RequestContext;
import infra.web.multipart.MultipartException;
import infra.web.multipart.MultipartParser;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;
import infra.web.util.WebUtils;

/**
 * Default implementation of the {@link MultipartParser}
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class DefaultMultipartParser implements MultipartParser {

  /**
   * The default threshold in bytes above which uploads will be stored on disk.
   */
  public static final int DEFAULT_THRESHOLD = 10_240;

  /**
   * The default length of the buffer used for processing a request.
   */
  static final int DEFAULT_BUF_SIZE = 4096;

  /**
   * The maximum permitted number of fields in a single request.
   */
  private int maxFields = 128;

  /**
   * The maximum permitted size of the headers provided with a single part in bytes.
   */
  private int maxHeaderSize = MultipartInput.DEFAULT_PART_HEADER_SIZE_MAX;

  /**
   * The threshold above which field will be stored on disk.
   */
  private long threshold = DEFAULT_THRESHOLD;

  /**
   * The directory in which uploaded files will be stored, if stored on disk.
   */
  private Path tempRepository = ApplicationTemp.createDirectory("upload");

  /**
   * Default content Charset to be used when no explicit Charset parameter is provided by the sender.
   */
  private Charset defaultCharset = StandardCharsets.UTF_8;

  /**
   * The progress listener.
   */
  private ProgressListener progressListener = ProgressListener.NOP;

  private boolean deleteOnExit;

  /**
   * The length of the buffer used for processing the request.
   */
  private int parsingBufferSize = DEFAULT_BUF_SIZE;

  /**
   * Constructs a new instance for subclasses.
   */
  public DefaultMultipartParser() {
  }

  /**
   * Sets the maximum number of fields allowed per request.
   *
   * @param maxFields the maximum number of fields allowed per request.
   */
  public void setMaxFields(final int maxFields) {
    Assert.isTrue(maxFields > 0, "Maximum number of fields must be greater than 0");
    this.maxFields = maxFields;
  }

  /**
   * Sets the per part size limit for headers.
   *
   * @param maxHeaderSize The maximum size of the headers in bytes.
   */
  public void setMaxHeaderSize(final int maxHeaderSize) {
    this.maxHeaderSize = maxHeaderSize;
  }

  /**
   * Sets the progress listener.
   *
   * @param listener The progress listener, if any. Defaults to null.
   */
  public void setProgressListener(final @Nullable ProgressListener listener) {
    this.progressListener = listener != null ? listener : ProgressListener.NOP;
  }

  /**
   * Sets the threshold. The uploaded data is typically kept in memory, until
   * a certain number of bytes (the threshold) is reached. At this point, the
   * incoming data is transferred to a temporary file, and the in-memory data
   * is removed.
   *
   * @param threshold The threshold, which is being used.
   */
  public void setThreshold(long threshold) {
    Assert.isTrue(threshold >= 0, "Threshold must be greater than or equal to 0");
    this.threshold = threshold;
  }

  /**
   * Sets the directory in which uploaded fields will be stored, if stored on disk.
   *
   * @param tempRepository The directory path where temporary fields should be stored.
   */
  public void setTempRepository(Path tempRepository) {
    this.tempRepository = tempRepository;
  }

  /**
   * Sets the default character encoding to be used when no explicit
   * character encoding is provided by the sender.
   *
   * @param defaultCharset The default character encoding to be used
   */
  public void setDefaultCharset(Charset defaultCharset) {
    Assert.notNull(defaultCharset, "Default charset is required");
    this.defaultCharset = defaultCharset;
  }

  /**
   * Sets whether temporary files should be deleted on JVM exit.
   *
   * @param deleteOnExit Whether to delete temporary files on JVM exit
   */
  public void setDeleteOnExit(boolean deleteOnExit) {
    this.deleteOnExit = deleteOnExit;
  }

  /**
   * Sets the size of the buffer used for parsing multipart requests.
   *
   * @param parsingBufferSize The size of the buffer in bytes
   * @see MultipartInput
   */
  public void setParsingBufferSize(int parsingBufferSize) {
    Assert.isTrue(parsingBufferSize > 0, "parsingBufferSize must be greater than 0");
    this.parsingBufferSize = parsingBufferSize;
  }

  /**
   * Gets the size threshold beyond which files are written directly to disk.
   * The default value is {@value #DEFAULT_THRESHOLD} bytes.
   *
   * @return The size threshold in bytes.
   */
  public long getThreshold() {
    return threshold;
  }

  /**
   * Determines whether temporary files should be deleted on JVM exit.
   *
   * @return {@code true} if temporary files should be deleted on JVM exit,
   * {@code false} otherwise
   */
  public boolean isDeleteOnExit() {
    return deleteOnExit;
  }

  /**
   * Gets the directory in which uploaded files will be stored, if stored on disk.
   *
   * @return The directory path where temporary files are stored.
   */
  public Path getTempRepository() {
    return tempRepository;
  }

  /**
   * Gets the default character encoding to be used when no explicit
   * character encoding is provided by the sender.
   *
   * @return The default character encoding
   */
  public Charset getDefaultCharset() {
    return defaultCharset;
  }

  /**
   * Gets the size of the buffer used for parsing multipart requests.
   *
   * @return The size of the buffer in bytes
   */
  public int getParsingBufferSize() {
    return parsingBufferSize;
  }

  /**
   * Gets the maximum number of files allowed in a single request.
   *
   * @return The maximum number of files allowed in a single request.
   */
  public int getMaxFields() {
    return maxFields;
  }

  /**
   * Gets the per part size limit for headers.
   *
   * @return The maximum size of the headers for a single part in bytes.
   */
  public int getMaxHeaderSize() {
    return maxHeaderSize;
  }

  /**
   * Gets the progress listener.
   *
   * @return The progress listener, if any, or null.
   */
  public @Nullable ProgressListener getProgressListener() {
    return progressListener;
  }

  @Override
  public MultipartRequest parse(RequestContext request) throws infra.web.multipart.MultipartException {
    return new DefaultMultipartRequest(this, request);
  }

  /**
   * Parses an <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant {@code multipart/form-data} stream.
   *
   * @param context The context for the request to be parsed.
   * @return A Map of {@code Part} instances parsed from the request, in the order that they were transmitted.
   * @throws MultipartException if there are problems reading/parsing the request or storing files.
   */
  public MultiValueMap<String, Part> parseRequest(final RequestContext context) throws MultipartException {
    MultiValueMap<String, Part> parts = MultiValueMap.forLinkedHashMap();
    boolean successful = false;
    try {
      final byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
      var itemIterator = new FieldItemInputIterator(this, context);
      while (itemIterator.hasNext()) {
        FieldItemInput field = itemIterator.next();
        final int size = parts.size();
        if (size == maxFields) {
          // The next item will exceed the limit.
          throw new MultipartFieldCountLimitException("Request '%s' failed: Maximum file count %,d exceeded."
                  .formatted(context.getContentType(), maxFields), maxFields, size);
        }

        DefaultPart fieldItem = new DefaultPart(field.getName(), field.getContentType(),
                field.getHeaders(), field.isFormField(), field.getFilename(), this);

        parts.add(fieldItem.getName(), fieldItem);
        try (var inputStream = field.getInputStream();
                var outputStream = fieldItem.getOutputStream()) {
          StreamUtils.copy(inputStream, outputStream, buffer);
        }
        catch (IOException e) {
          throw new MultipartException(String.format("Request '%s' failed: %s", context.getContentType(), e.getMessage()), e);
        }
      }
      successful = true;
      return parts;
    }
    catch (final IOException e) {
      throw new MultipartException(e.getMessage(), e);
    }
    finally {
      if (!successful) {
        WebUtils.cleanupMultipartRequest(parts);
      }
    }
  }

}

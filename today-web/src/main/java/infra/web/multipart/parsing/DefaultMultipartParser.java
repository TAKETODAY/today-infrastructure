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
import java.util.List;
import java.util.Locale;

import infra.core.ApplicationTemp;
import infra.http.HttpHeaders;
import infra.http.MediaType;
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
 * High level API for processing file uploads.
 * <p>
 * This class handles multiple files per single HTML widget, sent using {@code multipart/mixed} encoding type, as specified by
 * <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>.
 * Use {@link #parseRequest(RequestContext)} to acquire a list of {@link DefaultPart}s associated with
 * a given HTML widget.
 * </p>
 * <p>
 * How the data for individual parts is stored is determined by the factory used to create them; a given part may be in memory, on disk, or somewhere else.
 * </p>
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
   * Default content charset to be used when no explicit charset parameter is provided by the sender. Media subtypes of the "text" type are defined to have a
   * default charset value of "ISO-8859-1" when received via HTTP.
   */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

  /**
   * Content-disposition value for form data.
   */
  private static final String FORM_DATA = "form-data";

  /**
   * Name parameter key.
   */
  private static final String NAME_KEY = "name";

  /**
   * File name parameter key.
   */
  private static final String FILENAME_KEY = "filename";

  /**
   * The maximum size permitted for the complete request, as opposed to {@link #maxFileSize}. A value of -1 indicates no maximum.
   */
  private long maxSize = -1;

  /**
   * The maximum size permitted for a single uploaded file, as opposed to {@link #maxSize}. A value of -1 indicates no maximum.
   */
  private long maxFileSize = -1;

  /**
   * The maximum permitted number of files that may be uploaded in a single request. A value of -1 indicates no maximum.
   */
  private long maxFileCount = -1;

  /**
   * The maximum permitted size of the headers provided with a single part in bytes.
   */
  private int maxPartHeaderSize = MultipartInput.DEFAULT_PART_HEADER_SIZE_MAX;

  /**
   * The threshold above which field will be stored on disk.
   */
  private long threshold = DEFAULT_THRESHOLD;

  /**
   * The directory in which uploaded files will be stored, if stored on disk.
   */
  private Path tempRepository = ApplicationTemp.createDirectory("upload");

  /**
   * The content encoding to use when reading part headers.
   */
  private @Nullable Charset headerCharset;

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
   * Constructs a new instance for subclasses.
   */
  public DefaultMultipartParser() {
  }

  /**
   * Gets the boundary from the {@code Content-type} header.
   *
   * @param contentType The value of the content type header from which to extract the boundary value.
   * @return The boundary, as a byte array.
   */
  public byte @Nullable [] getBoundary(final String contentType) {
    final var parser = new ParameterParser();
    parser.setLowerCaseNames(true);
    // Parameter parser can handle null input
    final var params = parser.parse(contentType, new char[] { ';', ',' });
    final var boundaryStr = params.get(HttpHeaders.BOUNDARY);
    return boundaryStr != null ? boundaryStr.getBytes(StandardCharsets.ISO_8859_1) : null;
  }

  /**
   * Gets the field name from the {@code Content-disposition} header.
   *
   * @param headers A {@code Map} containing the HTTP request headers.
   * @return The field name for the current {@code encapsulation}.
   */
  public @Nullable String getFieldName(final HttpHeaders headers) {
    return getFieldName(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION));
  }

  /**
   * Gets the field name, which is given by the content-disposition header.
   *
   * @param contentDisposition The content-dispositions header value.
   * @return The field name.
   */
  private @Nullable String getFieldName(final @Nullable String contentDisposition) {
    String fieldName = null;
    if (contentDisposition != null && contentDisposition.toLowerCase(Locale.ROOT).startsWith(FORM_DATA)) {
      final var parser = new ParameterParser();
      parser.setLowerCaseNames(true);
      // Parameter parser can handle null input
      final var params = parser.parse(contentDisposition, ';');
      fieldName = params.get(NAME_KEY);
      if (fieldName != null) {
        fieldName = fieldName.trim();
      }
    }
    return fieldName;
  }

  /**
   * Gets the file name from the {@code Content-disposition} header.
   *
   * @param headers The HTTP headers object.
   * @return The file name for the current {@code encapsulation}.
   */
  public @Nullable String getFileName(final HttpHeaders headers) {
    return getFileName(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION));
  }

  /**
   * Gets the given content-disposition headers file name.
   *
   * @param contentDisposition The content-disposition headers value.
   * @return The file name
   */
  private @Nullable String getFileName(final @Nullable String contentDisposition) {
    String fileName = null;
    if (contentDisposition != null) {
      final var cdl = contentDisposition.toLowerCase(Locale.ROOT);
      if (cdl.startsWith(FORM_DATA) || cdl.startsWith("attachment")) {
        final var parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handle null input
        final var params = parser.parse(contentDisposition, ';');
        if (params.containsKey(FILENAME_KEY)) {
          fileName = params.get(FILENAME_KEY);
          if (fileName != null) {
            fileName = fileName.trim();
          }
          else {
            // Even if there is no value, the parameter is present,
            // so we return an empty file name rather than no file
            // name.
            fileName = "";
          }
        }
      }
    }
    return fileName;
  }

  /**
   * Gets the character encoding used when reading the headers of an individual part. When not specified, or {@code null}, the request encoding is used. If
   * that is also not specified, or {@code null}, the platform default encoding is used.
   *
   * @return The encoding used to read part headers.
   */
  public @Nullable Charset getHeaderCharset() {
    return headerCharset;
  }

  /**
   * Gets an <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant {@code multipart/form-data} stream.
   *
   * @param requestContext The context for the request to be parsed.
   * @return An iterator to instances of {@code FileItemInput} parsed from the request, in the order that they were transmitted.
   * @throws FileUploadException if there are problems reading/parsing the request or storing files.
   * @throws IOException An I/O error occurred. This may be a network error while communicating with the client or a problem while storing the
   * uploaded content.
   */
  public FileItemInputIterator getItemIterator(final RequestContext requestContext) throws FileUploadException, IOException {
    return new FileItemInputIterator(this, requestContext);
  }

  /**
   * Gets the maximum number of files allowed in a single request.
   *
   * @return The maximum number of files allowed in a single request.
   */
  public long getMaxFileCount() {
    return maxFileCount;
  }

  /**
   * Gets the maximum allowed size of a single form field, as opposed to {@link #getMaxSize()}.
   *
   * @return Maximum size of a single form field.
   * @see #setMaxFileSize(long)
   */
  public long getMaxFileSize() {
    return maxFileSize;
  }

  /**
   * Gets the per part size limit for headers.
   *
   * @return The maximum size of the headers for a single part in bytes.
   */
  public int getMaxPartHeaderSize() {
    return maxPartHeaderSize;
  }

  /**
   * Gets the maximum allowed size of a complete request, as opposed to {@link #getMaxFileSize()}.
   *
   * @return The maximum allowed size, in bytes. The default value of -1 indicates, that there is no limit.
   * @see #setMaxSize(long)
   */
  public long getMaxSize() {
    return maxSize;
  }

  /**
   * Parses the {@code header-part} and returns as key/value pairs.
   * <p>
   * If there are multiple headers of the same names, the name will map to a comma-separated list containing the values.
   * </p>
   *
   * @param headerPart The {@code header-part} of the current {@code encapsulation}.
   * @return A {@code Map} containing the parsed HTTP request headers.
   */
  public HttpHeaders getParsedHeaders(final String headerPart) {
    final var len = headerPart.length();
    final var headers = newItemHeaders();
    var start = 0;
    for (; ; ) {
      var end = parseEndOfLine(headerPart, start);
      if (start == end) {
        break;
      }
      final var header = new StringBuilder(headerPart.substring(start, end));
      start = end + 2;
      while (start < len) {
        var nonWs = start;
        while (nonWs < len) {
          final var c = headerPart.charAt(nonWs);
          if (c != ' ' && c != '\t') {
            break;
          }
          ++nonWs;
        }
        if (nonWs == start) {
          break;
        }
        // Continuation line found
        end = parseEndOfLine(headerPart, nonWs);
        header.append(' ').append(headerPart, nonWs, end);
        start = end + 2;
      }
      parseHeaderLine(headers, header.toString());
    }
    return headers;
  }

  /**
   * Gets the progress listener.
   *
   * @return The progress listener, if any, or null.
   */
  public @Nullable ProgressListener getProgressListener() {
    return progressListener;
  }

  /**
   * Creates a new instance of {@link HttpHeaders}.
   *
   * @return The new instance.
   */
  protected HttpHeaders newItemHeaders() {
    return HttpHeaders.forWritable();
  }

  /**
   * Skips bytes until the end of the current line.
   *
   * @param headerPart The headers, which are being parsed.
   * @param end Index of the last byte, which has yet been processed.
   * @return Index of the \r\n sequence, which indicates end of line.
   */
  private int parseEndOfLine(final String headerPart, final int end) {
    var index = end;
    for (; ; ) {
      final var offset = headerPart.indexOf('\r', index);
      if (offset == -1 || offset + 1 >= headerPart.length()) {
        throw new IllegalStateException("Expected headers to be terminated by an empty line.");
      }
      if (headerPart.charAt(offset + 1) == '\n') {
        return offset;
      }
      index = offset + 1;
    }
  }

  /**
   * Parses the next header line.
   *
   * @param headers String with all headers.
   * @param header Map where to store the current header.
   */
  private void parseHeaderLine(final HttpHeaders headers, final String header) {
    final var colonOffset = header.indexOf(':');
    if (colonOffset == -1) {
      // This header line is malformed, skip it.
      return;
    }
    final var headerName = header.substring(0, colonOffset).trim();
    final var headerValue = header.substring(colonOffset + 1).trim();
    headers.add(headerName, headerValue);
  }

  @Override
  public MultipartRequest parse(RequestContext request) throws MultipartException {
    return new DefaultMultipartRequest(this, request);
  }

  /**
   * Parses an <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant {@code multipart/form-data} stream.
   *
   * @param context The context for the request to be parsed.
   * @return A list of {@code FileItem} instances parsed from the request, in the order that they were transmitted.
   * @throws FileUploadException if there are problems reading/parsing the request or storing files.
   */
  public MultiValueMap<String, Part> parseRequest(final RequestContext context) throws FileUploadException {
    MultiValueMap<String, Part> parts = MultiValueMap.forLinkedHashMap();
    var successful = false;
    try {
      final var buffer = new byte[StreamUtils.BUFFER_SIZE];
      FileItemInputIterator itemIterator = getItemIterator(context);
      while (itemIterator.hasNext()) {
        FieldItemInput field = itemIterator.next();
        final int size = parts.size();
        if (size == maxFileCount) {
          // The next item will exceed the limit.
          throw new FileUploadFileCountLimitException("Request '%s' failed: Maximum file count %,d exceeded."
                  .formatted(MediaType.MULTIPART_FORM_DATA_VALUE, maxFileCount), getMaxFileCount(), size);
        }

        DefaultPart fieldItem = new DefaultPart(field.getName(), field.getContentType(),
                field.getHeaders(), field.isFormField(), field.getFilename(), this);

        parts.add(fieldItem.getName(), fieldItem);
        try (var inputStream = field.getInputStream();
                var outputStream = fieldItem.getOutputStream()) {
          StreamUtils.copy(inputStream, outputStream, buffer);
        }
        catch (final IOException e) {
          throw new FileUploadException(String.format("Request '%s' failed: %s", MediaType.MULTIPART_FORM_DATA_VALUE, e.getMessage()), e);
        }
      }
      successful = true;
      return parts;
    }
    catch (final IOException e) {
      throw new FileUploadException(e.getMessage(), e);
    }
    finally {
      if (!successful) {
        WebUtils.cleanupMultipartRequest(parts);
      }
    }
  }

  /**
   * Specifies the character encoding to be used when reading the headers of individual part. When not specified, or {@code null}, the request encoding is
   * used. If that is also not specified, or {@code null}, the platform default encoding is used.
   *
   * @param headerCharset The encoding used to read part headers.
   */
  public void setHeaderCharset(final @Nullable Charset headerCharset) {
    this.headerCharset = headerCharset;
  }

  /**
   * Sets the maximum number of files allowed per request.
   *
   * @param fileCountMax The new limit. {@code -1} means no limit.
   */
  public void setMaxFileCount(final long fileCountMax) {
    this.maxFileCount = fileCountMax;
  }

  /**
   * Sets the maximum allowed size of a single form field, as opposed to {@link #getMaxSize()}.
   *
   * @param fileSizeMax Maximum size of a single form field.
   * @see #getMaxFileSize()
   */
  public void setMaxFileSize(final long fileSizeMax) {
    this.maxFileSize = fileSizeMax;
  }

  /**
   * Sets the per part size limit for headers.
   *
   * @param partHeaderSizeMax The maximum size of the headers in bytes.
   */
  public void setMaxPartHeaderSize(final int partHeaderSizeMax) {
    this.maxPartHeaderSize = partHeaderSizeMax;
  }

  /**
   * Sets the maximum allowed size of a complete request, as opposed to {@link #setMaxFileSize(long)}.
   *
   * @param sizeMax The maximum allowed size, in bytes. The default value of -1 indicates, that there is no limit.
   * @see #getMaxSize()
   */
  public void setMaxSize(final long sizeMax) {
    this.maxSize = sizeMax;
  }

  /**
   * Sets the progress listener.
   *
   * @param progressListener The progress listener, if any. Defaults to null.
   */
  public void setProgressListener(final @Nullable ProgressListener progressListener) {
    this.progressListener = progressListener != null ? progressListener : ProgressListener.NOP;
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
   * Sets the directory in which uploaded files will be stored, if stored on disk.
   *
   * @param tempRepository The directory path where temporary files should be stored.
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

}

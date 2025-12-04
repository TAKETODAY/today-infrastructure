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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import infra.http.ContentDisposition;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.StreamUtils;
import infra.web.RequestContext;
import infra.web.multipart.MultipartException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class FieldItemInputIterator {

  /**
   * The multipart stream to process.
   */
  public final MultipartInput multiPartInput;

  /**
   * The notifier, which used for triggering the {@link ProgressListener}.
   */
  private final ProgressNotifier progressNotifier;

  /**
   * The boundary, which separates the various parts.
   */
  private final byte[] multipartBoundary;

  /**
   * The item, which we currently process.
   */
  private @Nullable FieldItemInput currentItem;

  /**
   * The current items field name.
   */
  private @Nullable String currentFieldName;

  /**
   * Whether we are currently skipping the preamble.
   */
  private boolean skipPreamble;

  /**
   * Whether the current item may still be read.
   */
  private boolean itemValid;

  /**
   * Whether we have seen the end of the file.
   */
  private boolean eof;

  /**
   * Is the Request of type {@code multipart/related}.
   */
  private final boolean multipartRelated;

  /**
   * Constructs a new instance.
   *
   * @param parser Main processor.
   * @param context The request context.
   * @throws MultipartException An error occurred while parsing the request.
   * @throws IOException An I/O error occurred.
   */
  FieldItemInputIterator(final DefaultMultipartParser parser, final RequestContext context) throws MultipartException, IOException {
    MediaType contentType = context.getHeaders().getContentType();
    Assert.state(contentType != null, "No contentType");

    this.skipPreamble = true;
    this.multipartRelated = "related".equals(contentType.getSubtype());
    this.progressNotifier = new ProgressNotifier(parser.getProgressListener(), context.getContentLength());

    InputStream inputStream = context.getInputStream();
    byte[] multipartBoundary = getBoundary(contentType);
    if (multipartBoundary == null) {
      StreamUtils.closeQuietly(inputStream); // avoid possible resource leak
      throw new MultipartException("the request was rejected because no multipart boundary was found");
    }

    this.multipartBoundary = multipartBoundary;

    try {
      this.multiPartInput = new MultipartInput(inputStream, multipartBoundary, progressNotifier, parser);
    }
    catch (IllegalArgumentException e) {
      StreamUtils.closeQuietly(inputStream); // avoid possible resource leak
      throw new MultipartContentTypeException("The boundary specified in the 'Content-type' header is too long", e);
    }

    findNextItem();
  }

  /**
   * Tests whether another instance of {@link FieldItemInput} is available.
   *
   * @return True, if one or more additional file items are available, otherwise false.
   * @throws MultipartException Parsing or processing the file item failed.
   */
  public boolean hasNext() throws MultipartException, IOException {
    if (eof) {
      return false;
    }
    if (itemValid) {
      return true;
    }
    return findNextItem();
  }

  /**
   * Returns the next available {@link FieldItemInput}.
   *
   * @return FieldItemInput instance, which provides access to the next file item.
   * @throws NoSuchElementException No more items are available. Use {@link #hasNext()} to prevent this exception.
   * @throws MultipartException Parsing or processing the file item failed.
   */
  @SuppressWarnings("NullAway")
  public FieldItemInput next() throws IOException {
    if (eof || !itemValid && !hasNext()) {
      throw new NoSuchElementException();
    }
    itemValid = false;
    return currentItem;
  }

  /**
   * Finds the next item, if any.
   *
   * @return True, if a next item was found, otherwise false.
   */
  private boolean findNextItem() throws MultipartException, IOException {
    if (eof) {
      return false;
    }
    if (currentItem != null) {
      currentItem.close();
      currentItem = null;
    }
    final var input = multiPartInput;
    for (; ; ) {
      final boolean nextPart;
      if (skipPreamble) {
        nextPart = input.skipPreamble();
      }
      else {
        nextPart = input.readBoundary();
      }
      if (!nextPart) {
        if (currentFieldName == null) {
          // Outer multipart terminated -> No more data
          eof = true;
          return false;
        }
        // Inner multipart terminated -> Return to parsing the outer
        input.setBoundary(multipartBoundary);
        currentFieldName = null;
        continue;
      }
      final HttpHeaders headers = parseHeaders(input.readHeaders());
      MediaType subContentType = headers.getContentType();
      if (multipartRelated) {
        currentFieldName = "";
        currentItem = new FieldItemInput(this, null, "", subContentType, false, headers);
        progressNotifier.onItem();
        itemValid = true;
        return true;
      }
      ContentDisposition contentDisposition = headers.getContentDisposition();
      if (currentFieldName == null) {
        // We're parsing the outer multipart
        final String fieldName = contentDisposition.getName();
        if (fieldName != null) {
          if (subContentType != null && subContentType.equalsTypeAndSubtype(MediaType.MULTIPART_MIXED)) {
            currentFieldName = fieldName;
            // Multiple files associated with this field name
            byte[] subBoundary = getBoundary(subContentType);
            if (subBoundary == null) {
              throw new MultipartBoundaryException("The request was rejected because no boundary token was defined for a multipart/mixed part");
            }
            input.setBoundary(subBoundary);
            skipPreamble = true;
            continue;
          }
          final String fileName = contentDisposition.getFilename();
          currentItem = new FieldItemInput(this, fileName, fieldName, subContentType, fileName == null, headers);
          progressNotifier.onItem();
          itemValid = true;
          return true;
        }
      }
      else {
        final String fileName = contentDisposition.getFilename();
        if (fileName != null) {
          currentItem = new FieldItemInput(this, fileName, currentFieldName, subContentType, false, headers);
          progressNotifier.onItem();
          itemValid = true;
          return true;
        }
      }
      input.discardBodyData();
    }
  }

  private static byte @Nullable [] getBoundary(MediaType subContentType) {
    String boundary = subContentType.getParameter("boundary");
    return boundary != null ? boundary.getBytes(StandardCharsets.ISO_8859_1) : null;
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
  private HttpHeaders parseHeaders(final String headerPart) {
    final int len = headerPart.length();
    final HttpHeaders headers = HttpHeaders.forWritable();
    int start = 0;
    for (; ; ) {
      var end = parseEndOfLine(headerPart, start);
      if (start == end) {
        break;
      }
      final StringBuilder header = new StringBuilder(headerPart.substring(start, end));
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

}

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
import java.util.Objects;

import infra.http.ContentDisposition;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.StreamUtils;
import infra.web.RequestContext;
import infra.web.multipart.MultipartException;
import infra.web.multipart.NotMultipartRequestException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class FieldItemInputIterator {

  private final DefaultMultipartParser parser;

  /**
   * The maximum allowed size of a single uploaded file.
   */
  private final long fileSizeMax;

  /**
   * The multi part stream to process.
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
    if (!context.isMultipart()) {
      throw new NotMultipartRequestException(String.format("the request doesn't contain a %s or %s stream, content type header is %s",
              MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_MIXED_VALUE, context.getContentType()), null);
    }
    MediaType contentType = context.getHeaders().getContentType();
    Assert.state(contentType != null, "No contentType");

    this.parser = parser;
    this.skipPreamble = true;
    this.fileSizeMax = parser.getMaxFieldSize();
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
      MultipartInput multiPartInput = MultipartInput.builder()
              .setInputStream(inputStream)
              .boundary(multipartBoundary)
              .progressNotifier(progressNotifier)
              .maxPartHeaderSize(parser.getMaxPartHeaderSize())
              .get();

      multiPartInput.setHeaderCharset(Objects.requireNonNullElse(parser.getHeaderCharset(), contentType.getCharset()));
      this.multiPartInput = multiPartInput;
    }
    catch (IllegalArgumentException e) {
      StreamUtils.closeQuietly(inputStream); // avoid possible resource leak
      throw new MultipartContentTypeException("The boundary specified in the 'Content-type' header is too long", e);
    }

    findNextItem();
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
      final HttpHeaders headers = parser.parseHeaders(input.readHeaders());
      MediaType subContentType = headers.getContentType();
      if (multipartRelated) {
        currentFieldName = "";
        currentItem = new FieldItemInput(this, null, null,
                subContentType, false, headers);
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
          currentItem = new FieldItemInput(this, fileName, fieldName, subContentType,
                  fileName == null, headers);
          progressNotifier.onItem();
          itemValid = true;
          return true;
        }
      }
      else {
        final String fileName = contentDisposition.getFilename();
        if (fileName != null) {
          currentItem = new FieldItemInput(this, fileName, currentFieldName,
                  subContentType, false, headers);
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

  public long getFileSizeMax() {
    return fileSizeMax;
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
  public FieldItemInput next() throws IOException {
    if (eof || !itemValid && !hasNext()) {
      throw new NoSuchElementException();
    }
    itemValid = false;
    return currentItem;
  }

}

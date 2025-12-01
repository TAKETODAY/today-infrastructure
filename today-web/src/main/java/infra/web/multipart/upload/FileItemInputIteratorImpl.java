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
package infra.web.multipart.upload;

import org.apache.commons.io.input.BoundedInputStream;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.StreamUtils;
import infra.web.RequestContext;
import infra.web.multipart.NotMultipartRequestException;

/**
 * The iterator returned by {@link FileUpload#getItemIterator(RequestContext)}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class FileItemInputIteratorImpl implements FileItemInputIterator {

  /**
   * The Content-Type Pattern for multipart/related Requests.
   */
  private static final Pattern MULTIPART_RELATED =
          Pattern.compile("^\\s*multipart/related.*", Pattern.CASE_INSENSITIVE);

  /**
   * The file uploads processing utility.
   *
   * @see FileUpload
   */
  private final FileUpload<?, ?> fileUpload;

  /**
   * The request context.
   */
  private final RequestContext context;

  /**
   * The maximum allowed size of a complete request.
   */
  private final long sizeMax;

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
  private @Nullable FileItemInputImpl currentItem;

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
   * @param fileUpload Main processor.
   * @param context The request context.
   * @throws FileUploadException An error occurred while parsing the request.
   * @throws IOException An I/O error occurred.
   */
  FileItemInputIteratorImpl(final FileUpload<?, ?> fileUpload, final RequestContext context) throws FileUploadException, IOException {
    if (!context.isMultipart()) {
      throw new NotMultipartRequestException(String.format("the request doesn't contain a %s or %s stream, content type header is %s",
              FileUpload.MULTIPART_FORM_DATA, FileUpload.MULTIPART_MIXED, context.getContentType()), null);
    }
    this.context = context;
    this.fileUpload = fileUpload;
    this.sizeMax = fileUpload.getMaxSize();
    this.fileSizeMax = fileUpload.getMaxFileSize();
    this.multipartRelated = MULTIPART_RELATED.matcher(context.getContentType()).matches();
    this.skipPreamble = true;
    this.progressNotifier = new ProgressNotifier(fileUpload.getProgressListener(), context.getContentLength());

    final var requestSize = context.getContentLength();
    final InputStream inputStream; // This is eventually closed in MultipartInput processing
    if (sizeMax >= 0) {
      if (requestSize != -1 && requestSize > sizeMax) {
        throw new FileUploadSizeException(
                String.format("the request was rejected because its size (%s) exceeds the configured maximum (%s)", requestSize, sizeMax), sizeMax,
                requestSize);
      }
      // This is eventually closed in MultipartInput processing
      inputStream = BoundedInputStream.builder()
              .setInputStream(context.getInputStream())
              .setMaxCount(sizeMax)
              .setOnMaxCount((max, count) -> {
                throw new FileUploadSizeException(
                        String.format("The request was rejected because its size (%s) exceeds the configured maximum (%s)", count, max), max, count);
              })
              .get();
    }
    else {
      inputStream = context.getInputStream();
    }
    MediaType contentType = context.getHeaders().getContentType();
    Assert.state(contentType != null, "No contentType");
    String boundary = contentType.getParameter("boundary");
    byte[] multipartBoundary = boundary != null ? boundary.getBytes(StandardCharsets.ISO_8859_1) : null;
    if (multipartBoundary == null) {
      StreamUtils.closeQuietly(inputStream); // avoid possible resource leak
      throw new FileUploadException("the request was rejected because no multipart boundary was found");
    }

    this.multipartBoundary = multipartBoundary;

    try {
      MultipartInput multiPartInput = MultipartInput.builder()
              .setInputStream(inputStream)
              .boundary(multipartBoundary)
              .progressNotifier(progressNotifier)
              .maxPartHeaderSize(fileUpload.getMaxPartHeaderSize())
              .get();

      multiPartInput.setHeaderCharset(Objects.requireNonNullElse(fileUpload.getHeaderCharset(), contentType.getCharset()));
      this.multiPartInput = multiPartInput;
    }
    catch (IllegalArgumentException e) {
      StreamUtils.closeQuietly(inputStream); // avoid possible resource leak
      throw new FileUploadContentTypeException(String.format("The boundary specified in the %s header is too long", FileUpload.CONTENT_TYPE), e);
    }

    findNextItem();
  }

  /**
   * Finds the next item, if any.
   *
   * @return True, if a next item was found, otherwise false.
   */
  private boolean findNextItem() throws FileUploadException, IOException {
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
      final var headers = fileUpload.getParsedHeaders(input.readHeaders());
      if (multipartRelated) {
        currentFieldName = "";
        currentItem = new FileItemInputImpl(this, null, null, headers.getFirst(FileUpload.CONTENT_TYPE),
                false, getContentLength(headers));
        currentItem.setHeaders(headers);
        progressNotifier.noteItem();
        itemValid = true;
        return true;
      }
      if (currentFieldName == null) {
        // We're parsing the outer multipart
        final String fieldName = fileUpload.getFieldName(headers);
        if (fieldName != null) {
          final var subContentType = headers.getFirst(FileUpload.CONTENT_TYPE);
          if (subContentType != null && subContentType.toLowerCase(Locale.ROOT).startsWith(FileUpload.MULTIPART_MIXED)) {
            currentFieldName = fieldName;
            // Multiple files associated with this field name
            final var subBoundary = fileUpload.getBoundary(subContentType);
            if (subBoundary == null) {
              throw new FileUploadBoundaryException("The request was rejected because no boundary token was defined for a multipart/mixed part");
            }
            input.setBoundary(subBoundary);
            skipPreamble = true;
            continue;
          }
          final var fileName = fileUpload.getFileName(headers);
          currentItem = new FileItemInputImpl(this, fileName, fieldName, subContentType,
                  fileName == null, getContentLength(headers));
          currentItem.setHeaders(headers);
          progressNotifier.noteItem();
          itemValid = true;
          return true;
        }
      }
      else {
        final String fileName = fileUpload.getFileName(headers);
        if (fileName != null) {
          currentItem = new FileItemInputImpl(this, fileName, currentFieldName, headers.getFirst(FileUpload.CONTENT_TYPE),
                  false, getContentLength(headers));
          currentItem.setHeaders(headers);
          progressNotifier.noteItem();
          itemValid = true;
          return true;
        }
      }
      input.discardBodyData();
    }
  }

  private long getContentLength(final HttpHeaders headers) {
    return headers.getContentLength();
  }

  @Override
  public long getFileSizeMax() {
    return fileSizeMax;
  }

  /**
   * Tests whether another instance of {@link FileItemInput} is available.
   *
   * @return True, if one or more additional file items are available, otherwise false.
   * @throws FileUploadException Parsing or processing the file item failed.
   */
  @Override
  public boolean hasNext() throws FileUploadException, IOException {
    if (eof) {
      return false;
    }
    if (itemValid) {
      return true;
    }
    return findNextItem();
  }

  /**
   * Returns the next available {@link FileItemInput}.
   *
   * @return FileItemInput instance, which provides access to the next file item.
   * @throws NoSuchElementException No more items are available. Use {@link #hasNext()} to prevent this exception.
   * @throws FileUploadException Parsing or processing the file item failed.
   */
  @Override
  public FileItemInput next() throws IOException {
    if (eof || !itemValid && !hasNext()) {
      throw new NoSuchElementException();
    }
    itemValid = false;
    return currentItem;
  }

}

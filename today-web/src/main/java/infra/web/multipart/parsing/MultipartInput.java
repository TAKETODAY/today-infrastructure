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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import infra.util.StreamUtils;

/**
 * API for processing multipart request.
 *
 * <p>
 * This class can be used to process data streams conforming to MIME
 * 'multipart' format as defined in <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC
 * 1867</a>. Arbitrarily large amounts of data in the stream can be
 * processed under constant memory usage.
 * </p>
 * <p>
 * The format of the stream is defined in the following way:
 * </p>
 * <pre>
 *   multipart-body := preamble 1*encapsulation close-delimiter epilogue<br>
 *   encapsulation := delimiter body CRLF<br>
 *   delimiter := "--" boundary CRLF<br>
 *   close-delimiter := "--" boundary "--"<br>
 *   preamble := &lt;ignore&gt;<br>
 *   epilogue := &lt;ignore&gt;<br>
 *   body := header-part CRLF body-part<br>
 *   header-part := 1*header CRLF<br>
 *   header := header-name ":" header-value<br>
 *   header-name := &lt;printable ASCII characters except ":"&gt;<br>
 *   header-value := &lt;any ASCII characters except CR &amp; LF&gt;<br>
 *   body-data := &lt;arbitrary data&gt;<br>
 * </pre>
 *
 * <p>
 * Note that body-data can contain another multipart entity. There is
 * limited support for single pass processing of such nested streams.
 * The nested stream is <strong>required</strong> to have a boundary
 * token of the same length as the parent stream (see {@link #setBoundary(byte[])}).
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class MultipartInput {

  /**
   * The Carriage Return ASCII character value.
   */
  public static final byte CR = 0x0D;

  /**
   * The Line Feed ASCII character value.
   */
  public static final byte LF = 0x0A;

  /**
   * The dash (-) ASCII character value.
   */
  public static final byte DASH = 0x2D;

  /**
   * Default per part header size limit in bytes.
   */
  public static final int DEFAULT_PART_HEADER_SIZE_MAX = 512;

  /**
   * A byte sequence that marks the end of {@code header-part} ({@code CRLFCRLF}).
   */
  static final byte[] HEADER_SEPARATOR = { CR, LF, CR, LF };

  /**
   * A byte sequence that that follows a delimiter that will be followed by an encapsulation ({@code CRLF}).
   */
  static final byte[] FIELD_SEPARATOR = { CR, LF };

  /**
   * A byte sequence that that follows a delimiter of the last encapsulation in the stream ({@code --}).
   */
  static final byte[] STREAM_TERMINATOR = { DASH, DASH };

  /**
   * A byte sequence that precedes a boundary ({@code CRLF--}).
   */
  static final byte[] BOUNDARY_PREFIX = { CR, LF, DASH, DASH };

  private final DefaultMultipartParser parser;

  /**
   * The input stream from which data is read.
   */
  private final InputStream input;

  /**
   * The amount of data, in bytes, that must be kept in the buffer in order to detect delimiters reliably.
   */
  private final int keepRegion;

  /**
   * The byte sequence that partitions the stream.
   */
  private final byte[] boundary;

  /**
   * The table for Knuth-Morris-Pratt search algorithm.
   */
  private final int[] boundaryTable;

  /**
   * The length of the buffer used for processing the request.
   */
  private final int bufSize;

  /**
   * The buffer used for processing the request.
   */
  private final byte[] buffer;

  /**
   * The length of the boundary token plus the leading {@code CRLF--}.
   */
  private int boundaryLength;

  /**
   * The index of first valid character in the buffer. <br>
   * 0 <= head < bufSize
   */
  private int head;

  /**
   * The index of last valid character in the buffer + 1. <br>
   * 0 <= tail <= bufSize
   */
  private int tail;

  /**
   * The progress notifier, if any, or null.
   */
  private final ProgressNotifier notifier;

  /**
   * The maximum size of the headers in bytes.
   */
  private final int maxPartHeaderSize;

  /**
   * Constructs a {@code MultipartInput} with a custom size buffer.
   * <p>
   * Note that the buffer must be at least big enough to contain the boundary string, plus 4 characters for CR/LF and double dash, plus at least one byte of
   * data. Too small a buffer size setting will degrade performance.
   * </p>
   *
   * @param input The {@code InputStream} to serve as a data source.
   * @param boundary The token used for dividing the stream into {@code encapsulations}.
   * @param notifier The notifier, which is used for calling the progress listener, if any.
   */
  public MultipartInput(InputStream input, byte[] boundary, ProgressNotifier notifier, DefaultMultipartParser parser) {
    int bufferSize = parser.getParsingBufferSize(); // bufferSize > 512B
    // We prepend CR/LF to the boundary to chop trailing CR/LF from
    // body-data tokens.
    this.boundaryLength = boundary.length + BOUNDARY_PREFIX.length;
    this.input = input;
    this.parser = parser;
    this.notifier = notifier;
    this.bufSize = Math.max(bufferSize, boundaryLength * 2);
    this.buffer = new byte[bufSize];
    this.maxPartHeaderSize = parser.getMaxHeaderSize();
    this.boundary = new byte[this.boundaryLength];
    this.boundaryTable = new int[this.boundaryLength + 1];
    this.keepRegion = this.boundary.length;
    System.arraycopy(BOUNDARY_PREFIX, 0, this.boundary, 0, BOUNDARY_PREFIX.length);
    System.arraycopy(boundary, 0, this.boundary, BOUNDARY_PREFIX.length, boundary.length);
    computeBoundaryTable();
    head = 0;
    tail = 0;
  }

  /**
   * Computes the table used for Knuth-Morris-Pratt search algorithm.
   */
  private void computeBoundaryTable() {
    int position = 2;
    int candidate = 0;

    final byte[] boundary = this.boundary;
    final int[] boundaryTable = this.boundaryTable;
    final int boundaryLength = this.boundaryLength;

    boundaryTable[0] = -1;
    boundaryTable[1] = 0;

    while (position <= boundaryLength) {
      if (boundary[position - 1] == boundary[candidate]) {
        boundaryTable[position] = candidate + 1;
        candidate++;
        position++;
      }
      else if (candidate > 0) {
        candidate = boundaryTable[candidate];
      }
      else {
        boundaryTable[position] = 0;
        position++;
      }
    }
  }

  /**
   * Reads {@code body-data} from the current {@code encapsulation} and discards it.
   * <p>
   * Use this method to skip encapsulations you don't need or don't understand.
   * </p>
   *
   * @return The amount of data discarded.
   * @throws MalformedStreamException if the stream ends unexpectedly.
   * @throws IOException if an i/o error occurs.
   */
  public long discardBodyData() throws MalformedStreamException, IOException {
    return readBodyData(OutputStream.nullOutputStream());
  }

  /**
   * Searches for the {@code boundary} in the {@code buffer} region delimited by {@code head} and {@code tail}.
   *
   * @return The position of the boundary found, counting from the beginning of the {@code buffer}, or {@code -1} if not found.
   */
  private int findSeparator() {
    int bufferPos = this.head;
    int tablePos = 0;
    final int tail = this.tail;
    final byte[] buffer = this.buffer;
    final byte[] boundary = this.boundary;
    final int[] boundaryTable = this.boundaryTable;
    final int boundaryLength = this.boundaryLength;

    while (bufferPos < tail) {
      while (tablePos >= 0 && buffer[bufferPos] != boundary[tablePos]) {
        tablePos = boundaryTable[tablePos];
      }
      bufferPos++;
      tablePos++;
      if (tablePos == boundaryLength) {
        return bufferPos - boundaryLength;
      }
    }
    return -1;
  }

  /**
   * Creates a new {@link ItemInputStream}.
   *
   * @return A new instance of {@link ItemInputStream}.
   */
  public ItemInputStream newInputStream() {
    return new ItemInputStream();
  }

  /**
   * Reads {@code body-data} from the current {@code encapsulation} and
   * writes its contents into the output {@code Stream}.
   * <p>
   * Arbitrary large amounts of data can be processed by this method
   * using a constant size buffer. (see {@link MultipartInput#MultipartInput}).
   * </p>
   *
   * @param output The {@code Stream} to write data into. May be null, in which case this method is equivalent to {@link #discardBodyData()}.
   * @return the amount of data written.
   * @throws MalformedStreamException if the stream ends unexpectedly.
   * @throws IOException if an i/o error occurs.
   */
  public long readBodyData(final OutputStream output) throws MalformedStreamException, IOException {
    try (var inputStream = newInputStream()) {
      return StreamUtils.copy(inputStream, output);
    }
  }

  /**
   * Skips a {@code boundary} token, and checks whether more {@code encapsulations} are contained in the stream.
   *
   * @return {@code true} if there are more encapsulations in this stream; {@code false} otherwise.
   * @throws MultipartSizeException if the bytes read from the stream exceeded the size limits
   * @throws MalformedStreamException if the stream ends unexpectedly or fails to follow required syntax.
   */
  public boolean readBoundary() throws MultipartSizeException, MalformedStreamException {
    final var marker = new byte[2];
    final boolean nextChunk;
    head += boundaryLength;
    try {
      marker[0] = readByte();
      if (marker[0] == LF) {
        // Work around IE5 Mac bug with input type=image.
        // Because the boundary delimiter, not including the trailing
        // CRLF, must not appear within any file (RFC 2046, section
        // 5.1.1), we know the missing CR is due to a buggy browser
        // rather than a file containing something similar to a
        // boundary.
        return true;
      }

      marker[1] = readByte();
      if (arrayEquals(marker, STREAM_TERMINATOR, 2)) {
        nextChunk = false;
      }
      else if (arrayEquals(marker, FIELD_SEPARATOR, 2)) {
        nextChunk = true;
      }
      else {
        throw new MalformedStreamException("Unexpected characters follow a boundary");
      }
    }
    catch (final IOException e) {
      throw new MalformedStreamException("Stream ended unexpectedly", e);
    }
    return nextChunk;
  }

  /**
   * Reads a byte from the {@code buffer}, and refills it as necessary.
   *
   * @return The next byte from the input stream.
   * @throws IOException if there is no more data available.
   */
  public byte readByte() throws IOException {
    // Buffer depleted ?
    if (head == tail) {
      head = 0;
      // Refill.
      tail = input.read(buffer, head, bufSize);
      if (tail == -1) {
        // No more data available.
        throw new EOFException("No more data is available");
      }
      notifier.onBytesRead(tail);
    }
    return buffer[head++];
  }

  /**
   * Reads the {@code header-part} of the current {@code encapsulation}.
   * <p>
   * Headers are returned verbatim to the input stream, including the trailing {@code CRLF} marker. Parsing is left to the application.
   * </p>
   * <p>
   * <strong>TODO</strong> allow limiting maximum header size to protect against abuse.
   * </p>
   *
   * @return The {@code header-part} of the current encapsulation.
   * @throws MultipartSizeException if the bytes read from the stream exceeded the size limits.
   * @throws MalformedStreamException if the stream ends unexpectedly.
   */
  public String readHeaders() throws MultipartSizeException, MalformedStreamException {
    final int mphs = maxPartHeaderSize;
    int i = 0;
    int size = 0;
    // to support multibyte characters
    final var baos = new ByteArrayOutputStream();
    while (i < HEADER_SEPARATOR.length) {
      byte b;
      try {
        b = readByte();
      }
      catch (final IOException e) {
        throw new MalformedStreamException("Stream ended unexpectedly", e);
      }

      if (mphs != -1 && ++size > mphs) {
        throw new MultipartSizeException(
                String.format("Header section has more than %s bytes (maybe it is not properly terminated)", mphs), mphs, size);
      }
      if (b == HEADER_SEPARATOR[i]) {
        i++;
      }
      else {
        i = 0;
      }
      baos.write(b);
    }
    return baos.toString(parser.getDefaultCharset());
  }

  /**
   * Changes the boundary token used for partitioning the stream.
   * <p>
   * This method allows single pass processing of nested multipart streams.
   * </p>
   * <p>
   * The boundary token of the nested stream is {@code required} to be of the same length as the boundary token in parent stream.
   * </p>
   * <p>
   * Restoring the parent stream boundary token after processing of a nested stream is left to the application.
   * </p>
   *
   * @param boundary The boundary to be used for parsing of the nested stream.
   * @throws MultipartBoundaryException if the {@code boundary} has a different length than the one being currently parsed.
   */
  public void setBoundary(final byte[] boundary) throws MultipartBoundaryException {
    if (boundary.length != boundaryLength - BOUNDARY_PREFIX.length) {
      throw new MultipartBoundaryException("The length of a boundary token cannot be changed");
    }
    System.arraycopy(boundary, 0, this.boundary, BOUNDARY_PREFIX.length, boundary.length);
    computeBoundaryTable();
  }

  /**
   * Finds the beginning of the first {@code encapsulation}.
   *
   * @return {@code true} if an {@code encapsulation} was found in the stream.
   * @throws IOException if an i/o error occurs.
   */
  public boolean skipPreamble() throws IOException {
    // First delimiter may be not preceded with a CRLF.
    System.arraycopy(boundary, 2, boundary, 0, boundary.length - 2);
    boundaryLength = boundary.length - 2;
    computeBoundaryTable();
    try {
      // Discard all data up to the delimiter.
      discardBodyData();

      // Read boundary - if succeeded, the stream contains an
      // encapsulation.
      return readBoundary();
    }
    catch (final MalformedStreamException e) {
      return false;
    }
    finally {
      // Restore delimiter.
      System.arraycopy(boundary, 0, boundary, 2, boundary.length - 2);
      boundaryLength = boundary.length;
      boundary[0] = CR;
      boundary[1] = LF;
      computeBoundaryTable();
    }
  }

  /**
   * Compares {@code count} first bytes in the arrays {@code a} and {@code b}.
   *
   * @param a The first array to compare.
   * @param b The second array to compare.
   * @param count How many bytes should be compared.
   * @return {@code true} if {@code count} first bytes in arrays {@code a} and {@code b} are equal.
   */
  static boolean arrayEquals(final byte[] a, final byte[] b, final int count) {
    for (var i = 0; i < count; i++) {
      if (a[i] != b[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * An {@link InputStream} for reading an items contents.
   */
  public class ItemInputStream extends InputStream {

    /**
     * Offset when converting negative bytes to integers.
     */
    private static final int BYTE_POSITIVE_OFFSET = 256;

    /*
     * The number of bytes, which have been read so far.
     */
    //private long total;

    /**
     * The number of bytes, which must be hold, because they might be a part of the boundary.
     */
    private int pad;

    /**
     * The current offset in the buffer.
     */
    private int pos;

    /**
     * Whether the stream is already closed.
     */
    private boolean closed;

    /**
     * Creates a new instance.
     */
    ItemInputStream() {
      findSeparator();
    }

    /**
     * Returns the number of bytes, which are currently available, without blocking.
     *
     * @return Number of bytes in the buffer.
     * @throws IOException An I/O error occurs.
     */
    @Override
    public int available() throws IOException {
      if (pos == -1) {
        return tail - head - pad;
      }
      return pos - head;
    }

    private void checkOpen() throws ItemSkippedException {
      if (closed) {
        throw new ItemSkippedException("checkOpen()");
      }
    }

    /**
     * Closes the input stream.
     *
     * @throws IOException An I/O error occurred.
     */
    @Override
    public void close() throws IOException {
      close(false);
    }

    /**
     * Closes the input stream.
     *
     * @param closeUnderlying Whether to close the underlying stream (hard close)
     * @throws IOException An I/O error occurred.
     */
    public void close(final boolean closeUnderlying) throws IOException {
      if (closed) {
        return;
      }
      if (closeUnderlying) {
        closed = true;
        input.close();
      }
      else {
        for (; ; ) {
          var avail = available();
          if (avail == 0) {
            avail = makeAvailable();
            if (avail == 0) {
              break;
            }
          }
          if (skip(avail) != avail) {
            // TODO What to do?
          }
        }
      }
      closed = true;
    }

    /**
     * Called for finding the separator.
     */
    private void findSeparator() {
      pos = MultipartInput.this.findSeparator();
      if (pos == -1) {
        pad = Math.min(tail - head, keepRegion);
      }
    }

    /**
     * Attempts to read more data.
     *
     * @return Number of available bytes
     * @throws IOException An I/O error occurred.
     */
    private int makeAvailable() throws IOException {
      if (pos != -1) {
        return 0;
      }

      // Move the data to the beginning of the buffer.
      //total += tail - head - pad;
      final byte[] buffer = MultipartInput.this.buffer;
      System.arraycopy(buffer, tail - pad, buffer, 0, pad);

      // Refill buffer with new data.
      head = 0;
      tail = pad;

      final InputStream input = MultipartInput.this.input;
      for (; ; ) {
        final int bytesRead = input.read(buffer, tail, bufSize - tail);
        if (bytesRead == -1) {
          // The last pad amount is left in the buffer.
          // Boundary can't be in there so signal an error
          // condition.
          throw new MalformedStreamException("Stream ended unexpectedly");
        }
        notifier.onBytesRead(bytesRead);
        tail += bytesRead;

        findSeparator();
        final var av = available();

        if (av > 0 || pos != -1) {
          return av;
        }
      }
    }

    /**
     * Reads the next byte in the stream.
     *
     * @return The next byte in the stream, as a non-negative integer, or -1 for EOF.
     * @throws IOException An I/O error occurred.
     */
    @Override
    public int read() throws IOException {
      checkOpen();
      if (available() == 0 && makeAvailable() == 0) {
        return -1;
      }
      //++total;
      final int b = buffer[head++];
      if (b >= 0) {
        return b;
      }
      return b + BYTE_POSITIVE_OFFSET;
    }

    /**
     * Reads bytes into the given buffer.
     *
     * @param b The destination buffer, where to write to.
     * @param off Offset of the first byte in the buffer.
     * @param len Maximum number of bytes to read.
     * @return Number of bytes, which have been actually read, or -1 for EOF.
     * @throws IOException An I/O error occurred.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      checkOpen();
      if (len == 0) {
        return 0;
      }
      var res = available();
      if (res == 0) {
        res = makeAvailable();
        if (res == 0) {
          return -1;
        }
      }
      res = Math.min(res, len);
      System.arraycopy(buffer, head, b, off, res);
      head += res;
      //total += res;
      return res;
    }

    /**
     * Skips the given number of bytes.
     *
     * @param bytes Number of bytes to skip.
     * @return The number of bytes, which have actually been skipped.
     * @throws IOException An I/O error occurred.
     */
    @Override
    public long skip(final long bytes) throws IOException {
      checkOpen();
      var available = available();
      if (available == 0) {
        available = makeAvailable();
        if (available == 0) {
          return 0;
        }
      }
      final int res = Math.toIntExact(Math.min(available, bytes));
      head += res;
      return res;
    }

  }

}

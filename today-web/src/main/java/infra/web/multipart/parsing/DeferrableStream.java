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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An {@link OutputStream}, which keeps its data in memory, until a configured
 * threshold is reached. If that is the case, a temporary file is being created,
 * and the in-memory data is transferred to that file. All following data will
 * be written to that file, too.
 *
 * In other words: If an uploaded file is small, then it will be kept completely
 * in memory. On the other hand, if the uploaded file's size exceeds the
 * configured threshold, it it considered a large file, and the data is kept
 * in a temporary file.
 *
 * More precisely, this output stream supports three modes of operation:
 * <ol>
 *   <li>{@code threshold=0}: Don't create empty, temporary files. (Create a
 *     temporary file, as soon as the first byte is written.)</li>
 *   <li>{@code threshold>0}: Create a temporary file, if the size exceeds the
 *     threshold, otherwise keep the file in memory.</li>
 * </ol>
 */
final class DeferrableStream extends OutputStream {

  /**
   * This enumeration represents the possible states of the {@link DeferrableStream}.
   */
  public enum State {
    /**
     * The stream object has been created with a non-negative threshold,
     * but so far no data has been written.
     */
    initialized,
    /**
     * The stream object has been created with a non-negative threshold,
     * and some data has been written, but the threshold is not yet exceeded,
     * and the data is still kept in memory.
     */
    opened,
    /**
     * Either of the following conditions is given:
     * <ol>
     *   <li>The stream object has been created with a threshold of -1, or</li>
     *   <li>the stream object has been created with a non-negative threshold,
     *     and some data has been written. The number of bytes, that have
     *     been written, exceeds the configured threshold.</li>
     * </ol>
     * In either case, a temporary file has been created, and all data has been
     * written to the temporary file, erasing all existing data from memory.
     */
    persisted,
    /**
     * The stream has been closed, and data can no longer be written. It is
     * now valid to invoke {@link DeferrableStream#getInputStream()}.
     */
    closed
  }

  private final DefaultMultipartParser parser;

  /**
   * If no temporary file was created: A stream, to which the
   * incoming data is being written, until the threshold is reached.
   * Otherwise null.
   */
  private @Nullable ByteArrayOutputStream baos;

  /**
   * If a temporary file has been created: An open stream
   * for writing to that file. Otherwise null.
   */
  private @Nullable OutputStream out;

  /**
   * True, if the stream has ever been in state {@link State#persisted}.
   * Or, in other words: True, if a temporary file has been created.
   */
  private boolean wasPersisted;

  /**
   * If no temporary file was created, and the stream is closed:
   * The in-memory data, that was written to the stream. Otherwise null.
   */
  public byte @Nullable [] bytes;

  /**
   * If a temporary file has been created: Path of the temporary
   * file. Otherwise, null.
   *
   */
  public @Nullable Path path;

  /**
   * Number of bytes, that have been written to this stream so far.
   */
  public long size;

  /**
   * The streams current state.
   */
  public State state;

  /**
   * Creates a new instance with the given threshold, and the given supplier for a
   * temporary files path.
   * If the threshold is -1, then the temporary file will be created immediately, and
   * no in-memory data will be kept, at all.
   * If the threshold is 0, then the temporary file will be created, as soon as the
   * first byte will be written, but no in-memory data will be kept.
   * If the threshold is &gt; 0, then the temporary file will be created, as soon as that
   * number of bytes have been written. Up to that point, data will be kept in an
   * in-memory buffer.
   *
   * @throws IOException Creating the temporary file (in the case of threshold -1)
   * has failed.
   */
  @SuppressWarnings("NullAway")
  public DeferrableStream(DefaultMultipartParser parser) throws IOException {
    this.parser = parser;
    if (parser.getThreshold() == 0L) {
      persistStream();
    }
    else {
      baos = new ByteArrayOutputStream();
      bytes = null;
      state = State.initialized;
    }
  }

  /**
   * Called to check, whether the threshold will be exceeded, if the given number
   * of bytes are written to the stream. If so, persists the in-memory data by
   * creating a new, temporary file, and writing the in-memory data to the file.
   *
   * @param incomingBytes The number of bytes, which are about to be written.
   * @return The actual output stream, to which the incoming data may be written.
   * If the threshold is not yet exceeded, then this will be an internal
   * {@link ByteArrayOutputStream}, otherwise a stream, which is writing to the
   * temporary output file.
   * @throws IOException Persisting the in-memory data to a temporary file
   * has failed.
   */
  private @Nullable OutputStream checkThreshold(final int incomingBytes) throws IOException {
    return switch (state) {
      case initialized, opened -> {
//          final int bytesWritten = baos.size();
//          if ((long) bytesWritten + (long) incomingBytes >= parser.getThreshold()) {
        if ((size + (long) incomingBytes) >= parser.getThreshold()) {
          yield persistStream();
        }
        if (incomingBytes > 0) {
          state = State.opened;
        }
        yield baos;
      }
      case persisted -> out; // Do nothing, we're staying in the current state.
      case closed -> null; // Do nothing, we're staying in the current state.
    };
  }

  @Override
  @SuppressWarnings("NullAway")
  public void close() throws IOException {
    switch (state) {
      case initialized, opened:
        bytes = baos.toByteArray();
        baos = null;
        state = State.closed;
        break;
      case persisted:
        bytes = null;
        out.close();
        state = State.closed;
        break;
      case closed:
        // Already closed, do nothing.
        break;
    }
  }

  /**
   * If the stream is closed: Returns an {@link InputStream} on the
   * data, that has been written to this stream. Otherwise, throws
   * an {@link IllegalStateException}.
   *
   * @return An {@link InputStream} on the data, that has been
   * written. Never null.
   * @throws IllegalStateException The stream has not yet been
   * closed.
   * @throws IOException Creating the {@link InputStream} has
   * failed.
   */
  public InputStream getInputStream() throws IOException {
    if (state == State.closed) {
      if (bytes != null) {
        return new ByteArrayInputStream(bytes);
      }
      else {
        return Files.newInputStream(path);
      }
    }
    else {
      throw new IllegalStateException("This stream isn't yet closed.");
    }
  }

  /**
   * Returns true, if this stream was never persisted,
   * and no output file has been created.
   *
   * @return True, if the stream was never in state
   * {@link State#persisted}, otherwise false.
   */
  public boolean isInMemory() {
    return switch (state) {
      case initialized, opened -> true;
      case persisted -> false;
      case closed -> !wasPersisted;
    };
  }

  /**
   * Create the output file, change the state to {@code persisted}, and
   * return an {@link OutputStream}, which is writing to that file.
   *
   * @return The {@link OutputStream}, which is writing to the created,
   * temporary file.
   * @throws IOException Creating the temporary file has failed.
   */
  private OutputStream persistStream() throws IOException {
    final Path tempFile = Files.createTempFile(parser.getTempRepository(), null, ".tmp");
    final OutputStream os = Files.newOutputStream(tempFile);
    if (baos != null) {
      baos.writeTo(os);
    }
    /*
     * At this point, the output file has been successfully created,
     * and we can safely switch state.
     */
    state = State.persisted;
    wasPersisted = true;
    path = tempFile;
    out = os;
    baos = null;
    bytes = null;

    if (parser.isDeleteOnExit()) {
      tempFile.toFile().deleteOnExit();
    }
    return os;
  }

  @Override
  public void write(final byte[] buffer) throws IOException {
    write(buffer, 0, buffer.length);
  }

  @Override
  public void write(final byte[] buffer, final int offset, final int len) throws IOException {
    if (len > 0) {
      final OutputStream os = checkThreshold(len);
      if (os == null) {
        throw new IOException("This stream has already been closed.");
      }
      bytes = null;
      os.write(buffer, offset, len);
      size += len;
    }
  }

  @Override
  public void write(final int b) throws IOException {
    final OutputStream os = checkThreshold(1);
    if (os == null) {
      throw new IOException("This stream has already been closed.");
    }
    bytes = null;
    os.write(b);
    size++;
  }
}

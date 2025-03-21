/*
 * Copyright 2017 - 2024 the original author or authors.
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
package infra.mock.api.fileupload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream which will retain data in memory until a specified
 * threshold is reached, and only then commit it to disk. If the stream is
 * closed before the threshold is reached, the data will not be written to
 * disk at all.
 * <p>
 * This class originated in FileUpload processing. In this use case, you do
 * not know in advance the size of the file being uploaded. If the file is small
 * you want to store it in memory (for speed), but if the file is large you want
 * to store it to file (to avoid memory issues).
 */
public class DeferredFileOutputStream
        extends ThresholdingOutputStream {
  // ----------------------------------------------------------- Data members

  /**
   * The output stream to which data will be written prior to the threshold
   * being reached.
   */
  private ByteArrayOutputStream memoryOutputStream;

  /**
   * The output stream to which data will be written at any given time. This
   * will always be one of <code>memoryOutputStream</code> or
   * <code>diskOutputStream</code>.
   */
  private OutputStream currentOutputStream;

  /**
   * The file to which output will be directed if the threshold is exceeded.
   */
  private File outputFile;

  /**
   * The temporary file prefix.
   */
  private final String prefix;

  /**
   * The temporary file suffix.
   */
  private final String suffix;

  /**
   * The directory to use for temporary files.
   */
  private final File directory;

  // ----------------------------------------------------------- Constructors

  /**
   * Constructs an instance of this class which will trigger an event at the
   * specified threshold, and save data to a file beyond that point.
   * The initial buffer size will default to 1024 bytes which is ByteArrayOutputStream's default buffer size.
   *
   * @param threshold The number of bytes at which to trigger an event.
   * @param outputFile The file to which data is saved beyond the threshold.
   */
  public DeferredFileOutputStream(final int threshold, final File outputFile) {
    this(threshold, outputFile, null, null, null, ByteArrayOutputStream.DEFAULT_SIZE);
  }

  /**
   * Constructs an instance of this class which will trigger an event at the
   * specified threshold, and save data either to a file beyond that point.
   *
   * @param threshold The number of bytes at which to trigger an event.
   * @param outputFile The file to which data is saved beyond the threshold.
   * @param prefix Prefix to use for the temporary file.
   * @param suffix Suffix to use for the temporary file.
   * @param directory Temporary file directory.
   * @param initialBufferSize The initial size of the in memory buffer.
   */
  private DeferredFileOutputStream(final int threshold, final File outputFile, final String prefix,
          final String suffix, final File directory, final int initialBufferSize) {
    super(threshold);
    this.outputFile = outputFile;
    this.prefix = prefix;
    this.suffix = suffix;
    this.directory = directory;

    memoryOutputStream = new ByteArrayOutputStream(initialBufferSize);
    currentOutputStream = memoryOutputStream;
  }

  // --------------------------------------- ThresholdingOutputStream methods

  /**
   * Returns the current output stream. This may be memory based or disk
   * based, depending on the current state with respect to the threshold.
   *
   * @return The underlying output stream.
   * @throws IOException if an error occurs.
   */
  @Override
  protected OutputStream getStream() throws IOException {
    return currentOutputStream;
  }

  /**
   * Switches the underlying output stream from a memory based stream to one
   * that is backed by disk. This is the point at which we realise that too
   * much data is being written to keep in memory, so we elect to switch to
   * disk-based storage.
   *
   * @throws IOException if an error occurs.
   */
  @Override
  protected void thresholdReached() throws IOException {
    if (prefix != null) {
      outputFile = File.createTempFile(prefix, suffix, directory);
    }
    FileUtils.forceMkdirParent(outputFile);
    final FileOutputStream fos = new FileOutputStream(outputFile);
    try {
      memoryOutputStream.writeTo(fos);
    }
    catch (final IOException e) {
      fos.close();
      throw e;
    }
    currentOutputStream = fos;
    memoryOutputStream = null;
  }

  // --------------------------------------------------------- Public methods

  /**
   * Determines whether or not the data for this output stream has been
   * retained in memory.
   *
   * @return {@code true} if the data is available in memory;
   * {@code false} otherwise.
   */
  public boolean isInMemory() {
    return !isThresholdExceeded();
  }

  /**
   * Returns the data for this output stream as an array of bytes, assuming
   * that the data has been retained in memory. If the data was written to
   * disk, this method returns {@code null}.
   *
   * @return The data for this output stream, or {@code null} if no such
   * data is available.
   */
  public byte[] getData() {
    if (memoryOutputStream != null) {
      return memoryOutputStream.toByteArray();
    }
    return null;
  }

  /**
   * Returns either the output file specified in the constructor or
   * the temporary file created or null.
   * <p>
   * If the constructor specifying the file is used then it returns that
   * same output file, even when threshold has not been reached.
   * <p>
   * If constructor specifying a temporary file prefix/suffix is used
   * then the temporary file created once the threshold is reached is returned
   * If the threshold was not reached then {@code null} is returned.
   *
   * @return The file for this output stream, or {@code null} if no such
   * file exists.
   */
  public File getFile() {
    return outputFile;
  }

  /**
   * Closes underlying output stream, and mark this as closed
   *
   * @throws IOException if an error occurs.
   */
  @Override
  public void close() throws IOException {
    super.close();
  }
}

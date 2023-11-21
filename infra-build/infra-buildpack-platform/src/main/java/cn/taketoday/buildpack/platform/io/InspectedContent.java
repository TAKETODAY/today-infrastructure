/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.StreamUtils;

/**
 * {@link Content} that is reads and inspects a source of data only once but allows it to
 * be consumed multiple times.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InspectedContent implements Content {

  static final int MEMORY_LIMIT = 4 * 1024 + 3;

  private final int size;

  private final Object content;

  InspectedContent(int size, Object content) {
    this.size = size;
    this.content = content;
  }

  @Override
  public int size() {
    return this.size;
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    if (this.content instanceof byte[] bytes) {
      FileCopyUtils.copy(bytes, outputStream);
    }
    else if (this.content instanceof File file) {
      InputStream inputStream = new FileInputStream(file);
      FileCopyUtils.copy(inputStream, outputStream);
    }
    else {
      throw new IllegalStateException("Unknown content type");
    }
  }

  /**
   * Factory method to create an {@link InspectedContent} instance from a source input
   * stream.
   *
   * @param inputStream the content input stream
   * @param inspectors any inspectors to apply
   * @return a new inspected content instance
   * @throws IOException on IO error
   */
  public static InspectedContent of(InputStream inputStream, Inspector... inspectors) throws IOException {
    Assert.notNull(inputStream, "InputStream is required");
    return of((outputStream) -> FileCopyUtils.copy(inputStream, outputStream), inspectors);
  }

  /**
   * Factory method to create an {@link InspectedContent} instance from source content.
   *
   * @param content the content
   * @param inspectors any inspectors to apply
   * @return a new inspected content instance
   * @throws IOException on IO error
   */
  public static InspectedContent of(Content content, Inspector... inspectors) throws IOException {
    Assert.notNull(content, "Content is required");
    return of(content::writeTo, inspectors);
  }

  /**
   * Factory method to create an {@link InspectedContent} instance from a source write
   * method.
   *
   * @param writer a consumer representing the write method
   * @param inspectors any inspectors to apply
   * @return a new inspected content instance
   * @throws IOException on IO error
   */
  public static InspectedContent of(IOConsumer<OutputStream> writer, Inspector... inspectors) throws IOException {
    Assert.notNull(writer, "Writer is required");
    InspectingOutputStream outputStream = new InspectingOutputStream(inspectors);
    try (outputStream) {
      writer.accept(outputStream);
    }
    return new InspectedContent(outputStream.getSize(), outputStream.getContent());
  }

  /**
   * Interface that can be used to inspect content as it is initially read.
   */
  public interface Inspector {

    /**
     * Update inspected information based on the provided bytes.
     *
     * @param input the array of bytes.
     * @param offset the offset to start from in the array of bytes.
     * @param len the number of bytes to use, starting at {@code offset}.
     * @throws IOException on IO error
     */
    void update(byte[] input, int offset, int len) throws IOException;

  }

  /**
   * Internal {@link OutputStream} used to capture the content either as bytes, or to a
   * File if the content is too large.
   */
  private static final class InspectingOutputStream extends OutputStream {

    private final Inspector[] inspectors;

    private int size;

    private OutputStream delegate;

    private File tempFile;

    private final byte[] singleByteBuffer = new byte[0];

    private InspectingOutputStream(Inspector[] inspectors) {
      this.inspectors = inspectors;
      this.delegate = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
      this.singleByteBuffer[0] = (byte) (b & 0xFF);
      write(this.singleByteBuffer);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      int size = len - off;
      if (this.tempFile == null && (this.size + size) > MEMORY_LIMIT) {
        convertToTempFile();
      }
      this.delegate.write(b, off, len);
      for (Inspector inspector : this.inspectors) {
        inspector.update(b, off, len);
      }
      this.size += size;
    }

    private void convertToTempFile() throws IOException {
      this.tempFile = ApplicationTemp.instance.createFile(null, "buildpack", ".tmp")
              .toFile();
      byte[] bytes = ((ByteArrayOutputStream) this.delegate).toByteArray();
      this.delegate = new FileOutputStream(this.tempFile);
      StreamUtils.copy(bytes, this.delegate);
      tempFile.deleteOnExit();
    }

    private Object getContent() {
      return (this.tempFile != null) ? this.tempFile : ((ByteArrayOutputStream) this.delegate).toByteArray();
    }

    private int getSize() {
      return this.size;
    }

  }

}

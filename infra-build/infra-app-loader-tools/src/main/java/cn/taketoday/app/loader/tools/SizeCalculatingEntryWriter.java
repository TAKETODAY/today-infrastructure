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

package cn.taketoday.app.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;

/**
 * {@link EntryWriter} that always provides size information.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SizeCalculatingEntryWriter implements EntryWriter {

  static final int THRESHOLD = 1024 * 20;

  private final Object content;

  private final int size;

  private SizeCalculatingEntryWriter(EntryWriter entryWriter) throws IOException {
    SizeCalculatingOutputStream outputStream = new SizeCalculatingOutputStream();
    try (outputStream) {
      entryWriter.write(outputStream);
    }
    this.content = outputStream.getContent();
    this.size = outputStream.getSize();
  }

  @Override
  public void write(OutputStream outputStream) throws IOException {
    InputStream inputStream = getContentInputStream();
    copy(inputStream, outputStream);
  }

  private InputStream getContentInputStream() throws FileNotFoundException {
    if (this.content instanceof File file) {
      return new FileInputStream(file);
    }
    return new ByteArrayInputStream((byte[]) this.content);
  }

  private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (inputStream) {
      StreamUtils.copy(inputStream, outputStream);
    }
  }

  @Override
  public int size() {
    return this.size;
  }

  static EntryWriter get(@Nullable EntryWriter entryWriter) throws IOException {
    if (entryWriter == null || entryWriter.size() != -1) {
      return entryWriter;
    }
    return new SizeCalculatingEntryWriter(entryWriter);
  }

  /**
   * {@link OutputStream} to calculate the size and allow content to be written again.
   */
  private static class SizeCalculatingOutputStream extends OutputStream {

    private int size = 0;

    private File tempFile;

    private OutputStream outputStream;

    SizeCalculatingOutputStream() {
      this.outputStream = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
      write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      int updatedSize = this.size + len;
      if (updatedSize > THRESHOLD && this.outputStream instanceof ByteArrayOutputStream byteArrayOutputStream) {
        this.outputStream = convertToFileOutputStream(byteArrayOutputStream);
      }
      this.outputStream.write(b, off, len);
      this.size = updatedSize;
    }

    private OutputStream convertToFileOutputStream(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
      initializeTempFile();
      FileOutputStream fileOutputStream = new FileOutputStream(this.tempFile);
      StreamUtils.copy(byteArrayOutputStream.toByteArray(), fileOutputStream);
      return fileOutputStream;
    }

    private void initializeTempFile() throws IOException {
      if (this.tempFile == null) {
        this.tempFile = File.createTempFile("springboot-", "-entrycontent");
        this.tempFile.deleteOnExit();
      }
    }

    @Override
    public void close() throws IOException {
      this.outputStream.close();
    }

    Object getContent() {
      return (this.outputStream instanceof ByteArrayOutputStream byteArrayOutputStream)
             ? byteArrayOutputStream.toByteArray() : this.tempFile;
    }

    int getSize() {
      return this.size;
    }

  }

}

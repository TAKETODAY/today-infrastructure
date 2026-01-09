/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.loader.tools;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import infra.core.ApplicationTemp;
import infra.lang.Assert;
import infra.util.StreamUtils;

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

  static @Nullable EntryWriter get(@Nullable EntryWriter entryWriter) throws IOException {
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

    @Nullable
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
      File tempFile = initializeTempFile();
      FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
      StreamUtils.copy(byteArrayOutputStream.toByteArray(), fileOutputStream);
      return fileOutputStream;
    }

    private File initializeTempFile() {
      if (this.tempFile == null) {
        this.tempFile = ApplicationTemp.instance.createFile(
                null, "infra-app-", "-entrycontent").toFile();
        this.tempFile.deleteOnExit();
      }
      return tempFile;
    }

    @Override
    public void close() throws IOException {
      this.outputStream.close();
    }

    Object getContent() {
      Object result = (this.outputStream instanceof ByteArrayOutputStream byteArrayOutputStream)
              ? byteArrayOutputStream.toByteArray() : this.tempFile;
      Assert.state(result != null, "'result' is required");
      return result;
    }

    int getSize() {
      return this.size;
    }

  }

}

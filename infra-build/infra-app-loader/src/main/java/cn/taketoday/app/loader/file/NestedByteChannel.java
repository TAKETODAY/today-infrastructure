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

package cn.taketoday.app.loader.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

import cn.taketoday.app.loader.net.protocol.nested.NestedLocation;
import cn.taketoday.app.loader.ref.Cleaner;
import cn.taketoday.app.loader.zip.CloseableDataBlock;
import cn.taketoday.app.loader.zip.DataBlock;
import cn.taketoday.app.loader.zip.ZipContent;

/**
 * {@link SeekableByteChannel} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NestedFileSystemProvider
 * @since 5.0
 */
class NestedByteChannel implements SeekableByteChannel {

  private long position;

  private final Resources resources;

  private final Cleanable cleanup;

  private final long size;

  private volatile boolean closed;

  NestedByteChannel(Path path, String nestedEntryName) throws IOException {
    this(path, nestedEntryName, Cleaner.instance);
  }

  NestedByteChannel(Path path, String nestedEntryName, Cleaner cleaner) throws IOException {
    this.resources = new Resources(path, nestedEntryName);
    this.cleanup = cleaner.register(this, this.resources);
    this.size = this.resources.getData().size();
  }

  @Override
  public boolean isOpen() {
    return !this.closed;
  }

  @Override
  public void close() throws IOException {
    if (this.closed) {
      return;
    }
    this.closed = true;
    try {
      this.cleanup.clean();
    }
    catch (UncheckedIOException ex) {
      throw ex.getCause();
    }
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    assertNotClosed();
    int total = 0;
    while (dst.remaining() > 0) {
      int count = this.resources.getData().read(dst, this.position);
      if (count <= 0) {
        return (total != 0) ? 0 : count;
      }
      total += count;
      this.position += count;
    }
    return total;
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    throw new NonWritableChannelException();
  }

  @Override
  public long position() throws IOException {
    assertNotClosed();
    return this.position;
  }

  @Override
  public SeekableByteChannel position(long position) throws IOException {
    assertNotClosed();
    if (position < 0 || position >= this.size) {
      throw new IllegalArgumentException("Position must be in bounds");
    }
    this.position = position;
    return this;
  }

  @Override
  public long size() throws IOException {
    assertNotClosed();
    return this.size;
  }

  @Override
  public SeekableByteChannel truncate(long size) throws IOException {
    throw new NonWritableChannelException();
  }

  private void assertNotClosed() throws ClosedChannelException {
    if (this.closed) {
      throw new ClosedChannelException();
    }
  }

  /**
   * Resources used by the channel and suitable for registration with a {@link Cleaner}.
   */
  static class Resources implements Runnable {

    private final ZipContent zipContent;

    private final CloseableDataBlock data;

    Resources(Path path, String nestedEntryName) throws IOException {
      this.zipContent = ZipContent.open(path, nestedEntryName);
      this.data = this.zipContent.openRawZipData();
    }

    DataBlock getData() {
      return this.data;
    }

    @Override
    public void run() {
      releaseAll();
    }

    private void releaseAll() {
      IOException exception = null;
      try {
        this.data.close();
      }
      catch (IOException ex) {
        exception = ex;
      }
      try {
        this.zipContent.close();
      }
      catch (IOException ex) {
        if (exception != null) {
          ex.addSuppressed(exception);
        }
        exception = ex;
      }
      if (exception != null) {
        throw new UncheckedIOException(exception);
      }
    }

  }

}

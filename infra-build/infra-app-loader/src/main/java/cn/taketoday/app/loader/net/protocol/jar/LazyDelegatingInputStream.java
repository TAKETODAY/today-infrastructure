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

package cn.taketoday.app.loader.net.protocol.jar;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} that delegates lazily to another {@link InputStream}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
abstract class LazyDelegatingInputStream extends InputStream {

  private volatile InputStream in;

  @Override
  public int read() throws IOException {
    return in().read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return in().read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return in().read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return in().skip(n);
  }

  @Override
  public int available() throws IOException {
    return in().available();
  }

  @Override
  public boolean markSupported() {
    try {
      return in().markSupported();
    }
    catch (IOException ex) {
      return false;
    }
  }

  @Override
  public synchronized void mark(int readlimit) {
    try {
      in().mark(readlimit);
    }
    catch (IOException ex) {
      // Ignore
    }
  }

  @Override
  public synchronized void reset() throws IOException {
    in().reset();
  }

  private InputStream in() throws IOException {
    InputStream in = this.in;
    if (in == null) {
      synchronized(this) {
        in = this.in;
        if (in == null) {
          in = getDelegateInputStream();
          this.in = in;
        }
      }
    }
    return in;
  }

  @Override
  public void close() throws IOException {
    InputStream in = this.in;
    if (in != null) {
      synchronized(this) {
        in = this.in;
        if (in != null) {
          in.close();
        }
      }
    }
  }

  protected abstract InputStream getDelegateInputStream() throws IOException;

}

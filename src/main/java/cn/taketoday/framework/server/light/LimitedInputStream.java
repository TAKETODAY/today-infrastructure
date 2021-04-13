/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.server.light;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The {@code LimitedInputStream} provides access to a limited number
 * of consecutive bytes from the underlying InputStream, starting at its
 * current position. If this limit is reached, it behaves as though the end
 * of stream has been reached (although the underlying stream remains open
 * and may contain additional data).
 *
 * @author TODAY 2021/4/13 11:25
 */
public class LimitedInputStream extends FilterInputStream {

  protected long limit; // decremented when read, until it reaches zero
  protected boolean prematureEndException;

  /**
   * Constructs a LimitedInputStream with the given underlying
   * input stream and limit.
   *
   * @param in
   *         the underlying input stream
   * @param limit
   *         the maximum number of bytes that may be consumed from
   *         the underlying stream before this stream ends. If zero or
   *         negative, this stream will be at its end from initialization.
   * @param prematureEndException
   *         specifies the stream's behavior when
   *         the underlying stream end is reached before the limit is
   *         reached: if true, an exception is thrown, otherwise this
   *         stream reaches its end as well (i.e. read() returns -1)
   *
   * @throws NullPointerException
   *         if the given stream is null
   */
  public LimitedInputStream(InputStream in, long limit, boolean prematureEndException) {
    super(in);
    if (in == null)
      throw new NullPointerException("input stream is null");
    this.limit = limit < 0 ? 0 : limit;
    this.prematureEndException = prematureEndException;
  }

  @Override
  public int read() throws IOException {
    int res = limit == 0 ? -1 : in.read();
    if (res < 0 && limit > 0 && prematureEndException)
      throw new IOException("unexpected end of stream");
    limit = res < 0 ? 0 : limit - 1;
    return res;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int res = limit == 0 ? -1 : in.read(b, off, len > limit ? (int) limit : len);
    if (res < 0 && limit > 0 && prematureEndException)
      throw new IOException("unexpected end of stream");
    limit = res < 0 ? 0 : limit - res;
    return res;
  }

  @Override
  public long skip(long len) throws IOException {
    long res = in.skip(len > limit ? limit : len);
    limit -= res;
    return res;
  }

  @Override
  public int available() throws IOException {
    int res = in.available();
    return res > limit ? (int) limit : res;
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public void close() {
    limit = 0; // end this stream, but don't close the underlying stream
  }
}

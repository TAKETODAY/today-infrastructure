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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import static cn.taketoday.framework.server.light.HTTPServer.CRLF;

/**
 * The {@code ChunkedOutputStream} encodes an OutputStream with the
 * "chunked" transfer encoding. It should be used only when the content
 * length is not known in advance, and with the response Transfer-Encoding
 * header set to "chunked".
 * <p>
 * Data is written to the stream by calling the {@link #write(byte[], int, int)}
 * method, which writes a new chunk per invocation. To end the stream,
 * the {@link #writeTrailingChunk} method must be called or the stream closed.
 *
 * @author TODAY 2021/4/13 11:27
 */
public class ChunkedOutputStream extends FilterOutputStream {

  protected int state; // the current stream state

  /**
   * Constructs a ChunkedOutputStream with the given underlying stream.
   *
   * @param out
   *         the underlying output stream to which the chunked stream
   *         is written
   *
   * @throws NullPointerException
   *         if the given stream is null
   */
  public ChunkedOutputStream(OutputStream out) {
    super(out);
    if (out == null)
      throw new NullPointerException("output stream is null");
  }

  /**
   * Initializes a new chunk with the given size.
   *
   * @param size
   *         the chunk size (must be positive)
   *
   * @throws IllegalArgumentException
   *         if size is negative
   * @throws IOException
   *         if an IO error occurs, or the stream has
   *         already been ended
   */
  protected void initChunk(long size) throws IOException {
    if (size < 0)
      throw new IllegalArgumentException("invalid size: " + size);
    if (state > 0)
      out.write(CRLF); // end previous chunk
    else if (state == 0)
      state = 1; // start first chunk
    else
      throw new IOException("chunked stream has already ended");
    out.write(Utils.getBytes(Long.toHexString(size)));
    out.write(CRLF);
  }

  /**
   * Writes the trailing chunk which marks the end of the stream.
   *
   * @param headers
   *         the (optional) trailing headers to write, or null
   *
   * @throws IOException
   *         if an error occurs
   */
  public void writeTrailingChunk(Headers headers) throws IOException {
    initChunk(0); // zero-sized chunk marks the end of the stream
    if (headers == null)
      out.write(CRLF); // empty header block
    else
      headers.writeTo(out);
    state = -1;
  }

  /**
   * Writes a chunk containing the given byte. This method initializes
   * a new chunk of size 1, and then writes the byte as the chunk data.
   *
   * @param b
   *         the byte to write as a chunk
   *
   * @throws IOException
   *         if an error occurs
   */
  @Override
  public void write(int b) throws IOException {
    write(new byte[] { (byte) b }, 0, 1);
  }

  /**
   * Writes a chunk containing the given bytes. This method initializes
   * a new chunk of the given size, and then writes the chunk data.
   *
   * @param b
   *         an array containing the bytes to write
   * @param off
   *         the offset within the array where the data starts
   * @param len
   *         the length of the data in bytes
   *
   * @throws IOException
   *         if an error occurs
   * @throws IndexOutOfBoundsException
   *         if the given offset or length
   *         are outside the bounds of the given array
   */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (len > 0) // zero-sized chunk is the trailing chunk
      initChunk(len);
    out.write(b, off, len);
  }

  /**
   * Writes the trailing chunk if necessary, and closes the underlying stream.
   *
   * @throws IOException
   *         if an error occurs
   */
  @Override
  public void close() throws IOException {
    if (state > -1)
      writeTrailingChunk(null);
    super.close();
  }
}

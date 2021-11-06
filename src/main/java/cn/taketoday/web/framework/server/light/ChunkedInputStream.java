/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.framework.server.light;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.http.HttpHeaders;

import static cn.taketoday.web.framework.server.light.Utils.parseULong;
import static cn.taketoday.web.framework.server.light.Utils.readHeaders;
import static cn.taketoday.web.framework.server.light.Utils.readLine;

/**
 * The {@code ChunkedInputStream} decodes an InputStream whose data has the
 * "chunked" transfer encoding applied to it, providing the underlying data.
 *
 * @author TODAY 2021/4/13 11:26
 */
public class ChunkedInputStream extends LimitedInputStream {

  protected HttpHeaders headers;
  protected boolean initialized;
  protected final LightHttpConfig config;

  /**
   * Constructs a ChunkedInputStream with the given underlying stream, and
   * a headers container to which the stream's trailing headers will be
   * added.
   *
   * @param in the underlying "chunked"-encoded input stream
   * @param headers the headers container to which the stream's trailing
   * headers will be added, or null if they are to be discarded
   * @param config light http config
   * @throws NullPointerException if the given stream is null
   */
  public ChunkedInputStream(InputStream in, HttpHeaders headers, LightHttpConfig config) {
    super(in, 0, true);
    this.headers = headers;
    this.config = config;
  }

  @Override
  public int read() throws IOException {
    return limit <= 0 && initChunk() < 0 ? -1 : super.read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return limit <= 0 && initChunk() < 0 ? -1 : super.read(b, off, len);
  }

  /**
   * Initializes the next chunk. If the previous chunk has not yet
   * ended, or the end of stream has been reached, does nothing.
   *
   * @return the length of the chunk, or -1 if the end of stream
   * has been reached
   * @throws IOException if an IO error occurs or the stream is corrupt
   */
  protected long initChunk() throws IOException {
    if (limit == 0) { // finished previous chunk
      // read chunk-terminating CRLF if it's not the first chunk
      if (initialized && readLine(in).length() > 0)
        throw new IOException("chunk data must end with CRLF");
      initialized = true;
      limit = parseChunkSize(readLine(in)); // read next chunk size
      if (limit == 0) { // last chunk has size 0
        limit = -1; // mark end of stream
        // read trailing headers, if any
        HttpHeaders trailingHeaders = readHeaders(in, config);
        if (headers != null)
          headers.addAll(trailingHeaders);
      }
    }
    return limit;
  }

  /**
   * Parses a chunk-size line.
   *
   * @param line the chunk-size line to parse
   * @return the chunk size
   * @throws IllegalArgumentException if the chunk-size line is invalid
   */
  protected static long parseChunkSize(String line) throws IllegalArgumentException {
    int pos = line.indexOf(';');
    line = pos < 0 ? line : line.substring(0, pos); // ignore params, if any
    try {
      return parseULong(line, 16); // throws NFE
    }
    catch (NumberFormatException nfe) {
      throw new IllegalArgumentException(
              "invalid chunk size line: \"" + line + "\"");
    }
  }
}

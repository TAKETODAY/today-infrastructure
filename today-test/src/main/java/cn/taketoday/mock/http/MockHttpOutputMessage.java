/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.mock.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.util.StreamUtils;

/**
 * Mock implementation of {@link HttpOutputMessage}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockHttpOutputMessage implements HttpOutputMessage {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final HttpHeaders headers = HttpHeaders.create();

  private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);

  /**
   * Return the headers.
   */
  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  /**
   * Return the body content.
   */
  @Override
  public OutputStream getBody() throws IOException {
    return this.body;
  }

  /**
   * Return body content as a byte array.
   */
  public byte[] getBodyAsBytes() {
    return this.body.toByteArray();
  }

  /**
   * Return the body content interpreted as a UTF-8 string.
   */
  public String getBodyAsString() {
    return getBodyAsString(DEFAULT_CHARSET);
  }

  /**
   * Return the body content as a string.
   *
   * @param charset the charset to use to turn the body content to a String
   */
  public String getBodyAsString(Charset charset) {
    return StreamUtils.copyToString(this.body, charset);
  }

}

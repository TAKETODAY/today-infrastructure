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

package infra.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import infra.lang.Assert;

/**
 * Mock implementation of {@link HttpInputMessage}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockHttpInputMessage implements HttpInputMessage {

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final InputStream body;

  public MockHttpInputMessage(byte[] contents) {
    Assert.notNull(contents, "'contents' is required");
    this.body = new ByteArrayInputStream(contents);
  }

  public MockHttpInputMessage(InputStream body) {
    Assert.notNull(body, "'body' is required");
    this.body = body;
  }

  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    return body;
  }

}

/*
 * Copyright 2002-present the original author or authors.
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

package infra.mock.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
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

  public MockHttpInputMessage(byte[] content) {
    Assert.notNull(content, "Byte array is required");
    this.body = new ByteArrayInputStream(content);
  }

  public MockHttpInputMessage(InputStream body) {
    Assert.notNull(body, "InputStream is required");
    this.body = body;
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    return this.body;
  }

}

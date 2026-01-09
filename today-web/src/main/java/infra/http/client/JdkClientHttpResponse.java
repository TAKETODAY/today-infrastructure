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

package infra.http.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation based on the Java {@link HttpClient}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 19:57
 */
class JdkClientHttpResponse implements ClientHttpResponse {

  private final HttpResponse<InputStream> response;

  private final HttpHeaders headers;

  private final InputStream body;

  /**
   * @since 5.0
   */
  public JdkClientHttpResponse(HttpResponse<InputStream> response, HttpHeaders httpHeaders, @Nullable InputStream body) {
    this.response = response;
    this.headers = httpHeaders;
    this.body = body != null ? body : InputStream.nullInputStream();
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(this.response.statusCode());
  }

  @Override
  public String getStatusText() {
    // HttpResponse does not expose status text
    if (getStatusCode() instanceof HttpStatus status) {
      return status.getReasonPhrase();
    }
    else {
      return "";
    }
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    return this.body;
  }

  @Override
  public void close() {
    try (body) {
      StreamUtils.drain(body);
    }
    catch (IOException ignored) {
    }
  }
}

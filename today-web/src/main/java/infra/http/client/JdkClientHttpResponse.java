/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.lang.Nullable;
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

  public JdkClientHttpResponse(HttpResponse<InputStream> response) {
    this(response, response.body());
  }

  /**
   * @since 5.0
   */
  public JdkClientHttpResponse(HttpResponse<InputStream> response, @Nullable InputStream body) {
    this.response = response;
    this.headers = HttpHeaders.fromResponse(response);
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

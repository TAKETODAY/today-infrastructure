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

package infra.mock.http.client.reactive;

import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.reactive.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MockClientHttpResponse implements ClientHttpResponse {

  private final int status;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

  private Flux<DataBuffer> body = Flux.empty();

  public MockClientHttpResponse(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatus is required");
    this.status = status.value();
  }

  public MockClientHttpResponse(int status) {
    Assert.isTrue(status > 99 && status < 1000, "Status must be between 100 and 999");
    this.status = status;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(this.status);
  }

  @Override
  public int getRawStatusCode() {
    return this.status;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (!getCookies().isEmpty() && this.headers.get(HttpHeaders.SET_COOKIE) == null) {
      getCookies().values().stream().flatMap(Collection::stream)
              .forEach(cookie -> this.headers.add(HttpHeaders.SET_COOKIE, cookie.toString()));
    }
    return this.headers;
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return this.cookies;
  }

  public void setBody(Publisher<DataBuffer> body) {
    this.body = Flux.from(body);
  }

  public void setBody(String body) {
    setBody(body, StandardCharsets.UTF_8);
  }

  public void setBody(String body, Charset charset) {
    DataBuffer buffer = toDataBuffer(body, charset);
    this.body = Flux.just(buffer);
  }

  private DataBuffer toDataBuffer(String body, Charset charset) {
    byte[] bytes = body.getBytes(charset);
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    return DefaultDataBufferFactory.sharedInstance.wrap(byteBuffer);
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.body;
  }

  /**
   * Return the response body aggregated and converted to a String using the
   * charset of the Content-Type response or otherwise as "UTF-8".
   */
  public Mono<String> getBodyAsString() {
    return DataBufferUtils.join(getBody())
            .map(buffer -> {
              String s = buffer.toString(getCharset());
              buffer.release();
              return s;
            })
            .defaultIfEmpty("");
  }

  private Charset getCharset() {
    Charset charset = null;
    MediaType contentType = getHeaders().getContentType();
    if (contentType != null) {
      charset = contentType.getCharset();
    }
    return (charset != null ? charset : StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    HttpStatus code = HttpStatus.resolve(this.status);
    return (code != null ? code.name() + "(" + this.status + ")" : "Status (" + this.status + ")") + this.headers;
  }
}

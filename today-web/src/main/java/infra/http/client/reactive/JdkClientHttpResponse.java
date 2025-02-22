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

package infra.http.client.reactive;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Function;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.util.MultiValueMap;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

/**
 * {@link ClientHttpResponse} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JdkClientHttpResponse extends AbstractClientHttpResponse {

  public JdkClientHttpResponse(HttpResponse<Flow.Publisher<List<ByteBuffer>>> response,
          DataBufferFactory bufferFactory, MultiValueMap<String, ResponseCookie> cookies) {

    super(HttpStatusCode.valueOf(response.statusCode()),
            HttpHeaders.fromResponse(response), cookies, adaptBody(response, bufferFactory));
  }

  private static Flux<DataBuffer> adaptBody(HttpResponse<Flow.Publisher<List<ByteBuffer>>> response, DataBufferFactory bufferFactory) {
    return JdkFlowAdapter.flowPublisherToFlux(response.body())
            .flatMapIterable(Function.identity())
            .map(bufferFactory::wrap)
            .doOnDiscard(DataBuffer.class, DataBuffer.RELEASE_CONSUMER)
            .cache(0);
  }

}

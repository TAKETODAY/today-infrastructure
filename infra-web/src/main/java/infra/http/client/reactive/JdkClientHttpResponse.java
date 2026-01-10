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
            HttpHeaders.copyOf(response.headers().map()).asReadOnly(), cookies, adaptBody(response, bufferFactory));
  }

  private static Flux<DataBuffer> adaptBody(HttpResponse<Flow.Publisher<List<ByteBuffer>>> response, DataBufferFactory bufferFactory) {
    Flow.Publisher<List<ByteBuffer>> body = response.body();
    if (body == null) {
      return Flux.empty();
    }
    return JdkFlowAdapter.flowPublisherToFlux(response.body())
            .flatMapIterable(Function.identity())
            .map(bufferFactory::wrap)
            .doOnDiscard(DataBuffer.class, DataBuffer.RELEASE_CONSUMER)
            .cache(0);
  }

}

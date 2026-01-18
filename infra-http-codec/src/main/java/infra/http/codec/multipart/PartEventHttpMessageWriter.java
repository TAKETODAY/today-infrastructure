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

package infra.http.codec.multipart;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.reactive.ReactiveHttpOutputMessage;
import infra.http.codec.HttpMessageWriter;
import infra.lang.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link HttpMessageWriter} for writing {@link PartEvent} objects. Useful for
 * server-side proxies, that relay multipart requests to others services.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PartEvent
 * @since 4.0 2022/4/22 9:11
 */
public class PartEventHttpMessageWriter extends MultipartWriterSupport implements HttpMessageWriter<PartEvent> {

  public PartEventHttpMessageWriter() {
    super(Collections.singletonList(MediaType.MULTIPART_FORM_DATA));
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    if (PartEvent.class.isAssignableFrom(elementType.toClass())) {
      if (mediaType == null) {
        return true;
      }
      for (MediaType supportedMediaType : getWritableMediaTypes()) {
        if (supportedMediaType.isCompatibleWith(mediaType)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Mono<Void> write(Publisher<? extends PartEvent> partDataStream, ResolvableType elementType,
          @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
          Map<String, Object> hints) {

    byte[] boundary = generateMultipartBoundary();

    mediaType = getMultipartMediaType(mediaType, boundary);
    outputMessage.getHeaders().setContentType(mediaType);

    if (logger.isDebugEnabled()) {
      logger.debug(Hints.getLogPrefix(hints) + "Encoding Publisher<PartEvent>");
    }

    Flux<DataBuffer> body = Flux.from(partDataStream)
            .windowUntil(PartEvent::isLast)
            .concatMap(partData -> partData.switchOnFirst((signal, flux) -> {
              if (signal.hasValue()) {
                PartEvent value = signal.get();
                Assert.state(value != null, "Null value");
                Flux<DataBuffer> dataBuffers = flux.map(PartEvent::content)
                        .filter(buffer -> buffer.readableBytes() > 0);
                return encodePartData(boundary, outputMessage.bufferFactory(), value.headers(), dataBuffers);
              }
              else {
                return flux.cast(DataBuffer.class);
              }
            }))
            .concatWith(generateLastLine(boundary, outputMessage.bufferFactory()))
            .doOnDiscard(DataBuffer.class, DataBuffer.RELEASE_CONSUMER);

    if (logger.isDebugEnabled()) {
      body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
    }

    return outputMessage.writeWith(body);
  }

  private Flux<DataBuffer> encodePartData(byte[] boundary,
          DataBufferFactory bufferFactory, HttpHeaders headers, Flux<DataBuffer> body) {
    return Flux.concat(
            generateBoundaryLine(boundary, bufferFactory),
            generatePartHeaders(headers, bufferFactory),
            body,
            generateNewLine(bufferFactory));
  }

}

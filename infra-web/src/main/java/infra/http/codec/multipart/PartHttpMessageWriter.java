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

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.ReactiveHttpOutputMessage;
import infra.http.codec.HttpMessageWriter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link HttpMessageWriter} for writing with {@link Part}. This can be useful
 * on the server side to write a {@code Flux<Part>} received from a client to
 * some remote service.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PartHttpMessageWriter extends MultipartWriterSupport implements HttpMessageWriter<Part> {

  public PartHttpMessageWriter() {
    super(MultipartHttpMessageReader.MIME_TYPES);
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    if (Part.class.isAssignableFrom(elementType.toClass())) {
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
  public Mono<Void> write(Publisher<? extends Part> parts, ResolvableType elementType,
          @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

    byte[] boundary = generateMultipartBoundary();

    mediaType = getMultipartMediaType(mediaType, boundary);
    outputMessage.getHeaders().setContentType(mediaType);

    if (logger.isDebugEnabled()) {
      logger.debug("{} Encoding Publisher<Part>", Hints.getLogPrefix(hints));
    }

    Flux<DataBuffer> body = Flux.from(parts)
            .concatMap(part -> encodePart(boundary, part, outputMessage.bufferFactory()))
            .concatWith(generateLastLine(boundary, outputMessage.bufferFactory()))
            .doOnDiscard(DataBuffer.class, DataBuffer.RELEASE_CONSUMER);

    if (logger.isDebugEnabled()) {
      body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
    }

    return outputMessage.writeWith(body);
  }

  private Flux<DataBuffer> encodePart(byte[] boundary, Part part, DataBufferFactory bufferFactory) {
    DefaultHttpHeaders headers = new DefaultHttpHeaders(part.headers());
    String name = part.name();
    if (!headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
      headers.setContentDispositionFormData(
              name, part instanceof FilePart ? ((FilePart) part).filename() : null);
    }

    return Flux.concat(
            generateBoundaryLine(boundary, bufferFactory),
            generatePartHeaders(headers, bufferFactory),
            part.content(),
            generateNewLine(bufferFactory)
    );
  }

}

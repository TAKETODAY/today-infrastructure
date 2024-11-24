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

package infra.http.codec.multipart;

import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.ReactiveHttpOutputMessage;
import infra.http.codec.HttpMessageWriter;
import infra.lang.Assert;
import infra.lang.Nullable;
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
                        .filter(buffer -> buffer.readableByteCount() > 0);
                return encodePartData(boundary, outputMessage.bufferFactory(), value.headers(), dataBuffers);
              }
              else {
                return flux.cast(DataBuffer.class);
              }
            }))
            .concatWith(generateLastLine(boundary, outputMessage.bufferFactory()))
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release);

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

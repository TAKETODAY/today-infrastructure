/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.codec.multipart;

import org.reactivestreams.Publisher;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link HttpMessageWriter} for writing with {@link Part}. This can be useful
 * on the server side to write a {@code Flux<Part>} received from a client to
 * some remote service.
 *
 * @author Rossen Stoyanchev
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
  public Mono<Void> write(
          Publisher<? extends Part> parts, ResolvableType elementType, @Nullable MediaType mediaType,
          ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

    byte[] boundary = generateMultipartBoundary();

    mediaType = getMultipartMediaType(mediaType, boundary);
    outputMessage.getHeaders().setContentType(mediaType);

    if (logger.isDebugEnabled()) {
      logger.debug("{} Encoding Publisher<Part>", Hints.getLogPrefix(hints));
    }

    Flux<DataBuffer> body = Flux.from(parts)
            .concatMap(part -> encodePart(boundary, part, outputMessage.bufferFactory()))
            .concatWith(generateLastLine(boundary, outputMessage.bufferFactory()))
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release);

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

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

package cn.taketoday.http.codec;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.codec.ResourceDecoder;
import cn.taketoday.core.codec.ResourceEncoder;
import cn.taketoday.core.codec.ResourceRegionEncoder;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceRegion;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaTypeFactory;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HttpMessageWriter} that can write a {@link Resource}.
 *
 * <p>Also an implementation of {@code HttpMessageWriter} with support for writing one
 * or more {@link ResourceRegion}'s based on the HTTP ranges specified in the request.
 *
 * <p>For reading to a Resource, use {@link ResourceDecoder} wrapped with
 * {@link DecoderHttpMessageReader}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see ResourceEncoder
 * @see ResourceRegionEncoder
 * @see HttpRange
 * @since 4.0
 */
public class ResourceHttpMessageWriter implements HttpMessageWriter<Resource> {
  private static final Logger logger = HttpLogging.forLogName(ResourceHttpMessageWriter.class);
  private static final ResolvableType REGION_TYPE = ResolvableType.fromClass(ResourceRegion.class);

  private final ResourceEncoder encoder;
  private final List<MediaType> mediaTypes;
  private final ResourceRegionEncoder regionEncoder;

  public ResourceHttpMessageWriter() {
    this(ResourceEncoder.DEFAULT_BUFFER_SIZE);
  }

  public ResourceHttpMessageWriter(int bufferSize) {
    this.encoder = new ResourceEncoder(bufferSize);
    this.regionEncoder = new ResourceRegionEncoder(bufferSize);
    this.mediaTypes = MediaType.asMediaTypes(this.encoder.getEncodableMimeTypes());
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    return this.encoder.canEncode(elementType, mediaType);
  }

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return this.mediaTypes;
  }

  // Client or server: single Resource...

  @Override
  public Mono<Void> write(
          Publisher<? extends Resource> inputStream, ResolvableType elementType,
          @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    return Mono.from(inputStream)
            .flatMap(resource -> writeResource(resource, elementType, mediaType, message, hints));
  }

  private Mono<Void> writeResource(
          Resource resource, ResolvableType type, @Nullable MediaType mediaType,
          ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    HttpHeaders headers = message.getHeaders();
    MediaType resourceMediaType = getResourceMediaType(mediaType, resource, hints);
    headers.setContentType(resourceMediaType);

    if (headers.getContentLength() < 0) {
      long length = lengthOf(resource);
      if (length != -1) {
        headers.setContentLength(length);
      }
    }

    return zeroCopy(resource, null, message, hints)
            .orElseGet(() -> {
              Mono<Resource> input = Mono.just(resource);
              DataBufferFactory factory = message.bufferFactory();
              Flux<DataBuffer> body = this.encoder.encode(input, factory, type, resourceMediaType, hints);
              if (logger.isDebugEnabled()) {
                body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
              }
              return message.writeWith(body);
            });
  }

  private static MediaType getResourceMediaType(
          @Nullable MediaType mediaType, Resource resource, Map<String, Object> hints) {
    if (mediaType != null && mediaType.isConcrete() && !mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)) {
      return mediaType;
    }
    mediaType = MediaTypeFactory.getMediaType(resource)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      logger.debug("{}Resource associated with '{}'", Hints.getLogPrefix(hints), mediaType);
    }
    return mediaType;
  }

  private static long lengthOf(Resource resource) {
    // Don't consume InputStream...
    if (InputStreamResource.class != resource.getClass()) {
      try {
        return resource.contentLength();
      }
      catch (IOException ignored) { }
    }
    return -1;
  }

  private static Optional<Mono<Void>> zeroCopy(
          Resource resource, @Nullable ResourceRegion region,
          ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    if (message instanceof ZeroCopyHttpOutputMessage && resource instanceof FileBasedResource fileResource) {
      try {
        Path path = fileResource.getPath();
        long size = Files.size(path);
        long pos = region != null ? region.getPosition() : 0;
        long count = region != null ? region.getCount() : size;
        if (logger.isDebugEnabled()) {
          String formatted = region != null ? "region " + pos + "-" + (count) + " of " : "";
          logger.debug("{}Zero-copy {}[{}]", Hints.getLogPrefix(hints), formatted, resource);
        }
        return Optional.of(((ZeroCopyHttpOutputMessage) message).writeWith(path, pos, count));
      }
      catch (IOException ex) {
        // should not happen
      }
    }
    return Optional.empty();
  }

  // Server-side only: single Resource or sub-regions...

  @Override
  public Mono<Void> write(
          Publisher<? extends Resource> inputStream, @Nullable ResolvableType actualType,
          ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
          ServerHttpResponse response, Map<String, Object> hints) {

    HttpHeaders headers = response.getHeaders();
    headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

    List<HttpRange> ranges;
    try {
      ranges = request.getHeaders().getRange();
    }
    catch (IllegalArgumentException ex) {
      response.setStatusCode(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
      return response.setComplete();
    }

    return Mono.from(inputStream).flatMap(resource -> {
      if (ranges.isEmpty()) {
        return writeResource(resource, elementType, mediaType, response, hints);
      }
      response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
      List<ResourceRegion> regions = HttpRange.toResourceRegions(ranges, resource);
      MediaType resourceMediaType = getResourceMediaType(mediaType, resource, hints);
      if (regions.size() == 1) {
        ResourceRegion region = regions.get(0);
        headers.setContentType(resourceMediaType);
        long contentLength = lengthOf(resource);
        if (contentLength != -1) {
          long start = region.getPosition();
          long end = start + region.getCount() - 1;
          end = Math.min(end, contentLength - 1);
          headers.add("Content-Range", "bytes " + start + '-' + end + '/' + contentLength);
          headers.setContentLength(end - start + 1);
        }
        return writeSingleRegion(region, response, hints);
      }
      else {
        String boundary = MimeTypeUtils.generateMultipartBoundaryString();
        MediaType multipartType = MediaType.parseMediaType("multipart/byteranges;boundary=" + boundary);
        headers.setContentType(multipartType);
        Map<String, Object> allHints = Hints.merge(hints, ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary);
        return encodeAndWriteRegions(Flux.fromIterable(regions), resourceMediaType, response, allHints);
      }
    });
  }

  private Mono<Void> writeSingleRegion(
          ResourceRegion region, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    return zeroCopy(region.getResource(), region, message, hints)
            .orElseGet(() -> {
              Publisher<? extends ResourceRegion> input = Mono.just(region);
              MediaType mediaType = message.getHeaders().getContentType();
              return encodeAndWriteRegions(input, mediaType, message, hints);
            });
  }

  private Mono<Void> encodeAndWriteRegions(
          Publisher<? extends ResourceRegion> publisher,
          @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    Flux<DataBuffer> body = this.regionEncoder.encode(
            publisher, message.bufferFactory(), REGION_TYPE, mediaType, hints);

    return message.writeWith(body);
  }

}

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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.codec.LoggingCodecSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

/**
 * Support class for multipart HTTP message writers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MultipartWriterSupport extends LoggingCodecSupport {

  /** THe default charset used by the writer. */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final List<MediaType> supportedMediaTypes;

  private Charset charset = DEFAULT_CHARSET;

  /**
   * Constructor with the list of supported media types.
   */
  protected MultipartWriterSupport(List<MediaType> supportedMediaTypes) {
    this.supportedMediaTypes = supportedMediaTypes;
  }

  /**
   * Return the configured charset for part headers.
   */
  public Charset getCharset() {
    return this.charset;
  }

  /**
   * Set the character set to use for part headers such as
   * "Content-Disposition" (and its filename parameter).
   * <p>By default this is set to "UTF-8". If changed from this default,
   * the "Content-Type" header will have a "charset" parameter that specifies
   * the character set used.
   */
  public void setCharset(Charset charset) {
    Assert.notNull(charset, "Charset must not be null");
    this.charset = charset;
  }

  public List<MediaType> getWritableMediaTypes() {
    return this.supportedMediaTypes;
  }

  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    if (MultiValueMap.class.isAssignableFrom(elementType.toClass())) {
      if (mediaType == null) {
        return true;
      }
      for (MediaType supportedMediaType : this.supportedMediaTypes) {
        if (supportedMediaType.isCompatibleWith(mediaType)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Generate a multipart boundary.
   * <p>By default delegates to {@link MimeTypeUtils#generateMultipartBoundary()}.
   */
  protected byte[] generateMultipartBoundary() {
    return MimeTypeUtils.generateMultipartBoundary();
  }

  /**
   * Prepare the {@code MediaType} to use by adding "boundary" and "charset"
   * parameters to the given {@code mediaType} or "mulitpart/form-data"
   * otherwise by default.
   */
  protected MediaType getMultipartMediaType(@Nullable MediaType mediaType, byte[] boundary) {
    Map<String, String> params = new HashMap<>();
    if (mediaType != null) {
      params.putAll(mediaType.getParameters());
    }
    params.put("boundary", new String(boundary, StandardCharsets.US_ASCII));
    Charset charset = getCharset();
    if (!charset.equals(StandardCharsets.UTF_8) &&
            !charset.equals(StandardCharsets.US_ASCII)) {
      params.put("charset", charset.name());
    }

    mediaType = (mediaType != null ? mediaType : MediaType.MULTIPART_FORM_DATA);
    mediaType = new MediaType(mediaType, params);
    return mediaType;
  }

  protected Mono<DataBuffer> generateBoundaryLine(byte[] boundary, DataBufferFactory bufferFactory) {
    return Mono.fromCallable(() -> {
      DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 4);
      buffer.write((byte) '-');
      buffer.write((byte) '-');
      buffer.write(boundary);
      buffer.write((byte) '\r');
      buffer.write((byte) '\n');
      return buffer;
    });
  }

  protected Mono<DataBuffer> generateNewLine(DataBufferFactory bufferFactory) {
    return Mono.fromCallable(() -> {
      DataBuffer buffer = bufferFactory.allocateBuffer(2);
      buffer.write((byte) '\r');
      buffer.write((byte) '\n');
      return buffer;
    });
  }

  protected Mono<DataBuffer> generateLastLine(byte[] boundary, DataBufferFactory bufferFactory) {
    return Mono.fromCallable(() -> {
      DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 6);
      buffer.write((byte) '-');
      buffer.write((byte) '-');
      buffer.write(boundary);
      buffer.write((byte) '-');
      buffer.write((byte) '-');
      buffer.write((byte) '\r');
      buffer.write((byte) '\n');
      return buffer;
    });
  }

  protected Mono<DataBuffer> generatePartHeaders(HttpHeaders headers, DataBufferFactory bufferFactory) {
    return Mono.fromCallable(() -> {
      DataBuffer buffer = bufferFactory.allocateBuffer();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        byte[] headerName = entry.getKey().getBytes(getCharset());
        for (String headerValueString : entry.getValue()) {
          byte[] headerValue = headerValueString.getBytes(getCharset());
          buffer.write(headerName);
          buffer.write((byte) ':');
          buffer.write((byte) ' ');
          buffer.write(headerValue);
          buffer.write((byte) '\r');
          buffer.write((byte) '\n');
        }
      }
      buffer.write((byte) '\r');
      buffer.write((byte) '\n');
      return buffer;
    });
  }

}

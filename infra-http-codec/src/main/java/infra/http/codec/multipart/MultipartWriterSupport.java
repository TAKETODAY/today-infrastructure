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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.codec.LoggingCodecSupport;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.FastByteArrayOutputStream;
import infra.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

/**
 * Support class for multipart HTTP message writers.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MultipartWriterSupport extends LoggingCodecSupport {

  private final List<MediaType> supportedMediaTypes;

  private Charset charset = Constant.DEFAULT_CHARSET;

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
    Assert.notNull(charset, "Charset is required");
    this.charset = charset;
  }

  public List<MediaType> getWritableMediaTypes() {
    return this.supportedMediaTypes;
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
    HashMap<String, String> params = new HashMap<>();
    if (mediaType != null) {
      params.putAll(mediaType.getParameters());
    }
    params.put("boundary", new String(boundary, StandardCharsets.US_ASCII));
    Charset charset = getCharset();
    if (!charset.equals(StandardCharsets.UTF_8)
            && !charset.equals(StandardCharsets.US_ASCII)) {
      params.put("charset", charset.name());
    }

    mediaType = mediaType != null ? mediaType : MediaType.MULTIPART_FORM_DATA;
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
      @SuppressWarnings("resource")
      FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        byte[] headerName = entry.getKey().getBytes(getCharset());
        for (String headerValueString : entry.getValue()) {
          byte[] headerValue = headerValueString.getBytes(getCharset());
          bos.write(headerName);
          bos.write((byte) ':');
          bos.write((byte) ' ');
          bos.write(headerValue);
          bos.write((byte) '\r');
          bos.write((byte) '\n');
        }
      }
      bos.write((byte) '\r');
      bos.write((byte) '\n');

      byte[] bytes = bos.toByteArrayUnsafe();
      return bufferFactory.wrap(bytes);
    });
  }

}

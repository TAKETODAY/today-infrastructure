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

package infra.core.codec;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DataBufferUtils;
import infra.lang.Assert;
import infra.util.MimeType;
import infra.util.StreamUtils;
import reactor.core.publisher.Flux;

/**
 * Encoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ResourceEncoder extends AbstractSingleValueEncoder<Resource> {

  /**
   * The default buffer size used by the encoder.
   */
  public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

  private final int bufferSize;

  public ResourceEncoder() {
    this(DEFAULT_BUFFER_SIZE);
  }

  public ResourceEncoder(int bufferSize) {
    super(MimeType.APPLICATION_OCTET_STREAM, MimeType.ALL);
    Assert.isTrue(bufferSize > 0, "'bufferSize' must be larger than 0");
    this.bufferSize = bufferSize;
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    Class<?> clazz = elementType.toClass();
    return super.canEncode(elementType, mimeType)
            && Resource.class.isAssignableFrom(clazz);
  }

  @Override
  protected Flux<DataBuffer> encode(
          Resource resource, DataBufferFactory bufferFactory,
          ResolvableType type, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
      logger.debug("{}Writing [{}]", Hints.getLogPrefix(hints), resource);
    }
    return DataBufferUtils.read(resource, bufferFactory, this.bufferSize);
  }

}

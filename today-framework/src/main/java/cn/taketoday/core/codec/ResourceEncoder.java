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

package cn.taketoday.core.codec;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.StreamUtils;
import reactor.core.publisher.Flux;

/**
 * Encoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
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
    super(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL);
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

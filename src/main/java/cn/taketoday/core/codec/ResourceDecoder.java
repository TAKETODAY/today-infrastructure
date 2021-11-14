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

import org.reactivestreams.Publisher;

import java.io.ByteArrayInputStream;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Decoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ResourceDecoder extends AbstractDataBufferDecoder<Resource> {

  /** Name of hint with a filename for the resource(e.g. from "Content-Disposition" HTTP header). */
  public static String FILENAME_HINT = ResourceDecoder.class.getName() + ".filename";

  public ResourceDecoder() {
    super(MimeTypeUtils.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return Resource.class.isAssignableFrom(elementType.toClass())
            && super.canDecode(elementType, mimeType);
  }

  @Override
  public Flux<Resource> decode(
          Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    return Flux.from(decodeToMono(inputStream, elementType, mimeType, hints));
  }

  @Override
  public Resource decode(DataBuffer dataBuffer, ResolvableType elementType,
                         @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    byte[] bytes = new byte[dataBuffer.readableByteCount()];
    dataBuffer.read(bytes);
    DataBufferUtils.release(dataBuffer);

    if (logger.isDebugEnabled()) {
      logger.debug("{}Read {} bytes", Hints.getLogPrefix(hints), bytes.length);
    }

    Class<?> clazz = elementType.toClass();
    String filename = hints != null ? (String) hints.get(FILENAME_HINT) : null;
    if (clazz == InputStreamResource.class) {
      return new InputStreamResource(new ByteArrayInputStream(bytes)) {
        @Override
        public String getName() {
          return filename;
        }

        @Override
        public long contentLength() {
          return bytes.length;
        }
      };
    }
    else if (Resource.class.isAssignableFrom(clazz)) {
      return new ByteArrayResource(bytes) {
        @Override
        public String getName() {
          return filename;
        }
      };
    }
    else {
      throw new IllegalStateException("Unsupported resource class: " + clazz);
    }
  }

}

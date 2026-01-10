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

package infra.core.codec;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.io.ByteArrayInputStream;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.ByteArrayResource;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Decoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ResourceDecoder extends AbstractDataBufferDecoder<Resource> {

  /** Name of hint with a filename for the resource(e.g. from "Content-Disposition" HTTP header). */
  public static String FILENAME_HINT = ResourceDecoder.class.getName() + ".filename";

  public ResourceDecoder() {
    super(MimeType.ALL);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return Resource.class.isAssignableFrom(elementType.toClass())
            && super.canDecode(elementType, mimeType);
  }

  @Override
  public Flux<Resource> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    return Flux.from(decodeToMono(inputStream, elementType, mimeType, hints));
  }

  @Override
  public Resource decode(DataBuffer dataBuffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    byte[] bytes = new byte[dataBuffer.readableBytes()];
    dataBuffer.read(bytes);
    dataBuffer.release();

    if (logger.isDebugEnabled()) {
      logger.debug("{}Read {} bytes", Hints.getLogPrefix(hints), bytes.length);
    }

    Class<?> clazz = elementType.toClass();
    String filename = hints != null ? (String) hints.get(FILENAME_HINT) : null;
    if (clazz == InputStreamResource.class) {
      return new InputStreamResource(new ByteArrayInputStream(bytes)) {
        @Nullable
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
        @Nullable
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

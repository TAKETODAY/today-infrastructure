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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.MimeType;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for {@link Decoder} implementations.
 *
 * @param <T> the element type
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractDecoder<T> implements Decoder<T> {

  private final List<MimeType> decodableMimeTypes;

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected AbstractDecoder(MimeType... supportedMimeTypes) {
    this.decodableMimeTypes = Arrays.asList(supportedMimeTypes);
  }

  /**
   * Set an alternative logger to use than the one based on the class name.
   *
   * @param logger the logger to use
   */
  public void setLogger(Logger logger) {
    this.logger = logger;
  }

  /**
   * Return the currently configured Logger.
   */
  public Logger getLogger() {
    return logger;
  }

  @Override
  public List<MimeType> getDecodableMimeTypes() {
    return this.decodableMimeTypes;
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    if (mimeType == null) {
      return true;
    }
    for (MimeType candidate : this.decodableMimeTypes) {
      if (candidate.isCompatibleWith(mimeType)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    throw new UnsupportedOperationException();
  }

}

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

package infra.http.codec.support;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.core.codec.Decoder;
import infra.http.codec.ClientCodecConfigurer;
import infra.http.codec.HttpMessageReader;
import infra.http.codec.ServerSentEventHttpMessageReader;

/**
 * Default implementation of {@link ClientCodecConfigurer.ClientDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {

  private @Nullable Decoder<?> sseDecoder;

  ClientDefaultCodecsImpl() {
  }

  ClientDefaultCodecsImpl(ClientDefaultCodecsImpl other) {
    super(other);
    this.sseDecoder = other.sseDecoder;
  }

  @Override
  public void serverSentEventDecoder(Decoder<?> decoder) {
    this.sseDecoder = decoder;
    initObjectReaders();
  }

  @Override
  protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {
    Decoder<?> decoder = (this.sseDecoder != null ? this.sseDecoder : JACKSON_PRESENT ? getJacksonJsonDecoder() : null);
    addCodec(objectReaders, new ServerSentEventHttpMessageReader(decoder));
  }

}

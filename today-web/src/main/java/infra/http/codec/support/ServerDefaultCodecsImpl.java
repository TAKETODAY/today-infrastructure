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

import infra.core.codec.Encoder;
import infra.http.codec.HttpMessageWriter;
import infra.http.codec.ServerCodecConfigurer;
import infra.http.codec.ServerSentEventHttpMessageWriter;

/**
 * Default implementation of {@link ServerCodecConfigurer.ServerDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ServerDefaultCodecsImpl extends BaseDefaultCodecs implements ServerCodecConfigurer.ServerDefaultCodecs {

  private @Nullable Encoder<?> sseEncoder;

  ServerDefaultCodecsImpl() {
  }

  ServerDefaultCodecsImpl(ServerDefaultCodecsImpl other) {
    super(other);
    this.sseEncoder = other.sseEncoder;
  }

  @Override
  public void serverSentEventEncoder(Encoder<?> encoder) {
    this.sseEncoder = encoder;
    initObjectWriters();
  }

  @Override
  protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
    objectWriters.add(new ServerSentEventHttpMessageWriter(getSseEncoder()));
  }

  private @Nullable Encoder<?> getSseEncoder() {
    return this.sseEncoder != null ? this.sseEncoder : JACKSON_PRESENT ? getJacksonJsonEncoder() : null;
  }

}

/*
 * Copyright 2017 - 2026 the original author or authors.
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

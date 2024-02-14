/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.codec.support;

import java.util.List;

import cn.taketoday.core.codec.Decoder;
import cn.taketoday.http.codec.ClientCodecConfigurer;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.ServerSentEventHttpMessageReader;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of {@link ClientCodecConfigurer.ClientDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {

  @Nullable
  private Decoder<?> sseDecoder;

  ClientDefaultCodecsImpl() { }

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

    Decoder<?> decoder = this.sseDecoder != null
                         ? this.sseDecoder
                         : jackson2Present
                           ? getJackson2JsonDecoder()
                           : null;

    addCodec(objectReaders, new ServerSentEventHttpMessageReader(decoder));
  }

}

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

package cn.taketoday.http.codec.support;

import java.util.List;

import cn.taketoday.core.codec.Encoder;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.codec.ServerCodecConfigurer;
import cn.taketoday.http.codec.ServerSentEventHttpMessageWriter;
import cn.taketoday.http.codec.multipart.DefaultPartHttpMessageReader;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageReader;
import cn.taketoday.http.codec.multipart.PartEventHttpMessageReader;
import cn.taketoday.http.codec.multipart.PartHttpMessageWriter;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of {@link ServerCodecConfigurer.ServerDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 */
class ServerDefaultCodecsImpl extends BaseDefaultCodecs implements ServerCodecConfigurer.ServerDefaultCodecs {

  @Nullable
  private HttpMessageReader<?> multipartReader;

  @Nullable
  private Encoder<?> sseEncoder;

  ServerDefaultCodecsImpl() { }

  ServerDefaultCodecsImpl(ServerDefaultCodecsImpl other) {
    super(other);
    this.multipartReader = other.multipartReader;
    this.sseEncoder = other.sseEncoder;
  }

  @Override
  public void multipartReader(HttpMessageReader<?> reader) {
    this.multipartReader = reader;
    initTypedReaders();
  }

  @Override
  public void serverSentEventEncoder(Encoder<?> encoder) {
    this.sseEncoder = encoder;
    initObjectWriters();
  }

  @Override
  protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) {
    if (this.multipartReader != null) {
      addCodec(typedReaders, this.multipartReader);
    }
    else {
      DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
      addCodec(typedReaders, partReader);
      addCodec(typedReaders, new MultipartHttpMessageReader(partReader));
    }
    addCodec(typedReaders, new PartEventHttpMessageReader());
  }

  @Override
  protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
    addCodec(typedWriters, new PartHttpMessageWriter());
  }

  @Override
  protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
    objectWriters.add(new ServerSentEventHttpMessageWriter(getSseEncoder()));
  }

  @Nullable
  private Encoder<?> getSseEncoder() {
    return this.sseEncoder != null
           ? this.sseEncoder
           : jackson2Present
             ? getJackson2JsonEncoder()
             : null;
  }

}

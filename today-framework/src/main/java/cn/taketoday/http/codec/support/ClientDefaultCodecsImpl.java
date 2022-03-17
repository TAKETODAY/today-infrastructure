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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.http.codec.ClientCodecConfigurer;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.FormHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.codec.ServerSentEventHttpMessageReader;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageWriter;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of {@link ClientCodecConfigurer.ClientDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {

  @Nullable
  private DefaultMultipartCodecs multipartCodecs;

  @Nullable
  private Decoder<?> sseDecoder;

  @Nullable
  private Supplier<List<HttpMessageWriter<?>>> partWritersSupplier;

  ClientDefaultCodecsImpl() { }

  ClientDefaultCodecsImpl(ClientDefaultCodecsImpl other) {
    super(other);
    this.multipartCodecs = other.multipartCodecs != null
                           ? new DefaultMultipartCodecs(other.multipartCodecs) : null;
    this.sseDecoder = other.sseDecoder;
  }

  /**
   * Set a supplier for part writers to use when
   * {@link #multipartCodecs()} are not explicitly configured.
   * That's the same set of writers as for general except for the multipart
   * writer itself.
   */
  void setPartWritersSupplier(Supplier<List<HttpMessageWriter<?>>> supplier) {
    this.partWritersSupplier = supplier;
    initTypedWriters();
  }

  @Override
  public ClientCodecConfigurer.MultipartCodecs multipartCodecs() {
    if (this.multipartCodecs == null) {
      this.multipartCodecs = new DefaultMultipartCodecs();
    }
    return this.multipartCodecs;
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

  @Override
  protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
    addCodec(typedWriters, new MultipartHttpMessageWriter(getPartWriters(), new FormHttpMessageWriter()));
  }

  private List<HttpMessageWriter<?>> getPartWriters() {
    if (this.multipartCodecs != null) {
      return this.multipartCodecs.getWriters();
    }
    else if (this.partWritersSupplier != null) {
      return this.partWritersSupplier.get();
    }
    else {
      return Collections.emptyList();
    }
  }

  /**
   * Default implementation of {@link ClientCodecConfigurer.MultipartCodecs}.
   */
  private class DefaultMultipartCodecs implements ClientCodecConfigurer.MultipartCodecs {

    private final ArrayList<HttpMessageWriter<?>> writers = new ArrayList<>();

    DefaultMultipartCodecs() { }

    DefaultMultipartCodecs(DefaultMultipartCodecs other) {
      this.writers.addAll(other.writers);
    }

    @Override
    public ClientCodecConfigurer.MultipartCodecs encoder(Encoder<?> encoder) {
      writer(new EncoderHttpMessageWriter<>(encoder));
      initTypedWriters();
      return this;
    }

    @Override
    public ClientCodecConfigurer.MultipartCodecs writer(HttpMessageWriter<?> writer) {
      this.writers.add(writer);
      initTypedWriters();
      return this;
    }

    List<HttpMessageWriter<?>> getWriters() {
      return this.writers;
    }
  }

}

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.server.reactive;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.codec.json.LeakAwareDataBufferFactory;
import io.netty.buffer.PooledByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpHeadResponseDecorator}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpHeadResponseDecoratorTests {

  private final LeakAwareDataBufferFactory bufferFactory =
          new LeakAwareDataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT));

  private final ServerHttpResponse response =
          new HttpHeadResponseDecorator(new MockServerHttpResponse(this.bufferFactory));

  @AfterEach
  public void tearDown() {
    this.bufferFactory.checkForLeaks();
  }

  @Test
  public void writeWithFlux() {
    Flux<DataBuffer> body = Flux.just(toDataBuffer("data1"), toDataBuffer("data2"));
    this.response.writeWith(body).block();
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(-1);
  }

  @Test
  public void writeWithMono() {
    Mono<DataBuffer> body = Mono.just(toDataBuffer("data1,data2"));
    this.response.writeWith(body).block();
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(11);
  }

  @Test // gh-23484
  public void writeWithGivenContentLength() {
    int length = 15;
    this.response.getHeaders().setContentLength(length);
    this.response.writeWith(Flux.empty()).block();
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(length);
  }

  @Test // gh-25908
  public void writeWithGivenTransferEncoding() {
    Flux<DataBuffer> body = Flux.just(toDataBuffer("data1"), toDataBuffer("data2"));
    this.response.getHeaders().add(HttpHeaders.TRANSFER_ENCODING, "chunked");
    this.response.writeWith(body).block();
    assertThat(this.response.getHeaders().getContentLength()).isEqualTo(-1);
  }

  private DataBuffer toDataBuffer(String s) {
    DataBuffer buffer = this.bufferFactory.allocateBuffer();
    buffer.write(s.getBytes(StandardCharsets.UTF_8));
    return buffer;
  }

}

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

package cn.taketoday.web.reactive.function.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.web.testfixture.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.web.testfixture.http.client.reactive.MockClientHttpResponse;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class DefaultClientResponseBuilderTests {

  @Test
  public void normal() {
    Flux<DataBuffer> body = Flux.just("baz")
            .map(s -> s.getBytes(StandardCharsets.UTF_8))
            .map(DefaultDataBufferFactory.sharedInstance::wrap);

    ClientResponse response = ClientResponse.create(HttpStatus.BAD_GATEWAY, ExchangeStrategies.withDefaults())
            .header("foo", "bar")
            .cookie("baz", "qux")
            .body(body)
            .build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    HttpHeaders responseHeaders = response.headers().asHttpHeaders();
    assertThat(responseHeaders.getFirst("foo")).isEqualTo("bar");
    assertThat(response.cookies().getFirst("baz")).as("qux").isNotNull();
    assertThat(response.cookies().getFirst("baz").getValue()).isEqualTo("qux");

    StepVerifier.create(response.bodyToFlux(String.class))
            .expectNext("baz")
            .verifyComplete();
  }

  @Test
  public void mutate() {
    Flux<DataBuffer> otherBody = Flux.just("foo", "bar")
            .map(s -> s.getBytes(StandardCharsets.UTF_8))
            .map(DefaultDataBufferFactory.sharedInstance::wrap);

    HttpRequest mockClientHttpRequest = new MockClientHttpRequest(HttpMethod.GET, "/path");

    MockClientHttpResponse httpResponse = new MockClientHttpResponse(HttpStatus.OK);
    httpResponse.getHeaders().add("foo", "bar");
    httpResponse.getHeaders().add("bar", "baz");
    httpResponse.getCookies().add("baz", ResponseCookie.from("baz", "qux").build());
    httpResponse.setBody(otherBody);

    DefaultClientResponse otherResponse = new DefaultClientResponse(
            httpResponse, ExchangeStrategies.withDefaults(), "my-prefix", "", () -> mockClientHttpRequest);

    ClientResponse result = otherResponse.mutate()
            .statusCode(HttpStatus.BAD_REQUEST)
            .headers(headers -> headers.setOrRemove("foo", "baar"))
            .cookies(cookies -> cookies.setOrRemove("baz", ResponseCookie.from("baz", "quux").build()))
            .build();

    assertThat(result.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(result.headers().asHttpHeaders().size()).isEqualTo(3);
    assertThat(result.headers().asHttpHeaders().getFirst("foo")).isEqualTo("baar");
    assertThat(result.headers().asHttpHeaders().getFirst("bar")).isEqualTo("baz");
    assertThat(result.cookies().size()).isEqualTo(1);
    assertThat(result.cookies().getFirst("baz").getValue()).isEqualTo("quux");
    assertThat(result.logPrefix()).isEqualTo("my-prefix");

    StepVerifier.create(result.bodyToFlux(String.class))
            .expectNext("foobar")
            .verifyComplete();
  }

  @Test
  public void mutateWithCustomStatus() {
    ClientResponse other = ClientResponse.create(499, ExchangeStrategies.withDefaults()).build();
    ClientResponse result = other.mutate().build();

    assertThat(result.rawStatusCode()).isEqualTo(499);
    assertThat(result.statusCode()).isEqualTo(HttpStatusCode.valueOf(499));
  }
}

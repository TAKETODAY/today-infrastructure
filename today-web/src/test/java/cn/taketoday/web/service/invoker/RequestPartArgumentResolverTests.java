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

package cn.taketoday.web.service.invoker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 17:30
 */
class RequestPartArgumentResolverTests {

  private final TestHttpClientAdapter client = new TestHttpClientAdapter();

  private Service service;

  @BeforeEach
  void setUp() throws Exception {
    HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(this.client);
    this.service = proxyFactory.createClient(Service.class);
  }

  // Base class functionality should be tested in NamedValueArgumentResolverTests.
  // Form data vs query params tested in HttpRequestValuesTests.

  @Test
  void requestPart() {
    HttpHeaders headers = HttpHeaders.create();
    headers.add("foo", "bar");
    HttpEntity<String> part2 = new HttpEntity<>("part 2", headers);
    this.service.postMultipart("part 1", part2, Mono.just("part 3"));

    Object body = this.client.getRequestValues().getBodyValue();
    assertThat(body).isNotNull().isInstanceOf(MultiValueMap.class);
    MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) body;

    assertThat(map.getFirst("part1").getBody()).isEqualTo("part 1");
    assertThat(map.getFirst("part2")).isEqualTo(part2);
    assertThat(((Mono<?>) map.getFirst("part3").getBody()).block()).isEqualTo("part 3");
  }

  private interface Service {

    @PostExchange
    void postMultipart(@RequestPart String part1, @RequestPart HttpEntity<String> part2, @RequestPart Mono<String> part3);

  }

}
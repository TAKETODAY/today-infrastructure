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

import java.net.URI;

import cn.taketoday.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link UrlArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class UrlArgumentResolverTests {

  private final TestHttpClientAdapter client = new TestHttpClientAdapter();

  private Service service;

  @BeforeEach
  void setUp() throws Exception {
    HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(this.client);
    proxyFactory.afterPropertiesSet();
    this.service = proxyFactory.createClient(Service.class);
  }

  @Test
  void url() {
    URI dynamicUrl = URI.create("dynamic-path");
    this.service.execute(dynamicUrl);

    assertThat(getRequestValues().getUri()).isEqualTo(dynamicUrl);
    assertThat(getRequestValues().getUriTemplate()).isNull();
  }

  @Test
  void notUrl() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.service.executeNotUri("test"))
            .withMessage("Could not resolve parameter [0] in " +
                    "public abstract void cn.taketoday.web.service.invoker." +
                    "UrlArgumentResolverTests$Service.executeNotUri(java.lang.String): " +
                    "No suitable resolver");
  }

  @Test
  void ignoreNull() {
    this.service.execute(null);
    assertThat(getRequestValues().getUri()).isNull();
  }

  private HttpRequestValues getRequestValues() {
    return this.client.getRequestValues();
  }

  private interface Service {

    @GetExchange("/path")
    void execute(URI uri);

    @GetExchange
    void executeNotUri(String other);
  }

}

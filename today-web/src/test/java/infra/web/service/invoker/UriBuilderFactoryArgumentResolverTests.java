/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.service.invoker;

import org.junit.jupiter.api.Test;

import infra.lang.Nullable;
import infra.web.service.annotation.GetExchange;
import infra.web.util.DefaultUriBuilderFactory;
import infra.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/15 21:01
 */
class UriBuilderFactoryArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final Service service =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

  @Test
  void uriBuilderFactory() {
    UriBuilderFactory factory = new DefaultUriBuilderFactory("https://example.com");
    this.service.execute(factory);

    assertThat(getRequestValues().getUriBuilderFactory()).isEqualTo(factory);
    assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
    assertThat(getRequestValues().getURI()).isNull();
  }

  @Test
  void ignoreNullUriBuilderFactory() {
    this.service.execute(null);

    assertThat(getRequestValues().getUriBuilderFactory()).isEqualTo(null);
    assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
    assertThat(getRequestValues().getURI()).isNull();
  }

  private HttpRequestValues getRequestValues() {
    return this.client.getRequestValues();
  }

  private interface Service {

    @GetExchange("/path")
    void execute(@Nullable UriBuilderFactory uri);

  }
}
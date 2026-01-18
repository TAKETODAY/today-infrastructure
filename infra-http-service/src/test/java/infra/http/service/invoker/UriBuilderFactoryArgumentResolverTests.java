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

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.http.service.annotation.GetExchange;
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
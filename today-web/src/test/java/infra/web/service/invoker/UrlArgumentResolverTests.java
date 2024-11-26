/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.web.service.invoker;

import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.lang.Nullable;
import infra.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link UrlArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class UrlArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final Service service =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

  @Test
  void url() {
    URI dynamicUrl = URI.create("dynamic-path");
    this.service.execute(dynamicUrl);

    assertThat(getRequestValues().getUri()).isEqualTo(dynamicUrl);
    assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
  }

  @Test
  void notUrl() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.service.executeNotUri("test"))
            .withMessage("Could not resolve parameter [0] in " +
                    "public abstract void infra.web.service.invoker." +
                    "UrlArgumentResolverTests$Service.executeNotUri(java.lang.String): " +
                    "No suitable resolver");
  }

  @Test
  void ignoreNull() {
    this.service.execute(null);

    assertThat(getRequestValues().getUri()).isNull();
    assertThat(getRequestValues().getUriTemplate()).isEqualTo("/path");
  }

  private HttpRequestValues getRequestValues() {
    return this.client.getRequestValues();
  }

  private interface Service {

    @GetExchange("/path")
    void execute(@Nullable URI uri);

    @GetExchange
    void executeNotUri(String other);
  }

}

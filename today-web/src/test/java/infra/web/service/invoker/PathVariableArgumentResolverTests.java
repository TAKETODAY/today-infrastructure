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

import infra.lang.Nullable;
import infra.web.annotation.PathVariable;
import infra.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PathVariableArgumentResolver}.
 *
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
class PathVariableArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final Service service =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

  @Test
  void pathVariable() {
    this.service.execute("test");
    assertPathVariable("id", "test");
  }

  @SuppressWarnings("SameParameterValue")
  private void assertPathVariable(String name, @Nullable String expectedValue) {
    assertThat(this.client.getRequestValues().getUriVariables().get(name)).isEqualTo(expectedValue);
  }

  private interface Service {

    @GetExchange
    void execute(@PathVariable String id);

  }

}

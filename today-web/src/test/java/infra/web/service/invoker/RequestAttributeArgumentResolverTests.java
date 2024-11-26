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
import infra.web.annotation.RequestAttribute;
import infra.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestAttributeArgumentResolver}.
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
class RequestAttributeArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final Service service =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

  @Test
  void cookieValue() {
    this.service.execute("test");
    assertAttribute("attribute", "test");
  }

  @SuppressWarnings("SameParameterValue")
  private void assertAttribute(String name, @Nullable String expectedValue) {
    assertThat(this.client.getRequestValues().getAttributes().get(name)).isEqualTo(expectedValue);
  }

  private interface Service {

    @GetExchange
    void execute(@RequestAttribute String attribute);

  }

}

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

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PathVariableArgumentResolver}.
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
class PathVariableArgumentResolverTests {

  private final TestHttpClientAdapter client = new TestHttpClientAdapter();

  private Service service;

  @BeforeEach
  void setUp() throws Exception {
    HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(this.client);
    proxyFactory.afterPropertiesSet();
    this.service = proxyFactory.createClient(Service.class);
  }

  // Base class functionality should be tested in NamedValueArgumentResolverTests.

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

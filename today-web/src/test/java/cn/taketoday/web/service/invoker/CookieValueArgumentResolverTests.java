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

import java.util.List;

import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CookieValueArgumentResolver}.
 * <p>For base class functionality, see {@link NamedValueArgumentResolverTests}.
 *
 * @author Rossen Stoyanchev
 */
class CookieValueArgumentResolverTests {

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
  void cookieValue() {
    this.service.execute("test");
    assertCookie("cookie", "test");
  }

  private void assertCookie(String key, String... values) {
    List<String> actualValues = this.client.getRequestValues().getCookies().get(key);
    if (ObjectUtils.isEmpty(values)) {
      assertThat(actualValues).isNull();
    }
    else {
      assertThat(actualValues).containsOnly(values);
    }
  }

  private interface Service {

    @GetExchange
    void execute(@CookieValue String cookie);

  }

}

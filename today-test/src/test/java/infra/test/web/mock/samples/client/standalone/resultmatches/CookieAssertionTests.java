/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.stereotype.Controller;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.RequestMapping;
import infra.web.i18n.CookieLocaleResolver;
import infra.web.i18n.LocaleChangeInterceptor;

import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.CookieAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class CookieAssertionTests {

  private static final String COOKIE_NAME = CookieLocaleResolver.DEFAULT_COOKIE_NAME;

  private WebTestClient client;

  @BeforeEach
  public void setup() {
    CookieLocaleResolver localeResolver = new CookieLocaleResolver();
    localeResolver.setCookieDomain("domain");
    localeResolver.setCookieHttpOnly(true);
    localeResolver.setCookieSameSite("Strict");

    client = MockMvcWebTestClient.bindToController(new SimpleController())
            .interceptors(new LocaleChangeInterceptor())
            .localeResolver(localeResolver)
            .alwaysExpect(status().isOk())
            .configureClient()
            .baseUrl("/?locale=en_US")
            .build();
  }

  @Test
  public void testExists() {
    client.get().uri("/").exchange().expectCookie().exists(COOKIE_NAME);
  }

  @Test
  public void testNotExists() {
    client.get().uri("/").exchange().expectCookie().doesNotExist("unknownCookie");
  }

  @Test
  public void testEqualTo() {
    client.get().uri("/").exchange().expectCookie().valueEquals(COOKIE_NAME, "en-US");
    client.get().uri("/").exchange().expectCookie().value(COOKIE_NAME, equalTo("en-US"));
  }

  @Test
  public void testMatcher() {
    client.get().uri("/").exchange().expectCookie().value(COOKIE_NAME, startsWith("en-US"));
  }

  @Test
  public void testMaxAge() {
    client.get().uri("/").exchange().expectCookie().maxAge(COOKIE_NAME, Duration.ofSeconds(-1));
  }

  @Test
  public void testDomain() {
    client.get().uri("/").exchange().expectCookie().domain(COOKIE_NAME, "domain");
  }

  @Test
  public void testPath() {
    client.get().uri("/").exchange().expectCookie().path(COOKIE_NAME, "/");
  }

  @Test
  public void testSecured() {
    client.get().uri("/").exchange().expectCookie().secure(COOKIE_NAME, false);
  }

  @Test
  public void testHttpOnly() {
    client.get().uri("/").exchange().expectCookie().httpOnly(COOKIE_NAME, true);
  }

  @Test
  public void sameSite() {
    client.get().uri("/")
            .exchange()
            .expectCookie()
            .sameSite(COOKIE_NAME, "Strict");
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/")
    public String home() {
      return "home";
    }
  }

}

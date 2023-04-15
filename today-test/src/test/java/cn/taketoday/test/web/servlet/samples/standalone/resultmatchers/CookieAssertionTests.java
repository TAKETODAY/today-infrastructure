/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.http.ResponseCookie;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.i18n.CookieLocaleResolver;
import cn.taketoday.web.i18n.LocaleChangeInterceptor;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * Examples of expectations on response cookies values.
 *
 * @author Rossen Stoyanchev
 * @author Nikola Yovchev
 */
public class CookieAssertionTests {

  private static final String COOKIE_NAME = CookieLocaleResolver.DEFAULT_COOKIE_NAME;
  private static final String COOKIE_WITH_ATTRIBUTES_NAME = "SecondCookie";

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    CookieLocaleResolver localeResolver = new CookieLocaleResolver();
    localeResolver.setCookieDomain("domain");
    localeResolver.setCookieHttpOnly(true);
    localeResolver.setCookieSameSite("foo");

    ResponseCookie cookie = ResponseCookie.from(COOKIE_WITH_ATTRIBUTES_NAME, "value")
            .sameSite("Strict")
            .build();

    this.mockMvc = standaloneSetup(new SimpleController())
            .addInterceptors(new LocaleChangeInterceptor())
            .addInterceptors(new HandlerInterceptor() {

              @Override
              public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
                request.addCookie(cookie);
                return true;
              }
            })
            .setLocaleResolver(localeResolver)
            .defaultRequest(get("/").param("locale", "en_US"))
            .alwaysExpect(status().isOk())
            .build();
  }

  @Test
  public void testExists() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().exists(COOKIE_NAME));
  }

  @Test
  public void testNotExists() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().doesNotExist("unknownCookie"));
  }

  @Test
  public void testEqualTo() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, "en-US"));
    this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, equalTo("en-US")));
  }

  @Test
  public void testMatcher() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, startsWith("en")));
  }

  @Test
  public void testMaxAge() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().maxAge(COOKIE_NAME, -1));
  }

  @Test
  public void testDomain() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().domain(COOKIE_NAME, "domain"));
  }

  @Test
  void sameSite() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie()
            .sameSite(COOKIE_NAME, "foo"));
  }

  @Test
  void sameSiteMatcher() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie()
            .sameSite(COOKIE_WITH_ATTRIBUTES_NAME, startsWith("Str")));
  }

  @Test
  void sameSiteNotEquals() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    this.mockMvc.perform(get("/")).andExpect(cookie()
                            .sameSite(COOKIE_WITH_ATTRIBUTES_NAME, "Str")))
            .withMessage("Response cookie 'SecondCookie' attribute 'SameSite' expected:<Str> but was:<Strict>");
  }

  @Test
  public void path() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().path(COOKIE_NAME, "/"));
  }

  @Test
  public void testSecured() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().secure(COOKIE_NAME, false));
  }

  @Test
  public void testHttpOnly() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(cookie().httpOnly(COOKIE_NAME, true));
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/")
    public String home() {
      return "home";
    }
  }

}

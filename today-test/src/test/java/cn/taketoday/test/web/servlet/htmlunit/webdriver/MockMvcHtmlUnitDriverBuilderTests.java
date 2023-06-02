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

package cn.taketoday.test.web.servlet.htmlunit.webdriver;

import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.io.IOException;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Integration tests for {@link MockMvcHtmlUnitDriverBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitWebConfig
class MockMvcHtmlUnitDriverBuilderTests {

  private static final String EXPECTED_BODY = "MockMvcHtmlUnitDriverBuilderTests mvc";

  private MockMvc mockMvc;

  private HtmlUnitDriver driver;

  MockMvcHtmlUnitDriverBuilderTests(WebApplicationContext wac) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  void webAppContextSetupNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> MockMvcHtmlUnitDriverBuilder.webAppContextSetup(null));
  }

  @Test
  void mockMvcSetupNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> MockMvcHtmlUnitDriverBuilder.mockMvcSetup(null));
  }

  @Test
  void mockMvcSetupWithCustomDriverDelegate() throws Exception {
    WebConnectionHtmlUnitDriver otherDriver = new WebConnectionHtmlUnitDriver();
    this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherDriver).build();

    assertMockMvcUsed("http://localhost/test");
  }

  @Test
  void mockMvcSetupWithDefaultDriverDelegate() throws Exception {
    this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).build();

    assertMockMvcUsed("http://localhost/test");
  }

  @Test
  void javaScriptEnabledByDefault() {
    this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).build();
    assertThat(this.driver.isJavascriptEnabled()).isTrue();
  }

  @Test
  void javaScriptDisabled() {
    this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).javascriptEnabled(false).build();
    assertThat(this.driver.isJavascriptEnabled()).isFalse();
  }

  @Test
    // SPR-14066
  void cookieManagerShared() throws Exception {
    WebConnectionHtmlUnitDriver otherDriver = new WebConnectionHtmlUnitDriver();
    this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
    this.driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherDriver).build();

    assertThat(get("http://localhost/")).isEqualTo("");
    Cookie cookie = new Cookie("localhost", "cookie", "cookieManagerShared");
    otherDriver.getWebClient().getCookieManager().addCookie(cookie);
    assertThat(get("http://localhost/")).isEqualTo("cookieManagerShared");
  }

  private void assertMockMvcUsed(String url) throws Exception {
    assertThat(get(url)).contains(EXPECTED_BODY);
  }

  private String get(String url) throws IOException {
    this.driver.get(url);
    return this.driver.getPageSource();
  }

  @Configuration
  @EnableWebMvc
  static class Config {

    @RestController
    static class ContextPathController {

      @RequestMapping("/test")
      String contextPath(HttpServletRequest request) {
        return EXPECTED_BODY;
      }
    }
  }

  @RestController
  static class CookieController {

    @RequestMapping(path = "/", produces = "text/plain")
    String cookie(@CookieValue("cookie") String cookie) {
      return cookie;
    }
  }

}

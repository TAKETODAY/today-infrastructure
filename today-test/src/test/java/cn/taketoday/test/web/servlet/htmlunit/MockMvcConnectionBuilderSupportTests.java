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

package cn.taketoday.test.web.servlet.htmlunit;

import org.htmlunit.WebClient;
import org.htmlunit.WebConnection;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link MockMvcWebConnectionBuilderSupport}.
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@JUnitWebConfig
@SuppressWarnings("rawtypes")
public class MockMvcConnectionBuilderSupportTests {

  private final WebClient client = mock();

  private MockMvcWebConnectionBuilderSupport builder;

  @Autowired
  private WebApplicationContext wac;

  @BeforeEach
  public void setup() {
    given(this.client.getWebConnection()).willReturn(mock());
    this.builder = new MockMvcWebConnectionBuilderSupport(this.wac) { };
  }

  @Test
  public void constructorMockMvcNull() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new MockMvcWebConnectionBuilderSupport((MockMvc) null) { });
  }

  @Test
  public void constructorContextNull() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new MockMvcWebConnectionBuilderSupport((WebApplicationContext) null) { });
  }

  @Test
  public void context() throws Exception {
    WebConnection conn = this.builder.createConnection(this.client);

    assertMockMvcUsed(conn, "http://localhost/");
    assertMockMvcNotUsed(conn, "https://example.com/");
  }

  @Test
  public void mockMvc() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    WebConnection conn = new MockMvcWebConnectionBuilderSupport(mockMvc) { }.createConnection(this.client);

    assertMockMvcUsed(conn, "http://localhost/");
    assertMockMvcNotUsed(conn, "https://example.com/");
  }

  @Test
  public void mockMvcExampleDotCom() throws Exception {
    WebConnection conn = this.builder.useMockMvcForHosts("example.com").createConnection(this.client);

    assertMockMvcUsed(conn, "http://localhost/");
    assertMockMvcUsed(conn, "https://example.com/");
    assertMockMvcNotUsed(conn, "http://other.example/");
  }

  @Test
  public void mockMvcAlwaysUseMockMvc() throws Exception {
    WebConnection conn = this.builder.alwaysUseMockMvc().createConnection(this.client);
    assertMockMvcUsed(conn, "http://other.example/");
  }

  @Test
  public void defaultContextPathEmpty() throws Exception {
    WebConnection conn = this.builder.createConnection(this.client);
    assertThat(getResponse(conn, "http://localhost/abc").getContentAsString()).isEqualTo("");
  }

  @Test
  public void defaultContextPathCustom() throws Exception {
    WebConnection conn = this.builder.contextPath("/abc").createConnection(this.client);
    assertThat(getResponse(conn, "http://localhost/abc/def").getContentAsString()).isEqualTo("/abc");
  }

  private void assertMockMvcUsed(WebConnection connection, String url) throws Exception {
    assertThat(getResponse(connection, url)).isNotNull();
  }

  private void assertMockMvcNotUsed(WebConnection connection, String url) throws Exception {
    assertThat(getResponse(connection, url)).isNull();
  }

  private WebResponse getResponse(WebConnection connection, String url) throws IOException {
    return connection.getResponse(new WebRequest(new URL(url)));
  }

  @Configuration
  @EnableWebMvc
  static class Config {

    @RestController
    static class ContextPathController {

      @RequestMapping("/def")
      public String contextPath(HttpServletRequest request) {
        return request.getContextPath();
      }
    }
  }

}

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

package cn.taketoday.framework.web.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.util.WebUtils;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the default error view.
 *
 * @author Dave Syer
 * @author Scott Frederick
 */
@DirtiesContext
@InfraTest(properties = { "server.error.include-message=always" })
class DefaultErrorViewIntegrationTests {

  @Autowired
  private ApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  void testErrorForBrowserClient() throws Exception {
    MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
            .andExpect(status().is5xxServerError())
            .andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("<html>");
    assertThat(content).contains("999");
  }

  @Test
  void testErrorWithHtmlEscape() throws Exception {
    MvcResult response = this.mockMvc
            .perform(
                    get("/error")
                            .requestAttr(WebUtils.ERROR_EXCEPTION_ATTRIBUTE,
                                    new RuntimeException("<script>alert('Hello World')</script>"))
                            .accept(MediaType.TEXT_HTML))
            .andExpect(status().is5xxServerError())
            .andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("&lt;script&gt;");
    assertThat(content).contains("Hello World");
    assertThat(content).contains("999");
  }

  @Test
  void testErrorWithSpelEscape() throws Exception {
    String spel = "${T(" + getClass().getName() + ").injectCall()}";
    MvcResult response = this.mockMvc
            .perform(get("/error").requestAttr(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new RuntimeException(spel))
                    .accept(MediaType.TEXT_HTML))
            .andExpect(status().is5xxServerError())
            .andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).doesNotContain("injection");
  }

  static String injectCall() {
    return "injection";
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Import({
          RandomPortWebServerConfig.class,
          WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
          ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
  protected @interface MinimalWebConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @MinimalWebConfiguration
  static class TestConfiguration {

    // For manual testing
    public static void main(String[] args) {
      Application.run(TestConfiguration.class, args);
    }

  }

}

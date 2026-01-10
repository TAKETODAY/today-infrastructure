/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.RandomPortWebServerConfig;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.app.Application;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.http.MediaType;
import infra.test.annotation.DirtiesContext;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.util.WebUtils;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
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

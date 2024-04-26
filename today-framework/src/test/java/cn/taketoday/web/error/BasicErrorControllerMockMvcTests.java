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

package cn.taketoday.web.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.ImportAutoConfiguration;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.NonNull;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.RequestBuilder;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.validation.BindException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.server.error.BasicErrorController;
import cn.taketoday.web.view.AbstractView;
import cn.taketoday.web.view.View;
import jakarta.servlet.ServletContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvc} and
 * {@link InfraTest @InfraTest}.
 *
 * @author Dave Syer
 * @author Scott Frederick
 */
@InfraTest(properties = { "server.error.include-message=always" })
@DirtiesContext
class BasicErrorControllerMockMvcTests {

  @Autowired
  private ApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  void testDirectAccessForMachineClient() throws Exception {
    MvcResult response = this.mockMvc.perform(get("/error")).andExpect(status().is5xxServerError()).andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("999");
  }

  @Test
  void testErrorWithNotFoundResponseStatus() throws Exception {
    MvcResult result = this.mockMvc.perform(get("/bang")).andExpect(status().isNotFound()).andReturn();
    MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error")).andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("Expected!");
  }

  @Test
  void testErrorWithNoContentResponseStatus() throws Exception {
    MvcResult result = this.mockMvc.perform(get("/noContent").accept("some/thing"))
            .andExpect(status().isNoContent())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    assertThat(content).isEmpty();
  }

  @Test
  void testBindingExceptionForMachineClient() throws Exception {
    // In a real server the response is carried over into the error dispatcher, but
    // in the mock a new one is created so we have to assert the status at this
    // intermediate point
    MvcResult result = this.mockMvc.perform(get("/bind")).andExpect(status().is4xxClientError()).andReturn();
    MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error")).andReturn();
    // And the rendered status code is always wrong (but would be 400 in a real
    // system)
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("Validation failed");
  }

  @Test
  void testDirectAccessForBrowserClient() throws Exception {
    MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
            .andExpect(status().is5xxServerError())
            .andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("ERROR_BEAN");
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @ImportAutoConfiguration({
          WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
          ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
  private @interface MinimalWebConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @MinimalWebConfiguration
  static class TestConfiguration {

    // For manual testing
    static void main(String[] args) {
      Application.run(TestConfiguration.class, args);
    }

    @Bean
    View error() {
      return new AbstractView() {
        @Override
        protected void renderMergedOutputModel(@NonNull Map<String, Object> model, RequestContext request) throws Exception {
          request.getWriter().write("ERROR_BEAN");
        }
      };
    }

    @RestController
    public static class Errors {

      @RequestMapping("/")
      String home() {
        throw new IllegalStateException("Expected!");
      }

      @RequestMapping("/bang")
      String bang() {
        throw new NotFoundException("Expected!");
      }

      @RequestMapping("/bind")
      String bind() throws Exception {
        BindException error = new BindException(this, "test");
        error.rejectValue("foo", "bar.error");
        throw error;
      }

      @RequestMapping("/noContent")
      void noContent() {
        throw new NoContentException("Expected!");
      }

      public String getFoo() {
        return "foo";
      }

    }

  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  static class NotFoundException extends RuntimeException {

    NotFoundException(String string) {
      super(string);
    }

  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  private static class NoContentException extends RuntimeException {

    NoContentException(String string) {
      super(string);
    }

  }

  private static class ErrorDispatcher implements RequestBuilder {

    private final MvcResult result;

    private final String path;

    ErrorDispatcher(MvcResult result, String path) {
      this.result = result;
      this.path = path;
    }

    @Override
    public MockHttpServletRequest buildRequest(ServletContext servletContext) {
      MockHttpServletRequest request = this.result.getRequest();
      request.setRequestURI(this.path);
      return request;
    }

  }

}

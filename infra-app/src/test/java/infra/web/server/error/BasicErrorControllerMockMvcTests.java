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
import java.util.Map;

import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.app.Application;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.test.annotation.DirtiesContext;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.validation.BindException;
import infra.web.RequestContext;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.view.AbstractView;
import infra.web.view.View;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
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
        protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
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
    public HttpMockRequestImpl buildRequest(MockContext mockContext) {
      HttpMockRequestImpl request = this.result.getRequest();
      request.setRequestURI(this.path);
      return request;
    }

  }

}

/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.web.mock.samples.client.standalone;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.context.ApplicationContext;
import infra.http.HttpHeaders;
import infra.http.client.reactive.ClientHttpConnector;
import infra.mock.web.HttpMockRequestImpl;
import infra.stereotype.Controller;
import infra.test.web.mock.client.MockMvcHttpConnector;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.mock.request.RequestPostProcessor;
import infra.test.web.mock.setup.ConfigurableMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcConfigurerAdapter;
import infra.test.web.reactive.server.WebTestClient;
import infra.test.web.reactive.server.WebTestClientConfigurer;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;

import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.FrameworkExtensionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class FrameworkExtensionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SampleController())
                  .apply(defaultSetup())
                  .build();

  @Test
  public void fooHeader() {
    this.client.mutateWith(headers().foo("a=b"))
            .get().uri("/")
            .exchange()
            .expectBody(String.class).isEqualTo("Foo");
  }

  @Test
  public void barHeader() {
    this.client.mutateWith(headers().bar("a=b"))
            .get().uri("/")
            .exchange()
            .expectBody(String.class).isEqualTo("Bar");
  }

  private static TestMockMvcConfigurer defaultSetup() {
    return new TestMockMvcConfigurer();
  }

  private static TestWebTestClientConfigurer headers() {
    return new TestWebTestClientConfigurer();
  }

  /**
   * Test WebTestClientConfigurer that re-creates the MockMvcHttpConnector
   * with a {@code TestRequestPostProcessor}.
   */
  private static class TestWebTestClientConfigurer implements WebTestClientConfigurer {

    private final TestRequestPostProcessor requestPostProcessor = new TestRequestPostProcessor();

    public TestWebTestClientConfigurer foo(String value) {
      this.requestPostProcessor.foo(value);
      return this;
    }

    public TestWebTestClientConfigurer bar(String value) {
      this.requestPostProcessor.bar(value);
      return this;
    }

    @Override
    public void afterConfigurerAdded(WebTestClient.Builder builder, ClientHttpConnector connector) {
      if (connector instanceof MockMvcHttpConnector mockMvcConnector) {
        builder.clientConnector(mockMvcConnector.with(List.of(this.requestPostProcessor)));
      }
    }
  }

  /**
   * Test {@code RequestPostProcessor} for custom headers.
   */
  private static class TestRequestPostProcessor implements RequestPostProcessor {

    private final HttpHeaders headers = HttpHeaders.forWritable();

    public TestRequestPostProcessor foo(String value) {
      this.headers.add("Foo", value);
      return this;
    }

    public TestRequestPostProcessor bar(String value) {
      this.headers.add("Bar", value);
      return this;
    }

    @Override
    public HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request) {
      for (String headerName : this.headers.keySet()) {
        request.addHeader(headerName, this.headers.get(headerName));
      }
      return request;
    }
  }

  /**
   * Test {@code MockMvcConfigurer}.
   */
  private static class TestMockMvcConfigurer extends MockMvcConfigurerAdapter {

    @Override
    public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
      builder.alwaysExpect(status().isOk());
    }

    @Override
    public RequestPostProcessor beforeMockMvcCreated(
            ConfigurableMockMvcBuilder<?> builder, ApplicationContext context) {

      return request -> {
        request.setUserPrincipal(mock());
        return request;
      };
    }
  }

  @Controller
  @RequestMapping("/")
  private static class SampleController {

    @RequestMapping(headers = "Foo")
    @ResponseBody
    public String handleFoo() {
      return "Foo";
    }

    @RequestMapping(headers = "Bar")
    @ResponseBody
    public String handleBar() {
      return "Bar";
    }
  }

}

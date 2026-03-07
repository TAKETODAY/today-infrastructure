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

package infra.test.web.mock.assertj;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.ResolvableType;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.test.json.AbstractJsonContentAssert;
import infra.test.web.mock.assertj.MockMvcTester.MockMvcRequestBuilder;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RestController;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.mock.support.GenericWebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link infra.test.web.mock.assertj.MockMvcTester}.
 *
 * @author Stephane Nicoll
 */
class MockMvcTesterTests {

  private static final JacksonJsonHttpMessageConverter jsonHttpMessageConverter =
          new JacksonJsonHttpMessageConverter();

  private final MockContext mockContext = new MockContextImpl();

  @Test
  void createShouldRejectNullMockMvc() {
    assertThatIllegalArgumentException().isThrownBy(() -> MockMvcTester.create(null));
  }

  @Test
  void createWithExistingWebApplicationContext() {
    try (GenericWebApplicationContext wac = create(WebConfiguration.class)) {
      MockMvcTester mockMvc = MockMvcTester.from(wac);
      assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 41");
      assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 42");
    }
  }

  @Test
  void createWithControllerClassShouldInstantiateControllers() {
    MockMvcTester mockMvc = MockMvcTester.of(HelloController.class, CounterController.class);
    assertThat(mockMvc.perform(get("/hello"))).hasBodyTextEqualTo("Hello World");
    assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 1");
    assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 2");
  }

  @Test
  void createWithControllersShouldUseThemAsIs() {
    MockMvcTester mockMvc = MockMvcTester.of(new HelloController(),
            new CounterController(new AtomicInteger(41)));
    assertThat(mockMvc.perform(get("/hello"))).hasBodyTextEqualTo("Hello World");
    assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 42");
    assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 43");
  }

  @Test
  void createWithControllerAndCustomizations() {
    MockMvcTester mockMvc = MockMvcTester.of(List.of(new HelloController()), builder ->
            builder.defaultRequest(get("/hello").accept(MediaType.APPLICATION_JSON)).build());
    assertThat(mockMvc.perform(get("/hello"))).hasStatus(HttpStatus.NOT_ACCEPTABLE);
  }

  @Test
  void createWithControllersHasNoHttpMessageConverter() {
    MockMvcTester mockMvc = MockMvcTester.of(new HelloController());
    AbstractJsonContentAssert<?> jsonContentAssert = assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson();
    assertThatIllegalStateException()
            .isThrownBy(() -> jsonContentAssert.extractingPath("$").convertTo(Message.class))
            .withMessageContaining("No JSON message converter available");
  }

  @Test
  void createWithControllerCanConfigureHttpMessageConverters() {
    MockMvcTester mockMvc = MockMvcTester.of(HelloController.class)
            .withHttpMessageConverters(List.of(jsonHttpMessageConverter));
    assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson()
            .extractingPath("$").convertTo(Message.class).satisfies(message -> {
              assertThat(message.message()).isEqualTo("Hello World");
              assertThat(message.counter()).isEqualTo(42);
            });
  }

  @Test
  void withHttpMessageConverters() {
    MockMvcTester mockMvc = MockMvcTester.of(HelloController.class)
            .withHttpMessageConverters(HttpMessageConverters.forClient().addCustomConverter(jsonHttpMessageConverter).build());
    assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson()
            .extractingPath("$").convertTo(Message.class).satisfies(message -> {
              assertThat(message.message()).isEqualTo("Hello World");
              assertThat(message.counter()).isEqualTo(42);
            });
  }

  @Test
  void withHttpMessageConvertersClientBuilder() {
    MockMvcTester mockMvc = MockMvcTester.of(HelloController.class)
            .withHttpMessageConverters(clientBuilder -> clientBuilder.addCustomConverter(jsonHttpMessageConverter));
    assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson()
            .extractingPath("$").convertTo(Message.class).satisfies(message -> {
              assertThat(message.message()).isEqualTo("Hello World");
              assertThat(message.counter()).isEqualTo(42);
            });
  }

  @Test
  void withHttpMessageConverterUsesConverter() {
    JacksonJsonHttpMessageConverter converter = spy(jsonHttpMessageConverter);
    MockMvcTester mockMvc = MockMvcTester.of(HelloController.class)
            .withHttpMessageConverters(List.of(mock(), mock(), converter));
    assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson()
            .extractingPath("$").convertTo(Message.class).satisfies(message -> {
              assertThat(message.message()).isEqualTo("Hello World");
              assertThat(message.counter()).isEqualTo(42);
            });
    verify(converter).canWrite(ResolvableType.forClass(LinkedHashMap.class), LinkedHashMap.class, MediaType.APPLICATION_JSON);
  }

  @Test
  void performWithUnresolvedExceptionSetsException() {
    MockMvcTester mockMvc = MockMvcTester.of(HelloController.class);
    MvcTestResult result = mockMvc.perform(get("/error"));
    assertThat(result.getUnresolvedException()).isInstanceOf(IllegalStateException.class).hasMessage("Expected");
    assertThat(result).extracting("mvcResult").isNotNull();
  }

  @Test
  void getConfiguresBuilder() {
    assertThat(createMockHttpRequest(tester -> tester.get().uri("/hello/{id}", "world")))
            .satisfies(hasSettings(HttpMethod.GET, "/hello/{id}", "/hello/world"));
  }

  @Test
  void headConfiguresBuilder() {
    assertThat(createMockHttpRequest(tester -> tester.head().uri("/download/{file}", "test.json")))
            .satisfies(hasSettings(HttpMethod.HEAD, "/download/{file}", "/download/test.json"));
  }

  @Test
  void postConfiguresBuilder() {
    assertThat(createMockHttpRequest(tester -> tester.post().uri("/save/{id}", 123)))
            .satisfies(hasSettings(HttpMethod.POST, "/save/{id}", "/save/123"));
  }

  @Test
  void putConfiguresBuilder() {
    assertThat(createMockHttpRequest(tester -> tester.put().uri("/save/{id}", 123)))
            .satisfies(hasSettings(HttpMethod.PUT, "/save/{id}", "/save/123"));
  }

  @Test
  void patchConfiguresBuilder() {
    assertThat(createMockHttpRequest(tester -> tester.patch().uri("/update/{id}", 123)))
            .satisfies(hasSettings(HttpMethod.PATCH, "/update/{id}", "/update/123"));
  }

  @Test
  void deleteConfiguresBuilder() {
    assertThat(createMockHttpRequest(tester -> tester.delete().uri("/users/{id}", 42)))
            .satisfies(hasSettings(HttpMethod.DELETE, "/users/{id}", "/users/42"));
  }

  @Test
  void optionsConfiguresBuilder() {
    assertThat(createMockHttpRequest(tester -> tester.options().uri("/users/{id}", 42)))
            .satisfies(hasSettings(HttpMethod.OPTIONS, "/users/{id}", "/users/42"));
  }

  @Test
  void methodConfiguresBuilderWithFullURI() {
    assertThat(createMockHttpRequest(tester -> tester.get().uri(URI.create("/hello/world"))))
            .satisfies(hasSettings(HttpMethod.GET, null, "/hello/world"));
  }

  private HttpMockRequestImpl createMockHttpRequest(Function<MockMvcTester, MockMvcRequestBuilder> builder) {
    MockMvcTester mockMvcTester = MockMvcTester.of(HelloController.class);
    return builder.apply(mockMvcTester).buildRequest(this.mockContext);
  }

  private Consumer<HttpMockRequestImpl> hasSettings(HttpMethod method, @Nullable String uriTemplate, String uri) {
    return request -> {
      assertThat(request.getMethod()).isEqualTo(method.name());
      assertThat(request.getUriTemplate()).isEqualTo(uriTemplate);
      assertThat(request.getRequestURI()).isEqualTo(uri);
    };
  }

  private GenericWebApplicationContext create(Class<?>... classes) {
    GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(new MockContextImpl());
    AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
    for (Class<?> beanClass : classes) {
      applicationContext.registerBean(beanClass);
    }
    applicationContext.refresh();
    return applicationContext;
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class WebConfiguration {

    @Bean
    CounterController counterController() {
      return new CounterController(new AtomicInteger(40));
    }
  }

  @RestController
  private static class HelloController {

    @GetMapping(path = "/hello", produces = "text/plain")
    public String hello() {
      return "Hello World";
    }

    @GetMapping("/error")
    public String error() {
      throw new IllegalStateException("Expected");
    }

    @GetMapping(path = "/json", produces = "application/json")
    public String json() {
      return """
              {
              	"message": "Hello World",
              	"counter": 42
              }""";
    }
  }

  private record Message(String message, int counter) { }

  @RestController
  static class CounterController {

    private final AtomicInteger counter;

    CounterController() {
      this(new AtomicInteger());
    }

    CounterController(AtomicInteger counter) {
      this.counter = counter;
    }

    @PostMapping("/increase")
    String increase() {
      int value = this.counter.incrementAndGet();
      return "counter " + value;
    }
  }

}

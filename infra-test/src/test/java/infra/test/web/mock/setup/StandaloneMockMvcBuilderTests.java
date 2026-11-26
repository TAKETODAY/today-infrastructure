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

package infra.test.web.mock.setup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import infra.context.ApplicationContext;
import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.stereotype.Controller;
import infra.web.Filter;
import infra.web.annotation.RequestMapping;
import infra.web.handler.HandlerExecutionChain;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StandaloneMockMvcBuilder}
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sebastien Deleuze
 */
class StandaloneMockMvcBuilderTests {

  @Test
  void placeHoldersInRequestMapping() throws Exception {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
    builder.addPlaceholderValue("sys.login.ajax", "/foo");
    builder.build();

    RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

    MockRequest request = new MockRequest("GET", "/foo");
    HandlerExecutionChain chain = (HandlerExecutionChain) hm.getHandler(new MockServerHttpExchange(null,
            request, new MockResponse()));

    assertThat(chain).isNotNull();
    assertThat(((HandlerMethod) chain.getRawHandler()).getMethod().getName()).isEqualTo("handleWithPlaceholders");
  }

  @Test
  void suffixPatternMatch() throws Exception {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
    builder.build();

    RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

    MockRequest request = new MockRequest("GET", "/persons");
    HandlerExecutionChain chain = (HandlerExecutionChain) hm.getHandler(new MockServerHttpExchange(null,
            request, new MockResponse()));
    assertThat(chain).isNotNull();
    assertThat(((HandlerMethod) chain.getRawHandler()).getMethod().getName()).isEqualTo("persons");

    request = new MockRequest("GET", "/persons.xml");
    chain = (HandlerExecutionChain) hm.getHandler(new MockServerHttpExchange(null,
            request, new MockResponse()));
    assertThat(chain).isNull();
  }

  @Test
  void addFiltersFiltersNull() {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
    assertThatIllegalArgumentException().isThrownBy(() ->
            builder.addFilters((Filter[]) null));
  }

  @ParameterizedTest
  @CsvSource({
          "PT-1S, Timeout must not be negative",
          "PT0.000000001S, Timeout must be zero or at least 1 millisecond",
          "PT9223372036854775807S, Timeout is too large"
  })
  void setAsyncRequestTimeoutRejectsInvalidValues(Duration timeout, String message) {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());

    assertThatIllegalArgumentException().isThrownBy(() -> builder.setAsyncRequestTimeout(timeout))
            .withMessage(message);
  }

  @Controller
  private static class PlaceholderController {

    @RequestMapping(value = "${sys.login.ajax}")
    private void handleWithPlaceholders() { }
  }

  private static class TestStandaloneMockMvcBuilder extends StandaloneMockMvcBuilder {

    private ApplicationContext wac;

    private TestStandaloneMockMvcBuilder(Object... controllers) {
      super(controllers);
    }

    @Override
    protected ApplicationContext initApplicationContext() {
      this.wac = super.initApplicationContext();
      return this.wac;
    }
  }

  @Controller
  private static class PersonController {

    @RequestMapping(value = "/persons")
    public String persons() {
      return null;
    }

    @RequestMapping(value = "/forward")
    public String forward() {
      return "forward:/persons";
    }
  }

}

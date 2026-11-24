/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.web.mock.setup;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContextAware;
import infra.validation.Validator;
import infra.web.HandlerExceptionHandler;
import infra.web.RedirectModelManager;
import infra.web.accept.ApiVersionStrategy;
import infra.web.annotation.GET;
import infra.web.annotation.RestController;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.method.ParameterResolvingRegistry;
import infra.web.view.ViewResolver;
import infra.web.client.ApiVersionInserter;
import infra.web.Filter;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.withSettings;

class StandaloneInfrastructureTests {

  @Test
  void builderValidatesFiltersAndSupportsBothKindsOfDefaultRequests() {
    var builder = MockMvcBuilders.standaloneSetup(Controller.class);
    assertThatIllegalArgumentException().isThrownBy(() -> builder.addFilter(null));
    assertThatIllegalArgumentException().isThrownBy(() -> builder.addFilter(mock(Filter.class), (String[]) null));
    for (RequestBuilder defaults : new RequestBuilder[] { null, MockMvcRequestBuilders.get("/"), mock(RequestBuilder.class) }) {
      var configured = MockMvcBuilders.standaloneSetup(Controller.class)
              .defaultRequest(defaults).apiVersionInserter(ApiVersionInserter.forHeader("X-Version"));
      assertThat(configured.build()).isNotNull();
      var configurer = new MockMvcConfigurer() {
        @Override
        public infra.test.web.mock.request.MockRequestCustomizer beforeMockMvcCreated(
                ConfigurableMockMvcBuilder<?> builder, infra.web.mock.api.MockContext context) {
          return (request, httpContext) -> { };
        }
      };
      assertThat(MockMvcBuilders.standaloneSetup(Controller.class).defaultRequest(defaults).apply(configurer).build()).isNotNull();
    }
  }

  @Test
  void customInfrastructureAndExceptionHandlerLifecycleAreApplied() throws Exception {
    HandlerExceptionHandler handler = mock(HandlerExceptionHandler.class,
            withSettings().extraInterfaces(ApplicationContextAware.class, InitializingBean.class));
    var mvc = MockMvcBuilders.standaloneSetup(Controller.class)
            .setApiVersionStrategy(mock(ApiVersionStrategy.class))
            .setRedirectModelManager(mock(RedirectModelManager.class))
            .setParameterResolvingRegistry(new ParameterResolvingRegistry())
            .setReturnValueHandlerManager(new ReturnValueHandlerManager())
            .setViewResolvers(mock(ViewResolver.class), mock(ViewResolver.class))
            .setHandlerExceptionHandlers(List.of(handler)).build();
    assertThat(mvc).isNotNull();
    verify((ApplicationContextAware) handler, times(2)).setApplicationContext(any());
    verify((InitializingBean) handler, times(2)).afterPropertiesSet();
  }

  @Test
  void invalidValidatorInitializationRetainsCause() throws Exception {
    Validator validator = mock(Validator.class, withSettings().extraInterfaces(InitializingBean.class));
    Exception failure = new Exception("validator initialization");
    willThrow(failure).given((InitializingBean) validator).afterPropertiesSet();
    assertThatThrownBy(() -> MockMvcBuilders.standaloneSetup(Controller.class).setValidator(validator).build())
            .hasRootCause(failure);
  }

  @Test
  void invalidExceptionHandlerInitializationRetainsCause() throws Exception {
    HandlerExceptionHandler handler = mock(HandlerExceptionHandler.class, withSettings().extraInterfaces(InitializingBean.class));
    Exception failure = new Exception("handler initialization");
    willThrow(failure).given((InitializingBean) handler).afterPropertiesSet();
    assertThatThrownBy(() -> MockMvcBuilders.standaloneSetup(Controller.class).setHandlerExceptionHandlers(handler).build())
            .hasRootCause(failure);
  }

  @RestController
  static class Controller {
    @GET("/hello")
    String hello() {
      return "hello";
    }
  }

}

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

package infra.test.web.mock.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.http.MediaType;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.config.annotation.AsyncSupportConfigurer;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.context.AsyncControllerJavaConfigTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextHierarchy(@ContextConfiguration(classes = AsyncControllerJavaConfigTests.WebConfig.class))
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
public class AsyncControllerJavaConfigTests {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private CallableProcessingInterceptor callableInterceptor;

  private WebTestClient testClient;

  @BeforeEach
  public void setup() {
    this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
  }

  @Test
  public void callableInterceptor() throws Exception {
    testClient.get().uri("/callable")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
//            .consumeWith(System.out::println)
            .json("{\"key\":\"value\"}");

    verify(this.callableInterceptor).beforeConcurrentHandling(any(), any());
    verify(this.callableInterceptor).preProcess(any(), any());
    verify(this.callableInterceptor).postProcess(any(), any(), any());
    verify(this.callableInterceptor).afterCompletion(any(), any());
    verifyNoMoreInteractions(this.callableInterceptor);
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
      configurer.registerCallableInterceptors(callableInterceptor());
    }

    @Bean
    public CallableProcessingInterceptor callableInterceptor() {
      return mock();
    }

    @Bean
    public AsyncController asyncController() {
      return new AsyncController();
    }

  }

  @RestController
  static class AsyncController {

    @GetMapping("/callable")
    public Callable<Map<String, String>> getCallable() {
      return () -> Collections.singletonMap("key", "value");
    }
  }

}

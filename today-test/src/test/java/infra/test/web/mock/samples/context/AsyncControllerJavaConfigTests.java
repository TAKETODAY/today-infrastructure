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

package infra.test.web.mock.samples.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

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
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.config.annotation.AsyncSupportConfigurer;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.asyncDispatch;
import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.request;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * Tests with Java configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
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

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }



  @Test
  public void callableInterceptor() throws Exception {
    MvcResult mvcResult = this.mockMvc.perform(get("/callable").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andExpect(request().asyncResult(Collections.singletonMap("key", "value")))
            .andReturn();

    Mockito.verify(this.callableInterceptor).beforeConcurrentHandling(any(), any());
    Mockito.verify(this.callableInterceptor).preProcess(any(), any());
    Mockito.verify(this.callableInterceptor).postProcess(any(), any(), any());
    Mockito.verifyNoMoreInteractions(this.callableInterceptor);

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().string("{\"key\":\"value\"}"));

    Mockito.verify(this.callableInterceptor).afterCompletion(any(), any());
    Mockito.verifyNoMoreInteractions(this.callableInterceptor);
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

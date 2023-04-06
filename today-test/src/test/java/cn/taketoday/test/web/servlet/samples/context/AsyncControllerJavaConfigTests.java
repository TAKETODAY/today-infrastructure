/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet.samples.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.config.AsyncSupportConfigurer;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.context.async.CallableProcessingInterceptor;
import cn.taketoday.web.servlet.WebApplicationContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
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

  // SPR-13615

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

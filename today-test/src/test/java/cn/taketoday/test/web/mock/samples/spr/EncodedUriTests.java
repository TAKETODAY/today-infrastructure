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

package cn.taketoday.test.web.mock.samples.spr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.test.web.mock.ResultActions;
import cn.taketoday.test.web.mock.setup.InternalResourceViewResolver;
import cn.taketoday.ui.Model;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.ViewResolverRegistry;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.mock.WebApplicationContext;
import cn.taketoday.web.util.UriComponentsBuilder;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.hamcrest.core.Is.is;

/**
 * Tests for SPR-11441 (MockMvc accepts an already encoded URI).
 *
 * @author Sebastien Deleuze
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class EncodedUriTests {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @Test
  public void test() throws Exception {
    String id = "a/b";
    URI url = UriComponentsBuilder.fromUriString("/circuit").pathSegment(id).build().encode().toUri();
    ResultActions result = mockMvc.perform(get(url));
    result.andExpect(status().isOk())
            .andExpect(model().attribute("receivedId", is(id)));
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Bean
    public MyController myController() {
      return new MyController();
    }

    @Bean
    public HandlerMappingConfigurer myHandlerMappingConfigurer() {
      return new HandlerMappingConfigurer();
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
//      registry.jsp("", "");
      registry.viewResolver(new InternalResourceViewResolver());
    }
  }

  @Controller
  private static class MyController {

    @RequestMapping(value = "/circuit/{id}", method = HttpMethod.GET)
    public String getCircuit(@PathVariable String id, Model model) {
      model.addAttribute("receivedId", id);
      return "result";
    }
  }

  @Component
  private static class HandlerMappingConfigurer implements InitializationBeanPostProcessor, PriorityOrdered {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      if (bean instanceof RequestMappingHandlerMapping requestMappingHandlerMapping) {
        // URL decode after request mapping, not before.
//        UrlPathHelper pathHelper = new UrlPathHelper();
//        pathHelper.setUrlDecode(false);
//        requestMappingHandlerMapping.setUrlPathHelper(pathHelper);
      }
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      return bean;
    }

    @Override
    public int getOrder() {
      return PriorityOrdered.HIGHEST_PRECEDENCE;
    }
  }

}

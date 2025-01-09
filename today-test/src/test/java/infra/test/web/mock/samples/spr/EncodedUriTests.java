/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.web.mock.samples.spr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.PriorityOrdered;
import infra.http.HttpMethod;
import infra.stereotype.Component;
import infra.stereotype.Controller;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.ResultActions;
import infra.test.web.mock.setup.InternalResourceViewResolver;
import infra.ui.Model;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.config.EnableWebMvc;
import infra.web.config.ViewResolverRegistry;
import infra.web.config.WebMvcConfigurer;
import infra.web.handler.method.RequestMappingHandlerMapping;
import infra.web.mock.WebApplicationContext;
import infra.web.util.UriComponentsBuilder;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.model;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.hamcrest.core.Is.is;

/**
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
    URI url = UriComponentsBuilder.forURIString("/circuit").pathSegment(id).build().encode().toURI();
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

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

package infra.test.web.mock.samples.spr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.http.HttpMethod;
import infra.stereotype.Controller;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;
import infra.web.config.EnableWebMvc;
import infra.web.config.WebMvcConfigurer;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.options;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arnaud Cogoluègnes
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class HttpOptionsTests {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @Test
  public void test() throws Exception {
    MyController controller = this.wac.getBean(MyController.class);
    int initialCount = controller.counter.get();
    this.mockMvc.perform(options("/myUrl")).andExpect(status().isOk());

    assertThat(controller.counter.get()).isEqualTo((initialCount + 1));
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Bean
    public MyController myController() {
      return new MyController();
    }
  }

  @Controller
  private static class MyController {

    private final AtomicInteger counter = new AtomicInteger();

    @RequestMapping(value = "/myUrl", method = HttpMethod.OPTIONS)
    @ResponseBody
    public void handle() {
      counter.incrementAndGet();
    }
  }

}

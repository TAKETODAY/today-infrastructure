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

package cn.taketoday.test.web.servlet.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.aot.DisabledInAotMode;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.test.web.servlet.samples.context.PersonController;
import cn.taketoday.test.web.servlet.samples.context.PersonDao;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.ResourceHandlerRegistry;
import cn.taketoday.web.config.ViewControllerRegistry;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.mock.WebApplicationContext;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.context.JavaConfigTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration("classpath:META-INF/web-resources")
@ContextHierarchy({
        @ContextConfiguration(classes = JavaConfigTests.RootConfig.class),
        @ContextConfiguration(classes = JavaConfigTests.WebConfig.class)
})
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class JavaConfigTests {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private PersonDao personDao;

  private WebTestClient testClient;

  @BeforeEach
  void setup() {
    this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
    given(this.personDao.getPerson(5L)).willReturn(new Person("Joe"));
  }

  @Test
  void person() {
    testClient.get().uri("/person/5")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
  }

  @Configuration
  static class RootConfig {

    @Bean
    PersonDao personDao() {
      return mock();
    }
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RootConfig rootConfig;

    @Bean
    PersonController personController() {
      return new PersonController(this.rootConfig.personDao());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
      registry.addViewController("/")
              .setViewName("home");
    }

  }

}

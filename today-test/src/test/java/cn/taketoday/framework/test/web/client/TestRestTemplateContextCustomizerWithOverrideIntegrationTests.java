/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.web.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.framework.test.context.InfraTest.WebEnvironment;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.test.annotation.DirtiesContext;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestRestTemplateContextCustomizer} with a custom
 * {@link TestRestTemplate} bean.
 *
 * @author Phillip Webb
 */
@InfraTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
class TestRestTemplateContextCustomizerWithOverrideIntegrationTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void test() {
    assertThat(this.restTemplate).isInstanceOf(CustomTestRestTemplate.class);
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ TestServlet.class, NoTestRestTemplateBeanChecker.class })
  static class TestConfig {

    @Bean
    TomcatServletWebServerFactory webServerFactory() {
      return new TomcatServletWebServerFactory(0);
    }

    @Bean
    TestRestTemplate template() {
      return new CustomTestRestTemplate();
    }

  }

  static class TestServlet extends GenericServlet {

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
      try (PrintWriter writer = response.getWriter()) {
        writer.println("hello");
      }
    }

  }

  static class CustomTestRestTemplate extends TestRestTemplate {

  }

}

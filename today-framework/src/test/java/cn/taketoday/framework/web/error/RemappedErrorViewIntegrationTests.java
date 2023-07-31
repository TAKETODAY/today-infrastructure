/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.error;

import org.junit.jupiter.api.Test;

import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.servlet.DispatcherServletAutoConfiguration;
import cn.taketoday.annotation.config.web.servlet.ServletWebServerFactoryAutoConfiguration;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.builder.ApplicationBuilder;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.framework.test.context.InfraTest.WebEnvironment;
import cn.taketoday.framework.test.web.client.TestRestTemplate;
import cn.taketoday.framework.test.web.server.LocalServerPort;
import cn.taketoday.framework.web.server.ErrorPage;
import cn.taketoday.framework.web.server.ErrorPageRegistrar;
import cn.taketoday.framework.web.server.ErrorPageRegistry;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.classpath.ClassPathExclusions;
import cn.taketoday.web.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for remapped error pages.
 *
 * @author Dave Syer
 */
@InfraTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "web.mvc.servlet.path:/infra/")
@DirtiesContext
@ClassPathExclusions("tomcat-embed-jasper*")
class RemappedErrorViewIntegrationTests {

  @LocalServerPort
  private int port;

  private final TestRestTemplate template = new TestRestTemplate();

  @Test
  void directAccessToErrorPage() {
    String content = this.template.getForObject("http://localhost:" + this.port + "/infra/error", String.class);
    assertThat(content).contains("error");
    assertThat(content).contains("999");
  }

  @Test
  void forwardToErrorPage() {
    String content = this.template.getForObject("http://localhost:" + this.port + "/infra/", String.class);
    assertThat(content).contains("error");
    assertThat(content).contains("500");
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
          HttpMessageConvertersAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
          DispatcherServletAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
  @Controller
  static class TestConfiguration implements ErrorPageRegistrar {

    @RequestMapping("/")
    String home() {
      throw new RuntimeException("Planned!");
    }

    @Override
    public void registerErrorPages(ErrorPageRegistry errorPageRegistry) {
      errorPageRegistry.addErrorPages(new ErrorPage("/infra/error"));
    }

    // For manual testing
    static void main(String[] args) {
      new ApplicationBuilder(TestConfiguration.class)
              .properties("web.mvc.servlet.path:infra/*")
              .run(args);
    }

  }

}

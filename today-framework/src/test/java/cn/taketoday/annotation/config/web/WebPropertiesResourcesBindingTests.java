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

package cn.taketoday.annotation.config.web;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import cn.taketoday.annotation.config.web.WebProperties.Resources;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Binding tests for {@link Resources}.
 *
 * @author Stephane Nicoll
 */
class WebPropertiesResourcesBindingTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(TestConfiguration.class);

  @Test
  void staticLocationsExpandArray() {
    this.contextRunner
            .withPropertyValues("web.resources.static-locations[0]=classpath:/one/",
                    "web.resources.static-locations[1]=classpath:/two",
                    "web.resources.static-locations[2]=classpath:/three/",
                    "web.resources.static-locations[3]=classpath:/four",
                    "web.resources.static-locations[4]=classpath:/five/",
                    "web.resources.static-locations[5]=classpath:/six")
            .run(assertResourceProperties((properties) -> assertThat(properties.staticLocations).contains(
                    "classpath:/one/", "classpath:/two/", "classpath:/three/", "classpath:/four/",
                    "classpath:/five/", "classpath:/six/")));
  }

  private ContextConsumer<AssertableApplicationContext> assertResourceProperties(Consumer<Resources> consumer) {
    return (context) -> {
      assertThat(context).hasSingleBean(WebProperties.class);
      consumer.accept(context.getBean(WebProperties.class).resources);
    };
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(WebProperties.class)
  static class TestConfiguration {

  }

}

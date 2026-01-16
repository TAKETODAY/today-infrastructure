/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.config.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ContextConsumer;
import infra.context.annotation.Configuration;
import infra.context.properties.EnableConfigurationProperties;
import infra.app.config.web.WebProperties.Resources;

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
      Assertions.assertThat(context).hasSingleBean(infra.app.config.web.WebProperties.class);
      consumer.accept(context.getBean(infra.app.config.web.WebProperties.class).resources);
    };
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(infra.app.config.web.WebProperties.class)
  static class TestConfiguration {

  }

}

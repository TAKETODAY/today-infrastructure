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

package infra.restclient.test.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestClientTest @RestClientTest} with a
 * {@link ConfigurationProperties @ConfigurationProperties} annotated type.
 *
 * @author Stephane Nicoll
 */
@RestClientTest(components = ExampleProperties.class, properties = "example.name=Hello")
class RestClientTestWithConfigurationPropertiesIntegrationTests {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void configurationPropertiesCanBeAddedAsComponent() {
    assertThat(this.applicationContext.getBeansOfType(ExampleProperties.class).keySet())
            .containsOnly("example-" + ExampleProperties.class.getName());
    assertThat(this.applicationContext.getBean(ExampleProperties.class).getName()).isEqualTo("Hello");
  }

}

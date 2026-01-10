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

package infra.context.properties;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBindException}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertiesBindExceptionTests {

  @Test
  void createFromBeanHasDetails() {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(Example.class);
    ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.get(applicationContext,
            applicationContext.getBean(Example.class), "example");
    ConfigurationPropertiesBindException exception = new ConfigurationPropertiesBindException(bean,
            new IllegalStateException());
    assertThat(exception.getNestedMessage()).isEqualTo("Error creating bean with name 'example': "
            + "Could not bind properties to 'ConfigurationPropertiesBindExceptionTests.Example' : "
            + "prefix=, ignoreInvalidFields=false, ignoreUnknownFields=true; "
            + "nested exception is java.lang.IllegalStateException");
    assertThat(exception.getBeanType()).isEqualTo(Example.class);
    assertThat(exception.getBeanName()).isEqualTo("example");
    assertThat(exception.getAnnotation()).isInstanceOf(ConfigurationProperties.class);
    assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
  }

  @Component("example")
  @ConfigurationProperties
  static class Example {

  }

}

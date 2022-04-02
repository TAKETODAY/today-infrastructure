/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.properties;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.lang.Component;

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
    assertThat(exception.getMessage()).isEqualTo("Error creating bean with name 'example': "
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

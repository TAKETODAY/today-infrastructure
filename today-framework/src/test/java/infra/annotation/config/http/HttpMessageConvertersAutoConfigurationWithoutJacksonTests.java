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

package infra.annotation.config.http;

import org.junit.jupiter.api.Test;

import infra.context.annotation.config.AutoConfigurations;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.http.converter.HttpMessageConverters;
import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpMessageConvertersAutoConfiguration} without Jackson on the
 * classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-*.jar")
class HttpMessageConvertersAutoConfigurationWithoutJacksonTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class));

  @Test
  void autoConfigurationWorksWithSpringHateoasButWithoutJackson() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(HttpMessageConverters.class));
  }

}

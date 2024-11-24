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

package infra.annotation.config.validation;

import org.junit.jupiter.api.Test;

import infra.context.annotation.config.AutoConfigurations;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.test.classpath.ClassPathExclusions;
import infra.validation.beanvalidation.MethodValidationPostProcessor;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ValidationAutoConfiguration} when Hibernate validator is present but no
 * EL implementation is available.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions({ "tomcat-embed-el-*.jar", "el-api-*.jar" })
class ValidationAutoConfigurationWithHibernateValidatorMissingElImplTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class));

  @Test
  void missingElDependencyIsTolerated() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(Validator.class);
      assertThat(context).hasSingleBean(MethodValidationPostProcessor.class);
    });
  }

}

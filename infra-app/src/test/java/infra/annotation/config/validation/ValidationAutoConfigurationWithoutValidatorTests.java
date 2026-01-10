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

package infra.annotation.config.validation;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.test.classpath.ClassPathExclusions;
import infra.validation.beanvalidation.MethodValidationPostProcessor;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ValidationAutoConfiguration} when no JSR-303 provider is available.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("hibernate-validator-*.jar")
class ValidationAutoConfigurationWithoutValidatorTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class));

  @Test
  void validationIsDisabled() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(Validator.class);
      assertThat(context).doesNotHaveBean(MethodValidationPostProcessor.class);
    });
  }

}

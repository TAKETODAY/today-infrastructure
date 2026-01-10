/*
 * Copyright 2002-present the original author or authors.
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

package infra.context.aot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.AbstractBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.StandardEnvironment;
import infra.core.io.ResourceLoader;
import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationContextInitializationCodeGenerator}.
 *
 * @author Stephane Nicoll
 */
class ApplicationContextInitializationCodeGeneratorTests {

  private static final ArgumentCodeGenerator argCodeGenerator = ApplicationContextInitializationCodeGenerator.
          createInitializerMethodArgumentCodeGenerator();

  @ParameterizedTest
  @MethodSource("methodArguments")
  void argumentsForSupportedTypesAreResolved(Class<?> target, String expectedArgument) {
    CodeBlock code = CodeBlock.of(expectedArgument);
    assertThat(argCodeGenerator.generateCode(ClassName.get(target))).isEqualTo(code);
  }

  @Test
  void argumentForUnsupportedBeanFactoryIsNotResolved() {
    assertThat(argCodeGenerator.generateCode(ClassName.get(AbstractBeanFactory.class))).isNull();
  }

  @Test
  void argumentForUnsupportedEnvironmentIsNotResolved() {
    assertThat(argCodeGenerator.generateCode(ClassName.get(StandardEnvironment.class))).isNull();
  }

  static Stream<Arguments> methodArguments() {
    String applicationContext = "applicationContext";
    String environment = applicationContext + ".getEnvironment()";
    return Stream.of(
            Arguments.of(StandardBeanFactory.class, "beanFactory"),
            Arguments.of(ConfigurableBeanFactory.class, "beanFactory"),
            Arguments.of(ConfigurableEnvironment.class, environment),
            Arguments.of(Environment.class, environment),
            Arguments.of(ResourceLoader.class, applicationContext));
  }

}

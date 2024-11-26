/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

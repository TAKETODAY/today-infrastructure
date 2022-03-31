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

package cn.taketoday.test.context.junit.jupiter;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class demonstrates how to have a JUnit Jupiter extension managed as a
 * Spring bean in order to have dependencies injected into an extension from
 * a Spring {@code ApplicationContext}.
 *
 * @author Sam Brannen
 * @since 5.1
 */
@JUnitConfig
@TestInstance(Lifecycle.PER_CLASS)
class SpringManagedJupiterExtensionTests {

  @Autowired
  @RegisterExtension
  TestTemplateInvocationContextProvider provider;

  @TestTemplate
  void testTemplate(String parameter) {
    assertThat("foo".equals(parameter) || "bar".equals(parameter)).isTrue();
  }

  @Configuration
  static class Config {

    @Bean
    String foo() {
      return "foo";
    }

    @Bean
    String bar() {
      return "bar";
    }

    @Bean
    TestTemplateInvocationContextProvider provider(List<String> parameters) {
      return new StringInvocationContextProvider(parameters);
    }
  }

  private static class StringInvocationContextProvider implements TestTemplateInvocationContextProvider {

    private final List<String> parameters;

    StringInvocationContextProvider(List<String> parameters) {
      this.parameters = parameters;
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
      return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
      return this.parameters.stream().map(this::invocationContext);
    }

    private TestTemplateInvocationContext invocationContext(String parameter) {
      return new TestTemplateInvocationContext() {

        @Override
        public String getDisplayName(int invocationIndex) {
          return parameter;
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
          return Collections.singletonList(new ParameterResolver() {

            @Override
            public boolean supportsParameter(ParameterContext parameterContext,
                    ExtensionContext extensionContext) {
              return parameterContext.getParameter().getType() == String.class;
            }

            @Override
            public Object resolveParameter(ParameterContext parameterContext,
                    ExtensionContext extensionContext) {
              return parameter;
            }
          });
        }
      };
    }
  }

}

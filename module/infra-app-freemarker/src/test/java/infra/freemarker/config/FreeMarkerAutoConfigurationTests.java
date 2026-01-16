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

package infra.freemarker.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringWriter;
import java.nio.file.Path;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.annotation.Order;
import infra.test.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
@ExtendWith(OutputCaptureExtension.class)
class FreeMarkerAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(FreeMarkerAutoConfiguration.class));

  @Test
  @WithResource(name = "templates/message.ftl", content = "Message: ${greeting}")
  void renderNonWebAppTemplate() {
    this.contextRunner.run((context) -> {
      var freemarker = context.getBean(freemarker.template.Configuration.class);
      StringWriter writer = new StringWriter();
      freemarker.getTemplate("message.ftl").process(new DataModel(), writer);
      assertThat(writer.toString()).contains("Hello World");
    });
  }

  @Test
  void nonExistentTemplateLocation(CapturedOutput output) {
    this.contextRunner
            .withPropertyValues("freemarker.templateLoaderPath:"
                    + "classpath:/does-not-exist/,classpath:/also-does-not-exist")
            .run((context) -> assertThat(output).contains("Cannot find template location"));
  }

  @Test
  void emptyTemplateLocation(CapturedOutput output, @TempDir Path tempDir) {
    this.contextRunner.withPropertyValues("freemarker.templateLoaderPath:file:" + tempDir.toAbsolutePath())
            .run((context) -> assertThat(output).doesNotContain("Cannot find template location"));
  }

  @Test
  void nonExistentLocationAndEmptyLocation(CapturedOutput output, @TempDir Path tempDir) {
    this.contextRunner.withPropertyValues("freemarker.templateLoaderPath:" + "classpath:/does-not-exist/,file:" + tempDir.toAbsolutePath())
            .run((context) -> assertThat(output).doesNotContain("Cannot find template location"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void variableCustomizersShouldBeAppliedInOrder() {
    this.contextRunner.withUserConfiguration(VariablesCustomizersConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(freemarker.template.Configuration.class);
      freemarker.template.Configuration configuration = context.getBean(freemarker.template.Configuration.class);
      assertThat(configuration.getSharedVariableNames()).contains("order", "one", "two");
      assertThat(configuration.getSharedVariable("order")).hasToString("5");
    });
  }

  public static class DataModel {

    public String getGreeting() {
      return "Hello World";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class VariablesCustomizersConfiguration {

    @Bean
    @Order(5)
    FreeMarkerVariablesCustomizer variablesCustomizer() {
      return (variables) -> {
        variables.put("order", 5);
        variables.put("one", "one");
      };
    }

    @Bean
    @Order(2)
    FreeMarkerVariablesCustomizer anotherVariablesCustomizer() {
      return (variables) -> {
        variables.put("order", 2);
        variables.put("two", "two");
      };
    }

  }
}

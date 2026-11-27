/*
 * Copyright 2025-present the original author or authors.
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

package infra.shell.jline.config;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;

import infra.app.InfraApplication;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfigurations;
import infra.shell.command.annotation.Command;
import infra.shell.config.InfraShellAutoConfiguration;
import infra.shell.jline.JLineInputProvider;
import infra.shell.jline.PromptProvider;
import infra.test.context.runner.ApplicationContextRunner;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomPromptProviderAutoConfigurationTests {

  @Test
  void testCustomPromptProviderAutoConfiguration() {
    // given
    ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SpringShellApplication.class)
            .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class, JLineShellAutoConfiguration.class))
            .withPropertyValues("shell.interactive.enabled=true");

    // when
    contextRunner.run(context -> {
      // then
      assertThat(context.getBeansOfType(JLineInputProvider.class)).hasSize(1);
      assertThat(context.getBeansOfType(PromptProvider.class))
              .withFailMessage("Prompt providers: %s", context.getBeansOfType(PromptProvider.class))
              .containsKey("myPromptProvider");
      JLineInputProvider jLineInputProvider = context.getBean(JLineInputProvider.class);
      PromptProvider promptProvider = (PromptProvider) ReflectionTestUtils.getField(jLineInputProvider,
              "promptProvider");
      AttributedString prompt = promptProvider.getPrompt();
      assertThat(prompt.toString()).isEqualTo("myprompt:>");
    });

  }

  @InfraApplication
  static class SpringShellApplication {

    @Command
    public void hi() {
      System.out.println("Hello world!");
    }

    @Bean
    public PromptProvider myPromptProvider() {
      return () -> new AttributedString("myprompt:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }

  }

}

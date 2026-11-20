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

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import infra.app.InfraApplication;
import infra.context.annotation.config.AutoConfigurations;
import infra.shell.command.annotation.Command;
import infra.shell.config.InfraShellAutoConfiguration;
import infra.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author David Pilar
 */
class JLineShellAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(SpringShellApplication.class)
          .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class, JLineShellAutoConfiguration.class))
          .withPropertyValues("shell.interactive.enabled=false");

  @Test
  void defaultsToInfraShellHistoryFile(@TempDir Path configDirectory) {
    this.contextRunner.withPropertyValues("shell.config.location=" + configDirectory).run(context -> {
      LineReader lineReader = context.getBean(LineReader.class);
      assertThat(lineReader.getVariable(LineReader.HISTORY_FILE)).isEqualTo(configDirectory.resolve("infra-shell.log"));
    });
  }

  @Test
  void terminalIsReusedAcrossContextsAndSurvivesContextClose() {
    AtomicReference<Terminal> firstTerminal = new AtomicReference<>();
    this.contextRunner.run(context -> firstTerminal.set(context.getBean(Terminal.class)));

    // First context is closed; the shared terminal must survive so a restart reuses
    // it.
    this.contextRunner.run(context -> {
      Terminal terminal = context.getBean(Terminal.class);
      assertThat(terminal).isSameAs(firstTerminal.get());
      // getAttributes() throws if the terminal has been closed
      assertThatCode(terminal::getAttributes).doesNotThrowAnyException();
    });
  }

  @InfraApplication
  static class SpringShellApplication {

    @Command
    void hi() {
      System.out.println("Hello world!");
    }

  }

}

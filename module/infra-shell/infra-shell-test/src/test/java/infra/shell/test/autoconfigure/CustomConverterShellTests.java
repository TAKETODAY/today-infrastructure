/*
 * Copyright 2026-present the original author or authors.
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

package infra.shell.test.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import infra.app.InfraApplication;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.core.conversion.Converter;
import infra.shell.command.CommandContext;
import infra.shell.command.CommandOption;
import infra.shell.command.annotation.Command;
import infra.shell.command.annotation.Option;
import infra.shell.test.ShellAssertions;
import infra.shell.test.ShellScreen;
import infra.shell.test.ShellTestClient;
import infra.test.context.ContextConfiguration;

/**
 * Regression coverage for gh-1352. Mirrors the scenarios from the issue reproducer: an
 * annotation-based command with a custom-typed option, an annotation-based command with a
 * plain String option, and a programmatically-registered command, all running against a
 * context that defines a {@code Converter<String, Message>} bean.
 *
 * @author David Pilar
 */
@ShellTest
@ContextConfiguration(classes = CustomConverterShellTests.App.class)
class CustomConverterShellTests {

  @Test
  void customConverterIsAppliedToAnnotatedCommandOption(@Autowired ShellTestClient client) throws Exception {
    ShellScreen shellScreen = client.sendCommand("custom --msg hello");

    ShellAssertions.assertThat(shellScreen).containsText("hello");
  }

  @Test
  void plainStringOptionStillWorks(@Autowired ShellTestClient client) throws Exception {
    ShellScreen shellScreen = client.sendCommand("simple --msg hello");

    ShellAssertions.assertThat(shellScreen).containsText("hello");
  }

  @Test
  void programmaticCommandStillWorks(@Autowired ShellTestClient client) throws Exception {
    ShellScreen shellScreen = client.sendCommand("programmatic --msg hello");

    ShellAssertions.assertThat(shellScreen).containsText("hello");
  }

  @InfraApplication
  static class App {

    @Command(name = "custom")
    public String custom(@Option(longName = "msg", required = true) Message msg) {
      return msg.getText();
    }

    @Command(name = "simple")
    public String simple(@Option(longName = "msg", required = true) String msg) {
      return msg;
    }

    @Bean
    infra.shell.command.Command programmatic() {
      Function<CommandContext, String> action = ctx -> ctx.getOptionByName("msg").value();
      return infra.shell.command.Command.builder()
              .name("programmatic")
              .options(CommandOption.with().longName("msg").type(Message.class).build())
              .execute(action);
    }

    @Bean
    Converter<String, Message> messageConverter() {
      return source -> {
        Message m = new Message();
        m.setText(source);
        return m;
      };
    }

  }

  static class Message {

    private String text;

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

  }

}

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

package infra.shell.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRegistryTests {

  private final CommandRegistry commandRegistry = new CommandRegistry();

  @Test
  void getCommandByName() {
    // given
    commandRegistry.registerCommand(new Command() {

      @Override
      public String getName() {
        return "cmd1";
      }

      @Override
      public ExitStatus execute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    });
    commandRegistry.registerCommand(new Command() {

      @Override
      public String getName() {
        return "cmd2";
      }

      @Override
      public boolean isHidden() {
        return true;
      }

      @Override
      public ExitStatus execute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    });

    // when
    Command cmd1 = commandRegistry.getCommandByName("cmd1");
    Command cmd2 = commandRegistry.getCommandByName("cmd2");
    Command cmd3 = commandRegistry.getCommandByName("cmd3");

    // then
    assertThat(cmd1).isNotNull();
    assertThat(cmd2).isNull(); // hidden command should not be returned
    assertThat(cmd3).isNull(); // non-existing command should return null
  }

  @Test
  void getCommandsByPrefix() {
    // given
    commandRegistry.registerCommand(new Command() {

      @Override
      public String getName() {
        return "start";
      }

      @Override
      public ExitStatus execute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    });
    commandRegistry.registerCommand(new Command() {

      @Override
      public String getName() {
        return "status";
      }

      @Override
      public ExitStatus execute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    });
    commandRegistry.registerCommand(new Command() {

      @Override
      public String getName() {
        return "stop";
      }

      @Override
      public boolean isHidden() {
        return true;
      }

      @Override
      public ExitStatus execute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    });

    // when
    var commands = commandRegistry.getCommandsByPrefix("st");

    // then
    assertThat(commands.size()).isEqualTo(2);
    assertThat(commands.stream().anyMatch(cmd -> cmd.getName().equals("start"))).isTrue();
    assertThat(commands.stream().anyMatch(cmd -> cmd.getName().equals("status"))).isTrue();
  }

  @Test
  void getCommandByAlias() {
    // given
    commandRegistry.registerCommand(new Command() {

      @Override
      public String getName() {
        return "list";
      }

      @Override
      public java.util.List<String> getAliases() {
        return java.util.List.of("ls", "dir");
      }

      @Override
      public ExitStatus execute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    });

    // when
    Command cmdLs = commandRegistry.getCommandByAlias("ls");
    Command cmdDir = commandRegistry.getCommandByAlias("dir");
    Command cmdUnknown = commandRegistry.getCommandByAlias("unknown");

    // then
    assertThat(cmdLs).isNotNull();
    assertThat(cmdLs.getName()).isEqualTo("list");
    assertThat(cmdDir).isNotNull();
    assertThat(cmdDir.getName()).isEqualTo("list");
    assertThat(cmdUnknown).isNull();
  }

}

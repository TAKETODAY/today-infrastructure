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

package infra.shell.command.adapter;

import java.util.function.Consumer;

import infra.shell.command.AbstractCommand;
import infra.shell.command.CommandContext;
import infra.shell.command.ExitStatus;

/**
 * An adapter to adapt a {@link Consumer} as a command.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class ConsumerCommandAdapter extends AbstractCommand {

  Consumer<CommandContext> commandExecutor;

  /**
   * Create a new {@link ConsumerCommandAdapter}.
   *
   * @param name the name of the command
   * @param description the description of the command
   * @param group the group of the command
   * @param help the help text of the command
   * @param hidden whether the command is hidden or not
   * @param commandExecutor the consumer to adapt as a command
   */
  public ConsumerCommandAdapter(String name, String description, String group, String help, boolean hidden,
          Consumer<CommandContext> commandExecutor) {
    super(name, description, group, help, hidden);
    this.commandExecutor = commandExecutor;
  }

  @Override
  public ExitStatus doExecute(CommandContext commandContext) {
    this.commandExecutor.accept(commandContext);
    return ExitStatus.OK;
  }

}

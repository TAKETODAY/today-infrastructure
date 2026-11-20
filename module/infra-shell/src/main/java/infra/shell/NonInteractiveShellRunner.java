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

package infra.shell;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.shell.command.CommandContext;
import infra.shell.command.CommandExecutionException;
import infra.shell.command.CommandExecutor;
import infra.shell.command.CommandNotFoundException;
import infra.shell.command.CommandParser;
import infra.shell.command.CommandRegistry;
import infra.shell.command.ExitStatus;
import infra.shell.command.ParsedInput;

/**
 * A {@link ShellRunner} that executes commands without entering interactive shell mode.
 * It can process a single command, or a script of commands defined in a file. The script
 * file is specified by prefixing the file name with the special character {@literal @}.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class NonInteractiveShellRunner implements ShellRunner {

  private static final Logger log = LoggerFactory.getLogger(NonInteractiveShellRunner.class);

  private final CommandParser commandParser;

  private final CommandExecutor commandExecutor;

  private final CommandRegistry commandRegistry;

  // Use System.out as default output writer
  private final PrintWriter outputWriter;

  // Use a no-op InputReader since input is not needed in non-interactive mode
  private final InputReader inputReader = new InputReader() {
  };

  /**
   * Create a new {@link NonInteractiveShellRunner} instance.
   *
   * @param commandParser the command parser
   * @param commandRegistry the command registry
   */
  public NonInteractiveShellRunner(CommandParser commandParser, CommandRegistry commandRegistry) {
    this(commandParser, commandRegistry, new PrintWriter(System.out));
  }

  /**
   * Create a new {@link NonInteractiveShellRunner} instance.
   *
   * @param commandParser the command parser
   * @param commandRegistry the command registry
   * @param outputWriter the output writer
   * @since 4.0.1
   */
  public NonInteractiveShellRunner(CommandParser commandParser, CommandRegistry commandRegistry,
          PrintWriter outputWriter) {
    this.commandParser = commandParser;
    this.commandRegistry = commandRegistry;
    this.commandExecutor = new CommandExecutor(commandRegistry);
    this.outputWriter = outputWriter;
  }

  @Override
  public void run(String[] args) {
    if (args.length == 0) {
      log.debug("No non-interactive shell command supplied");
      return;
    }
    ExitStatus status = execute(args);
    if (status.code() != ExitStatus.OK.code()) {
      throw new CommandExecutionException(status.description(), status.code());
    }
  }

  /**
   * Execute a command or an {@code @file} script without exiting the JVM.
   * Individual command-line arguments containing whitespace are kept intact.
   *
   * @param args the command-line arguments
   * @return the status of the command or the first failure in a script
   */
  public ExitStatus execute(String... args) {
    if (args.length == 0) {
      return failure(ExitStatus.USAGE_ERROR, "Expected a command or an @file script");
    }
    if (args[0].startsWith("@")) {
      return executeScript(new File(args[0].substring(1)));
    }
    StringBuilder input = new StringBuilder();
    for (String arg : args) {
      if (!input.isEmpty()) {
        input.append(' ');
      }
      int equals = arg.indexOf('=');
      if (arg.startsWith("-") && equals >= 0) {
        input.append(arg, 0, equals + 1).append(quote(arg.substring(equals + 1)));
      }
      else {
        input.append(quote(arg));
      }
    }
    return executeLine(input.toString());
  }

  /**
   * Execute a shell command expressed as a line of text, including shell quoting.
   *
   * @param input the command line
   * @return the command's exit status
   */
  public ExitStatus executeLine(String input) {
    try {
      ParsedInput parsedInput = this.commandParser.parse(input);
      return this.commandExecutor.execute(new CommandContext(parsedInput, this.commandRegistry,
              this.outputWriter, this.inputReader));
    }
    catch (IllegalArgumentException ex) {
      return failure(ExitStatus.USAGE_ERROR, ex.getMessage());
    }
    catch (CommandNotFoundException ex) {
      return failure(ExitStatus.USAGE_ERROR, "Unknown command: " + ex.getCommandName());
    }
    catch (CommandExecutionException ex) {
      log.debug("Command execution failed", ex);
      return failure(new ExitStatus(ex.getExitCode(), ex.getMessage()), ex.getMessage());
    }
    finally {
      this.outputWriter.flush();
    }
  }

  private static String quote(String arg) {
    if (!arg.isEmpty() && arg.chars().noneMatch(c -> Character.isWhitespace(c) || c == '\'' || c == '"')) {
      return arg;
    }
    return '"' + arg.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
  }

  private ExitStatus executeScript(File script) {
    try (FileInputProvider inputProvider = new FileInputProvider(script)) {
      String input;
      while ((input = inputProvider.readInput()) != null) {
        if (!input.isBlank()) {
          ExitStatus status = executeLine(input);
          if (status.code() != ExitStatus.OK.code()) {
            return status;
          }
        }
      }
      return ExitStatus.OK;
    }
    catch (IOException ex) {
      log.debug("Unable to read script", ex);
      return failure(ExitStatus.EXECUTION_ERROR, "Unable to read script: " + script);
    }
  }

  private ExitStatus failure(ExitStatus status, String message) {
    this.outputWriter.println(message);
    this.outputWriter.flush();
    return new ExitStatus(status.code(), message);
  }

}

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

package infra.shell.jline;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.shell.ShellConfigurationException;
import infra.shell.ShellRunner;
import infra.shell.command.CommandRegistry;
import infra.shell.command.DefaultCommandParser;

@Configuration
public class DefaultJLineShellConfiguration {

  @Bean
  public ShellRunner shellRunner(JLineInputProvider inputProvider, CommandRegistry commandRegistry) {
    return new JLineShellRunner(inputProvider, new DefaultCommandParser(commandRegistry), commandRegistry);
  }

  @Bean
  public JLineInputProvider inputProvider(LineReader lineReader) {
    return new JLineInputProvider(lineReader);
  }

  @Bean
  public LineReader lineReader(Terminal terminal, Parser parser, CommandCompleter commandCompleter) {
    return LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(commandCompleter)
            .parser(parser)
            // keep backslashes in the returned line, escapes are resolved by the
            // command parser
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .build();
  }

  @Bean
  public CommandCompleter commandCompleter(CommandRegistry commandRegistry) {
    return new CommandCompleter(commandRegistry);
  }

  @Bean
  public Terminal terminal() {
    try {
      return TerminalBuilder.builder().build();
    }
    catch (IOException e) {
      throw new ShellConfigurationException("Unable to configure JLine terminal", e);
    }
  }

  @Bean
  public Parser parser() {
    ExtendedDefaultParser parser = new ExtendedDefaultParser();
    parser.setEofOnUnclosedQuote(true);
    parser.setEofOnEscapedNewLine(true);
    return parser;
  }

}

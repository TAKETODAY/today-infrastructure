/*
 * Copyright 2017-present the original author or authors.
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
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.TerminalBuilder.SystemOutput;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.Value;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.event.ContextClosedEvent;
import infra.context.event.EventListener;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.Environment;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.shell.NonInteractiveShellRunner;
import infra.shell.ShellRunner;
import infra.shell.command.Command;
import infra.shell.command.CommandParser;
import infra.shell.command.CommandRegistry;
import infra.shell.command.annotation.EnableCommand;
import infra.shell.config.InfraShellProperties;
import infra.shell.config.UserConfigPathProvider;
import infra.shell.jline.CommandCompleter;
import infra.shell.jline.CommandHighlighter;
import infra.shell.jline.ExtendedDefaultParser;
import infra.shell.jline.JLineInputProvider;
import infra.shell.jline.JLineShellRunner;
import infra.shell.jline.PromptProvider;
import infra.shell.jline.command.History;
import infra.util.StringUtils;

/**
 * Shell implementation using JLine to capture input and trigger completions.
 *
 * @author Eric Bottard
 * @author Florent Biville
 * @author Mahmoud Ben Hassine
 * @author Piotr Olaszewski
 * @author David Pilar
 */
@AutoConfiguration
@EnableConfigurationProperties(InfraShellProperties.class)
@ConditionalOnClass(org.jline.reader.History.class)
@ConditionalOnMissingBean(annotation = EnableCommand.class)
@ImportAutoConfiguration(classes = { ThemingAutoConfiguration.class, TerminalUIAutoConfiguration.class,
        ComponentFlowAutoConfiguration.class })
public class JLineShellAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(JLineShellAutoConfiguration.class);

  // Reused across context restarts (e.g. DevTools) to keep one reader of stdin.
  private static volatile Terminal sharedTerminal;

  private static final Object sharedTerminalLock = new Object();

  private org.jline.reader.History jLineHistory;

  @Value("${app.name:infra-shell}.log")
  private String fallbackHistoryFileName = "infra-shell.log";

  private InfraShellProperties infraShellProperties;

  private UserConfigPathProvider userConfigPathProvider;

  public JLineShellAutoConfiguration(org.jline.reader.History jLineHistory,
          InfraShellProperties infraShellProperties, UserConfigPathProvider userConfigPathProvider) {
    this.jLineHistory = jLineHistory;
    this.infraShellProperties = infraShellProperties;
    this.userConfigPathProvider = userConfigPathProvider;
  }

  @EventListener
  public void onContextClosedEvent(ContextClosedEvent event) throws IOException {
    jLineHistory.save();
  }

  @Bean
  @ConditionalOnProperty(prefix = "shell.interactive", name = "enabled", havingValue = "true",
          matchIfMissing = true)
  public ShellRunner jlineShellRunner(JLineInputProvider inputProvider, CommandParser commandParser,
          CommandRegistry commandRegistry, Environment environment) {
    JLineShellRunner jLineShellRunner = new JLineShellRunner(inputProvider, commandParser, commandRegistry);
    Boolean debugMode = environment.getProperty("shell.debug.enabled", Boolean.class, false);
    jLineShellRunner.setDebugMode(debugMode);
    return jLineShellRunner;
  }

  @Bean
  @ConditionalOnProperty(prefix = "shell.interactive", name = "enabled", havingValue = "false")
  public ShellRunner jlineNonInteractiveShellRunner(CommandParser commandParser, CommandRegistry commandRegistry,
          Terminal terminal) {
    return new NonInteractiveShellRunner(commandParser, commandRegistry, terminal.writer());
  }

  @Bean
  public LineReader lineReader(Terminal terminal, Parser parser, CommandCompleter commandCompleter,
          CommandHighlighter commandHighlighter) {
    LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder()
            .terminal(terminal)
            .appName("Infra Shell")
            .completer(commandCompleter)
            .history(jLineHistory)
            .highlighter(commandHighlighter)
            .parser(parser)
            // keep backslashes in the returned line, escapes are resolved by the
            // command parser
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

    LineReader lineReader = lineReaderBuilder.build();
    if (this.infraShellProperties.getHistory().isEnabled()) {
      // Discover history location
      Path userConfigPath = this.userConfigPathProvider.provide();
      log.debug("Resolved userConfigPath " + userConfigPath);
      String historyFileName = this.infraShellProperties.getHistory().getName();
      if (!StringUtils.hasText(historyFileName)) {
        historyFileName = fallbackHistoryFileName;
      }
      log.debug("Resolved historyFileName " + historyFileName);
      String historyPath = userConfigPath.resolve(historyFileName).toAbsolutePath().toString();
      log.debug("Resolved historyPath " + historyPath);
      // set history file
      lineReader.setVariable(LineReader.HISTORY_FILE, Paths.get(historyPath));
    }
    lineReader.unsetOpt(LineReader.Option.INSERT_TAB); // This allows completion on an
    // empty buffer, rather than
    // inserting a tab
    jLineHistory.attach(lineReader);
    return lineReader;
  }

  @Bean
  public JLineInputProvider inputProvider(LineReader lineReader, ObjectProvider<PromptProvider> promptProviders) {
    JLineInputProvider inputProvider = new JLineInputProvider(lineReader);
    PromptProvider promptProvider = promptProviders.getIfAvailable();
    if (promptProvider != null) {
      inputProvider.setPromptProvider(promptProvider);
    }
    return inputProvider;
  }

  // Not closed on context close (shared); closed on JVM shutdown instead.
  @Bean(destroyMethod = "")
  public Terminal terminal(ObjectProvider<TerminalCustomizer> customizers) {
    Terminal terminal = sharedTerminal;
    if (terminal != null) {
      return terminal;
    }
    synchronized(sharedTerminalLock) {
      if (sharedTerminal == null) {
        try {
          TerminalBuilder builder = TerminalBuilder.builder();
          builder.systemOutput(SystemOutput.SysOut);
          customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
          Terminal built = builder.build();
          Runtime.getRuntime()
                  .addShutdownHook(new Thread(() -> closeTerminal(built), "infra-shell-terminal-close"));
          sharedTerminal = built;
        }
        catch (IOException e) {
          throw new BeanCreationException("Could not create Terminal", e);
        }
      }
      return sharedTerminal;
    }
  }

  private static void closeTerminal(Terminal terminal) {
    try {
      terminal.close();
    }
    catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to close terminal on shutdown", ex);
      }
    }
  }

  @Bean
  public Parser parser() {
    ExtendedDefaultParser parser = new ExtendedDefaultParser();
    parser.setEofOnUnclosedQuote(true);
    parser.setEofOnEscapedNewLine(true);
    return parser;
  }

  @Bean
  @ConditionalOnMissingBean
  public CommandHighlighter commandHighlighter(CommandRegistry commandRegistry) {
    return new CommandHighlighter(commandRegistry);
  }

  @Bean
  @ConditionalOnMissingBean
  public CommandCompleter commandCompleter(CommandRegistry commandRegistry) {
    return new CommandCompleter(commandRegistry);
  }

  @Bean
  @ConditionalOnProperty(value = "shell.command.history.enabled", havingValue = "true", matchIfMissing = true)
  public Command historyCommand(org.jline.reader.History jLineHistory) {
    return new History(jLineHistory);
  }

  @Configuration
  @ConditionalOnMissingBean(org.jline.reader.History.class)
  public static class JLineHistoryConfiguration {

    @Bean
    public org.jline.reader.History history() {
      return new DefaultHistory();
    }

  }

}

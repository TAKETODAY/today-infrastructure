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

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import infra.shell.command.Command;
import infra.shell.command.CommandOption;
import infra.shell.command.CommandRegistry;
import infra.shell.command.completion.CompletionContext;
import infra.shell.command.completion.CompletionProposal;
import infra.shell.command.completion.CompletionProvider;
import infra.shell.utils.Utils;
import infra.util.StringUtils;

/**
 * A JLine {@link Completer} that completes command names from a {@link CommandRegistry}.
 *
 * @author Mahmoud Ben Hassine
 * @author David Pilar
 * @since 5.0
 */
public class CommandCompleter implements Completer {

  private final CommandRegistry commandRegistry;

  /**
   * Create a new {@link CommandCompleter} instance.
   *
   * @param commandRegistry the command registry
   */
  public CommandCompleter(CommandRegistry commandRegistry) {
    this.commandRegistry = commandRegistry;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    Command commandByName = findCommandByWords(line.words());
    if (commandByName != null) {
      List<CommandOption> options = commandByName.getOptions();
      CommandOption commandOption = findOptionByWords(line.words(), options);
      if (commandOption == null) {
        // add option completions for the command
        for (CommandOption option : options) {
          boolean present = isOptionPresent(line, option);
          if (StringUtils.isNotEmpty(option.longName()) && !present) {
            candidates.add(new Candidate("--" + option.longName()));
          }
          if (option.shortName() != ' ' && !present) {
            candidates.add(new Candidate("-" + option.shortName()));
          }
        }
      }
      // add custom completions from the command's completion provider
      CompletionProvider completionProvider = commandByName.getCompletionProvider();
      CompletionContext context = new CompletionContext(line.words(), line.wordIndex(), line.wordCursor(),
              commandByName, commandOption);
      List<CompletionProposal> proposals = completionProvider.apply(context);
      for (CompletionProposal proposal : proposals) {
        candidates.add(new Candidate(proposal.value(), proposal.displayText(), proposal.category(),
                proposal.description(), null, null, proposal.complete(), 0));
      }
    }
    else {
      this.commandRegistry.getCommandsByPrefix(line.line())
              .stream()
              .map(command -> toCommandCandidates(command, line.words()))
              .flatMap(List::stream)
              .sorted(Candidate::compareTo)
              .forEach(candidates::add);
    }
  }

  private List<Candidate> toCommandCandidates(Command command, List<String> words) {
    String prefix = words.size() > 1 ? String.join(" ", words.subList(0, words.size() - 1)) : "";
    return getCommandNames(command).filter(name -> name.startsWith(words.get(0)))
            .filter(name -> name.startsWith(prefix))
            .map(cmd -> new Candidate(cmd.substring(prefix.length()).trim(), cmd + ": " + command.getDescription(),
                    command.getGroup(), null, null, null, !Utils.QUIT_COMMAND.equals(command)))
            .toList();
  }

  private boolean isOptionPresent(ParsedLine line, CommandOption option) {
    return option.longName() != null
            && (line.line().contains(" --" + option.longName() + " ")
            || line.line().contains(" --" + option.longName() + "="))
            || option.shortName() != ' ' && (line.line().contains(" -" + option.shortName() + " ")
            || line.line().contains(" -" + option.shortName() + "="));
  }

  @Nullable
  private Command findCommandByWords(List<String> words) {
    Command match = null;
    StringBuilder commandName = new StringBuilder();
    for (String word : words) {
      if (word.startsWith("-")) {
        break;
      }
      commandName.append(word).append(" ");
      Command candidate = this.commandRegistry.getCommandByName(commandName.toString().trim());
      if (candidate != null) {
        match = candidate;
      }
    }
    // the command is found but was not completed on the line
    if (match != null && getCommandNames(match).toList().contains(String.join(" ", words))) {
      match = null;
    }
    return match;
  }

  @Nullable
  private CommandOption findOptionByWords(List<String> words, List<CommandOption> options) {
    List<String> reversed = new ArrayList<>(words);
    Collections.reverse(reversed);

    CommandOption option;
    if (reversed.get(0).isEmpty()) {
      // the option name was completed, but no value provided ---> "--optionName "
      option = findOption(options, o -> o.isOptionEqual(reversed.get(1)));
    }
    else {
      // the option uses key-value pair ---> "--optionName=someValue"
      option = findOption(options, o -> isOptionStartWith(reversed.get(0), o));

      // the option uses completion on the value level ---> "--optionName someValue"
      if (option == null) {
        option = findOption(options, o -> o.isOptionEqual(reversed.get(1)));
      }
    }

    return option;
  }

  @Nullable
  private CommandOption findOption(List<CommandOption> options, Predicate<CommandOption> optionFilter) {
    return options.stream().filter(optionFilter).findFirst().orElse(null);
  }

  private static boolean isOptionStartWith(String optionName, CommandOption option) {
    return StringUtils.isNotEmpty(option.longName()) && optionName.startsWith("--" + option.longName() + "=")
            || option.shortName() != ' ' && optionName.startsWith("-" + option.shortName() + "=");
  }

  private Stream<String> getCommandNames(Command command) {
    return Stream.concat(Stream.of(command.getName()), command.getAliases().stream());
  }

}

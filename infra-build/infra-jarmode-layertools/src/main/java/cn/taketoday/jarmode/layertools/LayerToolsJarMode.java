/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jarmode.layertools;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import cn.taketoday.app.loader.jarmode.JarMode;

/**
 * {@link JarMode} providing {@code "layertools"} support.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LayerToolsJarMode implements JarMode {

  @Override
  public boolean accepts(String mode) {
    return "layertools".equalsIgnoreCase(mode);
  }

  @Override
  public void run(String mode, String[] args) {
    try {
      new Runner().run(args);
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  static class Runner {

    static Context contextOverride;

    private final List<Command> commands;

    private final HelpCommand help;

    Runner() {
      Context context = (contextOverride != null) ? contextOverride : new Context();
      this.commands = getCommands(context);
      this.help = new HelpCommand(context, this.commands);
    }

    private void run(String[] args) {
      run(dequeOf(args));
    }

    private void run(Deque<String> args) {
      if (!args.isEmpty()) {
        String commandName = args.removeFirst();
        Command command = Command.find(this.commands, commandName);
        if (command != null) {
          runCommand(command, args);
          return;
        }
        printError("Unknown command \"" + commandName + "\"");
      }
      this.help.run(args);
    }

    private void runCommand(Command command, Deque<String> args) {
      try {
        command.run(args);
      }
      catch (UnknownOptionException ex) {
        printError("Unknown option \"" + ex.getMessage() + "\" for the " + command.getName() + " command");
        this.help.run(dequeOf(command.getName()));
      }
      catch (MissingValueException ex) {
        printError("Option \"" + ex.getMessage() + "\" for the " + command.getName()
                + " command requires a value");
        this.help.run(dequeOf(command.getName()));
      }
    }

    private void printError(String errorMessage) {
      System.out.println("Error: " + errorMessage);
      System.out.println();
    }

    private Deque<String> dequeOf(String... args) {
      return new ArrayDeque<>(Arrays.asList(args));
    }

    static List<Command> getCommands(Context context) {
      return List.of(new ListCommand(context), new ExtractCommand(context));
    }

  }

}

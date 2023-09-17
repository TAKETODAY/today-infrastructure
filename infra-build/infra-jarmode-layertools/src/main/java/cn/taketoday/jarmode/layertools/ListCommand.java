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

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * The {@code 'list'} tools command.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ListCommand extends Command {

  private final Context context;

  ListCommand(Context context) {
    super("list", "List layers from the jar that can be extracted", Options.none(), Parameters.none());
    this.context = context;
  }

  @Override
  protected void run(Map<Option, String> options, List<String> parameters) {
    printLayers(Layers.get(this.context), System.out);
  }

  void printLayers(Layers layers, PrintStream out) {
    layers.forEach(out::println);
  }

}

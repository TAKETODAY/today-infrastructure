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

package cn.taketoday.buildpack.platform.build;

import java.io.PrintStream;
import java.util.function.Consumer;

import cn.taketoday.buildpack.platform.docker.TotalProgressBar;
import cn.taketoday.buildpack.platform.docker.TotalProgressEvent;

/**
 * {@link BuildLog} implementation that prints output to a {@link PrintStream}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BuildLog#to(PrintStream)
 * @since 4.0
 */
class PrintStreamBuildLog extends AbstractBuildLog {

  private final PrintStream out;

  PrintStreamBuildLog(PrintStream out) {
    this.out = out;
  }

  @Override
  protected void log(String message) {
    this.out.println(message);
  }

  @Override
  protected Consumer<TotalProgressEvent> getProgressConsumer(String prefix) {
    return new TotalProgressBar(prefix, '.', false, this.out);
  }

}

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

package cn.taketoday.infra.maven;

import org.apache.maven.plugin.logging.Log;

import java.util.function.Supplier;

import cn.taketoday.app.loader.tools.Packager;

/**
 * {@link Packager.MainClassTimeoutWarningListener} backed by a supplied Maven {@link Log}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class LoggingMainClassTimeoutWarningListener implements Packager.MainClassTimeoutWarningListener {

  private final Supplier<Log> log;

  LoggingMainClassTimeoutWarningListener(Supplier<Log> log) {
    this.log = log;
  }

  @Override
  public void handleTimeoutWarning(long duration, String mainMethod) {
    this.log.get()
            .warn("Searching for the main-class is taking some time, "
                    + "consider using the mainClass configuration parameter");
  }

}

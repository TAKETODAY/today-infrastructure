/*
 * Copyright 2012-present the original author or authors.
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

package infra.maven;

import org.apache.maven.plugin.logging.Log;

import java.util.function.Supplier;

import infra.app.loader.tools.Packager;

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

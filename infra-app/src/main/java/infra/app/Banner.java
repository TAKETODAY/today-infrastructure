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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.io.PrintStream;

import infra.core.env.Environment;

/**
 * Interface class for writing a banner programmatically.
 *
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Jeremy Rickard
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:56
 */
@FunctionalInterface
public interface Banner {
  String BEAN_NAME = "applicationBanner";

  String BANNER_CHARSET = "banner.charset";
  String BANNER_LOCATION_TXT = "banner.txt";
  String BANNER_LOCATION = "banner.location";

  /**
   * Print the banner to the specified print stream.
   *
   * @param environment the application environment
   * @param sourceClass the source class for the application
   * @param out the output print stream
   */
  void printBanner(Environment environment, @Nullable Class<?> sourceClass, PrintStream out);

  /**
   * An enumeration of possible values for configuring the Banner.
   */
  enum Mode {

    /**
     * Disable printing of the banner.
     */
    OFF,

    /**
     * Print the banner to System.out.
     */
    CONSOLE,

    /**
     * Print the banner to the log file.
     */
    LOG

  }

}

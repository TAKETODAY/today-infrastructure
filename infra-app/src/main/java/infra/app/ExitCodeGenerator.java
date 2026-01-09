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

import infra.context.ApplicationContext;

/**
 * Interface used to generate an 'exit code' from a running command line
 * {@link Application}. Can be used on exceptions as well as directly on beans.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Application#exit(ApplicationContext, ExitCodeGenerator...)
 * @since 4.0 2022/1/16 20:36
 */
@FunctionalInterface
public interface ExitCodeGenerator {

  /**
   * Returns the exit code that should be returned from the application.
   *
   * @return the exit code.
   */
  int getExitCode();

}


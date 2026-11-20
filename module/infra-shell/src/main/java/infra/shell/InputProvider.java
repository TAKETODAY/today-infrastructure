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

package infra.shell;

import org.jspecify.annotations.Nullable;

/**
 * To be implemented by components able to provide a "line" of user input, whether
 * interactively or by batch.
 *
 * @author Eric Bottard
 * @author Piotr Olaszewski
 * @author Mahmoud Ben Hassine
 */
public interface InputProvider {

  /**
   * Return text entered by user to invoke commands.
   *
   * @return user input line or {@literal null} if end of input is reached
   * @throws Exception on error
   */
  @Nullable String readInput() throws Exception;

}

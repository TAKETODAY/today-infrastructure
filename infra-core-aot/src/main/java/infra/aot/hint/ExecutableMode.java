/*
 * Copyright 2002-present the original author or authors.
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

package infra.aot.hint;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Executable;

/**
 * Represent the need of reflection for a given {@link Executable}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public enum ExecutableMode {

  /**
   * Only retrieving the {@link Executable} and its metadata is required.
   */
  INTROSPECT,

  /**
   * Full reflection support is required, including the ability to invoke
   * the {@link Executable}.
   */
  INVOKE;

  /**
   * Specify if this mode already includes the specified {@code other} mode.
   *
   * @param other the other mode to check
   * @return {@code true} if this mode includes the other mode
   */
  boolean includes(@Nullable ExecutableMode other) {
    return (other == null || this.ordinal() >= other.ordinal());
  }

}

/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.util;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface used to resolve replacement values for placeholders contained in Strings.
 *
 * @author TODAY 2021/10/3 13:24
 */
@FunctionalInterface
public interface PlaceholderResolver {

  /**
   * Resolve the supplied placeholder name to the replacement value.
   *
   * @param placeholderName the name of the placeholder to resolve
   * @return the replacement value, or {@code null} if no replacement is to be made
   */
  @Nullable
  String resolvePlaceholder(String placeholderName);
}

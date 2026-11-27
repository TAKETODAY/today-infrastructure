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

package infra.shell.command.completion;

import java.util.Arrays;
import java.util.List;

/**
 * A completion provider that knows how to complete values from enumerations.
 * <p>
 * This provider can also add a prefix before each enum constant, useful when enum values
 * are expected to be provided as key=value pairs for options.
 *
 * @author Eric Bottard
 * @author Mahmoud Ben Hassine (adapted from EnumValueProvider in v3)
 * @since 5.0
 */
public class EnumCompletionProvider implements CompletionProvider {

  private final Class<?> enumType;

  private String prefix = "";

  /**
   * Create a new {@link EnumCompletionProvider} for the given enum type.
   *
   * @param enumType the enum type
   */
  public EnumCompletionProvider(Class<?> enumType) {
    this.enumType = enumType;
  }

  /**
   * Create a new {@link EnumCompletionProvider} for the given enum type with a prefix.
   *
   * @param enumType the enum type
   * @param prefix the prefix to be added before each enum constant
   */
  public EnumCompletionProvider(Class<?> enumType, String prefix) {
    this.enumType = enumType;
    this.prefix = prefix;
  }

  @Override
  public List<CompletionProposal> apply(CompletionContext completionContext) {
    Object[] enumConstants = enumType.getEnumConstants();
    return Arrays.stream(enumConstants)
            .map(enumConstant -> prefix.isEmpty() ? enumConstant.toString() : prefix + "=" + enumConstant)
            .map(CompletionProposal::new)
            .toList();
  }

}

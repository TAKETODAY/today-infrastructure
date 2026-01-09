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

package infra.core.conversion.support;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

import infra.core.conversion.Converter;
import infra.util.StringUtils;

/**
 * Converts from a String to a {@link UUID}.
 *
 * @author Phillip Webb
 * @see UUID#fromString
 * @since 4.0
 */
final class StringToUUIDConverter implements Converter<String, UUID> {

  @Override
  @Nullable
  public UUID convert(String source) {
    return StringUtils.hasText(source) ? UUID.fromString(source.trim()) : null;
  }

}

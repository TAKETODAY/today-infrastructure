/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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

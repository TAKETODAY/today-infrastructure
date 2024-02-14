/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.accept;

import java.util.List;

import cn.taketoday.http.MediaType;

/**
 * Strategy to resolve a {@link MediaType} to a list of file extensions &mdash;
 * for example, to resolve "application/json" to "json".
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface MediaTypeFileExtensionResolver {

  /**
   * Resolve the given media type to a list of file extensions.
   *
   * @param mediaType the media type to resolve
   * @return a list of extensions or an empty list (never {@code null})
   */
  List<String> resolveFileExtensions(MediaType mediaType);

  /**
   * Get all registered file extensions.
   *
   * @return a list of extensions or an empty list (never {@code null})
   */
  List<String> getAllFileExtensions();

}

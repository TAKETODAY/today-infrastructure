/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.web.util;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;

/**
 * Defines methods for expanding a URI template with variables.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.web.client.RestTemplate#setUriTemplateHandler(UriTemplateHandler)
 * @since 4.0
 */
public interface UriTemplateHandler {

  /**
   * Expand the given URI template with a map of URI variables.
   *
   * @param uriTemplate the URI template
   * @param uriVariables variable values
   * @return the created URI instance
   */
  URI expand(String uriTemplate, Map<String, ? extends @Nullable Object> uriVariables);

  /**
   * Expand the given URI template with an array of URI variables.
   *
   * @param uriTemplate the URI template
   * @param uriVariables variable values
   * @return the created URI instance
   */
  URI expand(String uriTemplate, @Nullable Object... uriVariables);

}

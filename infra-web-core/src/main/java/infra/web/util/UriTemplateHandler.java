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

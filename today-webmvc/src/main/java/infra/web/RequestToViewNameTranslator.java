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

package infra.web;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for translating an incoming {@link RequestContext}
 * into a logical view name when no view name is explicitly supplied.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 21:44
 */
public interface RequestToViewNameTranslator {

  /**
   * Translate the given {@link RequestContext} into a view name.
   *
   * @param request the incoming {@link RequestContext} providing
   * the context from which a view name is to be resolved
   * @return the view name, or {@code null} if no default found
   * @throws Exception if view name translation fails
   */
  @Nullable
  String getViewName(RequestContext request) throws Exception;

}


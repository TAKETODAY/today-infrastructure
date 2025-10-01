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


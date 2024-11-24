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

package infra.web.service.invoker;

import infra.core.MethodParameter;
import infra.web.annotation.RequestAttribute;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestAttribute @RequestAttribute}
 * annotated arguments.
 *
 * <p>The argument may be a single variable value or a {@code Map} with multiple
 * variables and values.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link RequestAttribute#required()} is set to {@code false}
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RequestAttributeArgumentResolver extends AbstractNamedValueArgumentResolver {

  @Override
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    RequestAttribute annot = parameter.getParameterAnnotation(RequestAttribute.class);
    return (annot == null ? null :
            new NamedValueInfo(annot.name(), annot.required(), null, "request attribute", false));
  }

  @Override
  protected void addRequestValue(String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    requestValues.addAttribute(name, value);
  }

}

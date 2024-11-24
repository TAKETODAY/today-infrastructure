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
import infra.core.conversion.ConversionService;
import infra.util.MultiValueMap;
import infra.web.annotation.RequestHeader;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestHeader @RequestHeader}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>{@code Map<String, ?>} or
 * {@link MultiValueMap MultiValueMap&lt;String, ?&gt;}
 * with multiple headers and value(s).
 * <li>{@code Collection} or an array of header values.
 * <li>An individual header value.
 * </ul>
 *
 * <p>Individual header values may be Strings or Objects to be converted to
 * String values through the configured {@link ConversionService}.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link RequestHeader#required()} is set to {@code false}
 * <li>{@link RequestHeader#defaultValue()} provides a fallback value
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RequestHeaderArgumentResolver extends AbstractNamedValueArgumentResolver {

  public RequestHeaderArgumentResolver(ConversionService conversionService) {
    super(conversionService);
  }

  @Override
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    RequestHeader annot = parameter.getParameterAnnotation(RequestHeader.class);
    return (annot == null ? null :
            new NamedValueInfo(annot.name(), annot.required(), annot.defaultValue(), "request header", true));
  }

  @Override
  protected void addRequestValue(String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    requestValues.addHeader(name, (String) value);
  }

}

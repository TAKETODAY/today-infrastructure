/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.service.invoker;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.RequestParam;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestParam @RequestParam}
 * annotated arguments.
 *
 * <p>When {@code "content-type"} is set to
 * {@code "application/x-www-form-urlencoded"}, request parameters are encoded
 * in the request body. Otherwise, they are added as URL query parameters.
 *
 * <p>The argument may be:
 * <ul>
 * <li>{@code Map<String, ?>} or
 * {@link MultiValueMap MultiValueMap&lt;String, ?&gt;} with
 * multiple request parameter and value(s).
 * <li>{@code Collection} or an array of request parameters.
 * <li>An individual request parameter.
 * </ul>
 *
 * <p>Individual request parameters may be Strings or Objects to be converted to
 * String values through the configured {@link ConversionService}.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link RequestParam#required()} is set to {@code false}
 * <li>{@link RequestParam#defaultValue()} provides a fallback value
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RequestParamArgumentResolver extends AbstractNamedValueArgumentResolver {

  public RequestParamArgumentResolver(ConversionService conversionService) {
    super(conversionService);
  }

  @Override
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    RequestParam annot = parameter.getParameterAnnotation(RequestParam.class);
    return (annot == null ? null :
            new NamedValueInfo(annot.name(), annot.required(), annot.defaultValue(), "request parameter", true));
  }

  @Override
  protected void addRequestValue(String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    requestValues.addRequestParameter(name, (String) value);
  }

}

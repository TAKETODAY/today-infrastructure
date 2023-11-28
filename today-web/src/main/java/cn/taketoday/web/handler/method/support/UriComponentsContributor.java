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

package cn.taketoday.web.handler.method.support;

import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.web.util.UriComponents;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * Strategy for contributing to the building of a {@link UriComponents} by
 * looking at a method parameter and an argument value and deciding what
 * part of the target URL should be updated.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/7 22:04
 */
public interface UriComponentsContributor {

  /**
   * Whether this contributor supports the given method parameter.
   */
  boolean supportsParameter(MethodParameter parameter);

  /**
   * Process the given method argument and either update the
   * {@link UriComponentsBuilder} or add to the map with URI variables
   * to use to expand the URI after all arguments are processed.
   *
   * @param parameter the controller method parameter (never {@code null})
   * @param value the argument value (possibly {@code null})
   * @param builder the builder to update (never {@code null})
   * @param uriVariables a map to add URI variables to (never {@code null})
   * @param conversionService a ConversionService to format values as Strings
   */
  void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
          Map<String, Object> uriVariables, ConversionService conversionService);

}

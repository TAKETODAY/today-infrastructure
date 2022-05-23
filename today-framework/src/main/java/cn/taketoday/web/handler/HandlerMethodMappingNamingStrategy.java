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

package cn.taketoday.web.handler;

import cn.taketoday.web.handler.method.AbstractHandlerMethodMapping;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * A strategy for assigning a name to a handler method's mapping.
 *
 * <p>The strategy can be configured on {@link AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping}. It is used to assign a name to the mapping of
 * every registered handler method. The names can then be queried via
 * {@link AbstractHandlerMethodMapping#getHandlerMethodsForMappingName(String)
 * AbstractHandlerMethodMapping#getHandlerMethodsForMappingName}.
 *
 * <p>Applications can build a URL to a controller method by name with the help
 * of the static method
 * {@link cn.taketoday.web.handler.method.MvcUriComponentsBuilder#fromMappingName(String)
 * MvcUriComponentsBuilder#fromMappingName}
 *
 * @param <T> the mapping type
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/1 21:58
 */
@FunctionalInterface
public interface HandlerMethodMappingNamingStrategy<T> {

  /**
   * Determine the name for the given HandlerMethod and mapping.
   *
   * @param handlerMethod the handler method
   * @param mapping the mapping
   * @return the name
   */
  String getName(HandlerMethod handlerMethod, T mapping);

}

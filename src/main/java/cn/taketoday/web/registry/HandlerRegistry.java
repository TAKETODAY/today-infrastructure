/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.registry;

import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * For registering handler
 *
 * @author TODAY 2019-12-08 23:06
 */
@FunctionalInterface
public interface HandlerRegistry {

  /**
   * Name of the {@link HttpServletRequest} attribute that contains the mapped
   * handler for the best matching pattern.
   *
   * @since 4.0
   */
  String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerRegistry.class.getName() + ".bestMatchingHandler";

  /**
   * Name of the {@link RequestContext} attribute that contains the path
   * within the handler , in case of a pattern match, or the full
   * relevant URI (typically within the DispatcherHandler's mapping) else.
   * <p>Note: This attribute is not required to be supported by all
   * HandlerRegistry implementations. URL-based HandlerRegistries will
   * typically support it, but handlers should not necessarily expect
   * this request attribute to be present in all scenarios.
   *
   * @since 4.0
   */
  String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          HandlerRegistry.class, ".pathWithinHandlerMapping");

  /**
   * Name of the {@link RequestContext} attribute that contains the
   * best matching pattern within the handler mapping.
   * <p>Note: This attribute is not required to be supported by all
   * HandlerRegistry implementations. URL-based HandlerRegistries will
   * typically support it, but handlers should not necessarily expect
   * this request attribute to be present in all scenarios.
   *
   * @since 4.0
   */
  String BEST_MATCHING_PATTERN_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          HandlerRegistry.class, ".bestMatchingPattern");

  /**
   * Name of the boolean {@link RequestContext} attribute that indicates
   * whether type-level mappings should be inspected.
   * <p>Note: This attribute is not required to be supported by all
   * HandlerRegistry implementations.
   *
   * @since 4.0
   */
  String INTROSPECT_TYPE_LEVEL_MAPPING = Conventions.getQualifiedAttributeName(
          HandlerRegistry.class, ".introspectTypeLevelMapping");

  /**
   * Name of the {@link RequestContext} attribute that contains the URI
   * templates map, mapping variable names to values.
   * <p>Note: This attribute is not required to be supported by all
   * HandlerRegistry implementations. URL-based HandlerRegistries will
   * typically support it, but handlers should not necessarily expect
   * this request attribute to be present in all scenarios.
   *
   * @since 4.0
   */
  String URI_TEMPLATE_VARIABLES_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          HandlerRegistry.class, ".uriTemplateVariables");

  /**
   * Name of the {@link RequestContext} attribute that contains a map with
   * URI variable names and a corresponding MultiValueMap of URI matrix
   * variables for each.
   * <p>Note: This attribute is not required to be supported by all
   * HandlerRegistry implementations and may also not be present depending on
   * whether the HandlerRegistry is configured to keep matrix variable content
   *
   * @since 4.0
   */
  String MATRIX_VARIABLES_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          HandlerRegistry.class, ".matrixVariables");

  /**
   * Name of the {@link RequestContext} attribute that contains the set of
   * producible MediaTypes applicable to the mapped handler.
   * <p>Note: This attribute is not required to be supported by all
   * HandlerRegistry implementations. Handlers should not necessarily expect
   * this request attribute to be present in all scenarios.
   *
   * @since 4.0
   */
  String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          HandlerRegistry.class, ".producibleMediaTypes");

  /**
   * Lookup current request context's handler
   * <p>
   * <b>NOTE</b> : cannot throws any exception
   * </p>
   *
   * @param context Current request context
   * @return Target handler. If returns {@code null} indicates no handler
   * @throws Exception if there is an internal error
   */
  @Nullable
  Object lookup(RequestContext context) throws Exception;
}

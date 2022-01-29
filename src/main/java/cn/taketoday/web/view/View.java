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

package cn.taketoday.web.view;

import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * MVC View for a web interaction. Implementations are responsible for rendering
 * content, and exposing the model. A single view exposes multiple model attributes.
 *
 * <p>This class and the MVC approach associated with it is discussed in Chapter 12 of
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002).
 *
 * <p>View implementations may differ widely. An obvious implementation would be
 * JSP-based. Other implementations might be XSLT-based, or use an HTML generation library.
 * This interface is designed to avoid restricting the range of possible implementations.
 *
 * <p>Views should be beans. They are likely to be instantiated as beans by a ViewResolver.
 * As this interface is stateless, view implementations should be thread-safe.
 *
 * <p>
 * From Spring
 * </p>
 *
 * @author Rod Johnson
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractView
 * @see InternalResourceView
 * @since 4.0 2022/1/29 11:07
 */
public interface View {

  /**
   * Name of the {@link HttpServletRequest} attribute that contains the response status code.
   * <p>Note: This attribute is not required to be supported by all View implementations.
   */
  String RESPONSE_STATUS_ATTRIBUTE = View.class.getName() + ".responseStatus";

  /**
   * Name of the {@link HttpServletRequest} attribute that contains a Map with path variables.
   * The map consists of String-based URI template variable names as keys and their corresponding
   * Object-based values -- extracted from segments of the URL and type converted.
   * <p>Note: This attribute is not required to be supported by all View implementations.
   */
  String PATH_VARIABLES = View.class.getName() + ".pathVariables";

  /**
   * The {@link MediaType} selected during content negotiation,
   * which may be more specific than the one the View is configured with. For example:
   * "application/vnd.example-v1+xml" vs "application/*+xml".
   */
  String SELECTED_CONTENT_TYPE = View.class.getName() + ".selectedContentType";

  /**
   * Return the content type of the view, if predetermined.
   * <p>Can be used to check the view's content type upfront,
   * i.e. before an actual rendering attempt.
   *
   * @return the content type String (optionally including a character set),
   * or {@code null} if not predetermined
   */
  @Nullable
  default String getContentType() {
    return null;
  }

  /**
   * Render the view given the specified model.
   * <p>The first step will be preparing the request: In the JSP case, this would mean
   * setting model objects as request attributes. The second step will be the actual
   * rendering of the view, for example including the JSP via a RequestDispatcher.
   *
   * @param model a Map with name Strings as keys and corresponding model
   * objects as values (Map can also be {@code null} in case of empty model)
   * @param request current HTTP request
   * @param response he HTTP response we are building
   * @throws Exception if rendering failed
   */
  void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
          throws Exception;

}

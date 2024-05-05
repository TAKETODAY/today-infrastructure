/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.mock.request;

import cn.taketoday.mock.web.HttpMockRequestImpl;

/**
 * Extension point for applications or 3rd party libraries that wish to further
 * initialize a {@link HttpMockRequestImpl} instance after it has been built
 * by {@link MockHttpServletRequestBuilder} or its subclass
 * {@link MockMultipartHttpServletRequestBuilder}.
 *
 * <p>Implementations of this interface can be provided to
 * {@link MockHttpServletRequestBuilder#with(RequestPostProcessor)} at the time
 * when a request is about to be constructed.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 4.0
 */
@FunctionalInterface
public interface RequestPostProcessor {

  /**
   * Post-process the given {@code MockHttpServletRequest} after its creation
   * and initialization through a {@code MockHttpServletRequestBuilder}.
   *
   * @param request the request to initialize
   * @return the request to use, either the one passed in or a wrapped one
   */
  HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request);

}

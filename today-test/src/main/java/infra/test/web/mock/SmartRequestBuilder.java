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

package infra.test.web.mock;

import infra.mock.web.HttpMockRequestImpl;
import infra.test.web.mock.request.RequestPostProcessor;

/**
 * Extended variant of a {@link RequestBuilder} that applies its
 * {@link RequestPostProcessor infra.test.web.servlet.request.RequestPostProcessors}
 * as a separate step from the {@link #buildRequest} method.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SmartRequestBuilder extends RequestBuilder {

  /**
   * Apply request post-processing. Typically, that means invoking one or more
   * {@link RequestPostProcessor infra.test.web.servlet.request.RequestPostProcessors}.
   *
   * @param request the request to initialize
   * @return the request to use, either the one passed in or a wrapped one
   */
  HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request);

}

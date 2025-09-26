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

package infra.test.web.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpResponse;
import infra.test.web.client.response.MockRestResponseCreators;

/**
 * A contract for creating a {@link ClientHttpResponse}.
 * Implementations can be obtained via {@link MockRestResponseCreators}.
 *
 * @author Craig Walls
 * @since 4.0
 */
@FunctionalInterface
public interface ResponseCreator {

  /**
   * Create a response for the given request.
   *
   * @param request the request
   */
  ClientHttpResponse createResponse(@Nullable ClientHttpRequest request) throws IOException;

}

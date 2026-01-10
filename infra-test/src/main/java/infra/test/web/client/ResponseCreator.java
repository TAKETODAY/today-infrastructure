/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

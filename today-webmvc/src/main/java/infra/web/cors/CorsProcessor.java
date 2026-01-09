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

package infra.web.cors;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.web.RequestContext;

/**
 * A strategy that takes a request and a {@link CorsConfiguration} and updates
 * the response.
 *
 * <p>
 * This component is not concerned with how a {@code CorsConfiguration} is
 * selected but rather takes follow-up actions such as applying CORS validation
 * checks and either rejecting the response or adding CORS headers to the
 * response.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author TODAY
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @since 2019-12-23 21:01
 */
public interface CorsProcessor {

  /**
   * Process a request given a {@code CorsConfiguration}.
   *
   * @param configuration the applicable CORS configuration (possibly {@code null})
   * @param context the current HTTP context
   * @return {@code false} if the request is rejected, {@code true} otherwise
   */
  boolean process(@Nullable CorsConfiguration configuration, RequestContext context) throws IOException;

}

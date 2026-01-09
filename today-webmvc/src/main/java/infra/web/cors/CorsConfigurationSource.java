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

import infra.web.RequestContext;

/**
 * Interface to be implemented by classes (usually HTTP request handlers) that
 * provides a {@link CorsConfiguration} instance based on the provided request.
 *
 * @author Sebastien Deleuze
 * @author TODAY 2020/12/8 22:29
 */
public interface CorsConfigurationSource {

  /**
   * Return a {@link CorsConfiguration} based on the incoming request.
   *
   * @return the associated {@link CorsConfiguration}, or {@code null} if none
   */
  @Nullable
  CorsConfiguration getCorsConfiguration(RequestContext request);

}

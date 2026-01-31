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

package infra.web;

import org.jspecify.annotations.Nullable;

/**
 * Strategy for managing redirect model data during web requests.
 * Handles the retrieval, updating, and saving of redirect models
 * that need to persist across redirects.
 *
 * @author TODAY 2021/4/2 21:52
 * @since 3.0
 */
public interface RedirectModelManager {

  String BEAN_NAME = "redirectModelManager";

  /**
   * Find a RedirectModel saved by a previous request that matches to the current
   * request, remove it from underlying storage, and also remove other expired
   * RedirectModel instances.
   * <p>This method is invoked in the beginning of every request in contrast
   * to {@link #saveRedirectModel}, which is invoked only when there are
   * flash attributes to be saved - i.e. before a redirect.
   *
   * @param context Current request context
   * @return a RedirectModel matching the current request or {@code null}
   */
  @Nullable
  RedirectModel retrieveAndUpdate(RequestContext context);

  /**
   * Set a {@link RedirectModel} to current request context
   *
   * @param context current request context
   * @param redirectModel value
   */
  void saveRedirectModel(RequestContext context, @Nullable RedirectModel redirectModel);

}

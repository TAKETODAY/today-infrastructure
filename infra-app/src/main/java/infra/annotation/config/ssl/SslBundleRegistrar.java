/*
 * Copyright 2012-present the original author or authors.
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

package infra.annotation.config.ssl;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleRegistry;

/**
 * Interface to be implemented by types that register {@link SslBundle} instances with an
 * {@link SslBundleRegistry}.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface SslBundleRegistrar {

  /**
   * Callback method for registering {@link SslBundle}s with an
   * {@link SslBundleRegistry}.
   *
   * @param registry the registry that accepts {@code SslBundle}s
   */
  void registerBundles(SslBundleRegistry registry);

}

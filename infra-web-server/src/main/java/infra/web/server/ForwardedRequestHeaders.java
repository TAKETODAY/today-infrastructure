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

package infra.web.server;

import infra.util.Assert;
import infra.web.config.WebProperties.ForwardedHeaders.Naming;

/**
 * Configuration for forwarded request headers.
 *
 * @param naming header naming
 * @param usePrefix if the 'prefix' forward header should be used
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 5.0
 */
public record ForwardedRequestHeaders(Naming naming, boolean usePrefix) {

  /**
   * A {@link ForwardedRequestHeaders} instance using {@link Naming#X_FORWARDED} and
   * {@code usePrefix} of {@code false}.
   */
  public static ForwardedRequestHeaders X_FORWARDED_WITHOUT_PREFIX =
          new ForwardedRequestHeaders(Naming.X_FORWARDED, false);

  /**
   * Create a forwarded request headers configuration.
   *
   * @param naming header naming
   * @param usePrefix if the 'prefix' forward header should be used
   */
  public ForwardedRequestHeaders {
    Assert.notNull(naming, "Naming is required");
    Assert.state(!usePrefix || naming == Naming.X_FORWARDED,
            "'usePrefix' can only be used with 'x-forwarded' naming");
  }

}

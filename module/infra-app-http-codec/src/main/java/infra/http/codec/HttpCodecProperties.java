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

package infra.http.codec;

import org.jspecify.annotations.Nullable;

import infra.context.properties.ConfigurationProperties;
import infra.util.DataSize;

/**
 * {@link ConfigurationProperties properties} for reactive codecs.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "http.codecs")
public class HttpCodecProperties {

  /**
   * Whether to log form data at DEBUG level, and headers at TRACE level.
   */
  public boolean logRequestDetails;

  /**
   * Limit on the number of bytes that can be buffered whenever the input stream needs
   * to be aggregated. This applies only to the auto-configured WebFlux server and
   * WebClient instances. By default this is not set, in which case individual codec
   * defaults apply. Most codecs are limited to 256K by default.
   */
  public @Nullable DataSize maxInMemorySize;

}

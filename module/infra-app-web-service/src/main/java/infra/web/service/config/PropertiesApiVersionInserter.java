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

package infra.web.service.config;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.web.client.ApiVersionInserter;

/**
 * {@link ApiVersionInserter} backed by {@link ApiVersionProperties}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class PropertiesApiVersionInserter {

  /**
   * Factory method to get a new {@link PropertiesApiVersionInserter} for the given
   * properties.
   *
   * @param properties the API version properties
   * @return an {@link PropertiesApiVersionInserter} configured from the properties
   */
  public static @Nullable ApiVersionInserter create(ApiVersionProperties.Insert properties) {
    ApiVersionInserter.@Nullable Builder builder = builder(properties);
    return builder != null ? builder.build() : null;
  }

  /**
   * Factory method to create a new
   * {@link infra.web.client.ApiVersionInserter.Builder builder} from the
   * given properties, if there are any.
   *
   * @param properties the API version properties
   * @return a builder configured from the properties or {@code null} if no properties
   * were mapped
   */
  private static ApiVersionInserter.@Nullable Builder builder(ApiVersionProperties.Insert properties) {
    Assert.notNull(properties, "'properties' is required");
    ApiVersionInserter.Builder builder = ApiVersionInserter.builder();

    boolean empty = true;

    if (properties.header != null) {
      builder.useHeader(properties.header);
      empty = false;
    }
    if (properties.queryParameter != null) {
      builder.useQueryParam(properties.queryParameter);
      empty = false;
    }
    if (properties.pathSegment != null) {
      builder.usePathSegment(properties.pathSegment);
      empty = false;
    }

    if (properties.mediaTypeParameter != null) {
      builder.useMediaTypeParam(properties.mediaTypeParameter);
      empty = false;
    }

    return empty ? null : builder;
  }

}

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

package infra.http.service.config;

import org.jspecify.annotations.Nullable;

import infra.context.properties.ConfigurationPropertiesSource;
import infra.context.properties.bind.Name;
import infra.web.client.ApiVersionInserter;

/**
 * API Version properties for both reactive and imperative HTTP Clients.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConfigurationPropertiesSource
public class ApiVersionProperties {

  /**
   * Default version that should be used for each request.
   */
  @Name("default")
  public @Nullable String defaultVersion;

  /**
   * How version details should be inserted into requests.
   */
  public final Insert insert = new Insert();

  /**
   * Factory method to get a new {@link ApiVersionInserter} for the given
   * properties.
   *
   * @return an {@link ApiVersionInserter} configured from the properties
   */
  public @Nullable ApiVersionInserter createApiVersionInserter() {
    ApiVersionInserter.@Nullable Builder builder = builder(insert);
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
  private static ApiVersionInserter.@Nullable Builder builder(Insert properties) {
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

  @ConfigurationPropertiesSource
  public static class Insert {

    /**
     * Insert the version into a header with the given name.
     */
    public @Nullable String header;

    /**
     * Insert the version into a query parameter with the given name.
     */
    public @Nullable String queryParameter;

    /**
     * Insert the version into a path segment at the given index.
     */
    public @Nullable Integer pathSegment;

    /**
     * Insert the version into a media type parameter with the given name.
     */
    public @Nullable String mediaTypeParameter;

  }

}

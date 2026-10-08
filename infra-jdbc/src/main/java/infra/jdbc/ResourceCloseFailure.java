/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import infra.util.Assert;

/**
 * Immutable description of a JDBC resource close failure, without retaining
 * the resource itself or its bound parameter values.
 *
 * @param resourceType the resource that failed to close
 * @param sql the associated SQL, if available
 * @param exception the original close failure
 * @param operationFailure the existing operation failure, if available
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public record ResourceCloseFailure(ResourceType resourceType, @Nullable String sql,
        Throwable exception, @Nullable Throwable operationFailure) {

  /**
   * Validate required context values.
   */
  public ResourceCloseFailure {
    Assert.notNull(resourceType, "ResourceType is required");
    Assert.notNull(exception, "Close exception is required");
  }

  /**
   * JDBC resource categories for close failure reporting.
   */
  public enum ResourceType {
    /**
     * A JDBC connection.
     */
    CONNECTION,
    /**
     * A JDBC statement.
     */
    STATEMENT,
    /**
     * A JDBC result set.
     */
    RESULT_SET,
    /**
     * Another closeable resource.
     */
    OTHER
  }

}

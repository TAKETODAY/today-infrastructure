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

/**
 * Records JDBC resource close failures, for example in logs, metrics or an
 * error collector. Implementations must be thread-safe and should not block.
 * Listener runtime exceptions are isolated and do not interrupt cleanup or alter
 * the operation result. Errors are propagated. Transaction completion failures
 * are not reported here.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface ResourceCloseFailureListener {

  /**
   * Record a resource close failure.
   *
   * @param failure the close failure context
   */
  void onCloseFailure(ResourceCloseFailure failure);

}

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

package infra.persistence.event;

/**
 * The phase in which an entity operation failed. A phase does not establish
 * transaction outcome, database state, or whether retrying is safe.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public enum EntityOperationPhase {

  /** Entity or batch pre-execution processing through registered listeners. */
  PRE_PROCESSING,

  /** ID generation, version increment, SQL preparation, binding, or batch collection. */
  PREPARATION,

  /** Statement execution and result validation, including optimistic locking. */
  EXECUTION,

  /** Generated ID assignment or post-execution listener invocation. */
  POST_PROCESSING

}

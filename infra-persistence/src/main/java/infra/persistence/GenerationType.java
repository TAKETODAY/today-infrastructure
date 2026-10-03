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

package infra.persistence;

/**
 * Built-in primary-key generation strategies.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public enum GenerationType {

  /** Database-generated identity, read back through JDBC generated keys. */
  IDENTITY,

  /**
   * UUID generated before insertion, for UUID or String properties.
   * Defaults to version 7 using a Unix millisecond timestamp and random bits
   * without guaranteeing strict monotonicity. A container-provided
   * {@link infra.util.UuidGenerator} bean overrides the default generator.
   * An explicit generator can also be configured through
   * {@link infra.persistence.support.IdGeneratorResolver#setUuidGenerator}.
   */
  UUID

}

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
 * An enumeration representing the sorting order for query results.
 *
 * <p>This enum defines two constants:
 * <ul>
 *   <li>{@code ASC}: Represents ascending order.</li>
 *   <li>{@code DESC}: Represents descending order.</li>
 * </ul>
 *
 * <p>It is typically used in query operations to specify the desired order
 * of the results. For example, it can be used in combination with sorting
 * annotations or methods to define how data should be ordered.
 */
public enum Order {
  ASC, DESC
}

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
 * A source interface for providing a {@link PropertyUpdateStrategy} that determines
 * how properties of an entity should be updated. Implementations of this interface
 * define the strategy used to decide whether a property should be updated based on
 * specific conditions or logic.
 *
 * <p>This interface is typically implemented by classes that represent entities or
 * parts of entities, allowing fine-grained control over update behavior for individual
 * properties or groups of properties.
 *
 * <p>Example usage:
 * <pre>{@code
 * @EntityRef(UserModel.class)
 * static class UserName implements UpdateStrategySource {
 *
 *   @Nullable
 *   final Integer id;
 *
 *   final String name;
 *
 *   UserName(@Nullable Integer id, String name) {
 *     this.id = id;
 *     this.name = name;
 *   }
 *
 *   @Override
 *   public PropertyUpdateStrategy updateStrategy() {
 *     return PropertyUpdateStrategy.noneNull();
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyUpdateStrategy
 * @since 4.0 2024/4/11 10:24
 */
public interface UpdateStrategySource {

  /**
   * Returns the {@link PropertyUpdateStrategy} that defines how properties of an entity
   * should be updated. The strategy determines whether a property should be updated based
   * on specific conditions or logic.
   *
   * <p>This method is typically implemented by classes that represent entities or parts
   * of entities, allowing fine-grained control over update behavior for individual
   * properties or groups of properties.
   *
   * <p>Example usage:
   * <pre>{@code
   * @Override
   * public PropertyUpdateStrategy updateStrategy() {
   *   return PropertyUpdateStrategy.noneNull();
   * }
   * }</pre>
   *
   * @return the {@link PropertyUpdateStrategy} to be used for determining property updates
   * @see PropertyUpdateStrategy
   */
  PropertyUpdateStrategy updateStrategy();

}

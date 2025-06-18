/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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

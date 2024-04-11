/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.persistence;

/**
 * Property Update Strategy
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UpdateStrategySource
 * @since 4.0 2022/12/18 22:20
 */
public interface PropertyUpdateStrategy {

  /**
   * Test input property should be updated?
   */
  boolean shouldUpdate(Object entity, EntityProperty property);

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new Strategy
   */
  default PropertyUpdateStrategy and(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) && next.shouldUpdate(entity, property);
  }

  /**
   * returns a new chain
   *
   * @param next next resolver
   * @return returns a new Strategy
   */
  default PropertyUpdateStrategy or(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) || next.shouldUpdate(entity, property);
  }

  /**
   * Update the none null property
   */
  static PropertyUpdateStrategy noneNull() {
    return (entity, property) -> property.getValue(entity) != null;
  }

  /**
   * Always update
   */
  static PropertyUpdateStrategy always() {
    return (entity, property) -> true;
  }

}

/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.util.subpackage;

/**
 * Abstract base class for <em>persistent entities</em>; intended for use in
 * unit tests.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class PersistentEntity {

  private long id;

  public long getId() {
    return this.id;
  }

  protected void setId(long id) {
    this.id = id;
  }

}

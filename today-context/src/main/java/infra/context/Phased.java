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
package infra.context;

/**
 * Interface for objects that may participate in a phased
 * process such as lifecycle management.
 *
 * @author Mark Fisher
 * @author TODAY 2021/11/12 16:29
 * @see SmartLifecycle
 * @since 4.0
 */
public interface Phased {

  /**
   * Return the phase value of this object.
   */
  int getPhase();

}

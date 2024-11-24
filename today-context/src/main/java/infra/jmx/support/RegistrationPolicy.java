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

package infra.jmx.support;

/**
 * Indicates registration behavior when attempting to register an MBean that already
 * exists.
 *
 * @author Phillip Webb
 * @author Chris Beams
 * @since 4.0
 */
public enum RegistrationPolicy {

  /**
   * Registration should fail when attempting to register an MBean under a name that
   * already exists.
   */
  FAIL_ON_EXISTING,

  /**
   * Registration should ignore the affected MBean when attempting to register an MBean
   * under a name that already exists.
   */
  IGNORE_EXISTING,

  /**
   * Registration should replace the affected MBean when attempting to register an MBean
   * under a name that already exists.
   */
  REPLACE_EXISTING

}

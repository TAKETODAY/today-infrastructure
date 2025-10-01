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

package infra.building.nullability;

import org.gradle.api.provider.Property;

/**
 * Extension for configuring the {@link NullabilityPlugin}.
 *
 * @author Andy Wilkinson
 */
public abstract class NullabilityPluginExtension {

  static final String ERROR_PRONE_VERSION = "2.41.0";

  static final String NULL_AWAY_VERSION = "0.12.10";

  /**
   * Internal use only.
   */
  public NullabilityPluginExtension() {
    getErrorProneVersion().convention(ERROR_PRONE_VERSION);
    getNullAwayVersion().convention(NULL_AWAY_VERSION);
  }

  /**
   * The version of Error Prone to use.
   *
   * @return the Error Prone version
   */
  public abstract Property<String> getErrorProneVersion();

  /**
   * The version of NullAway to use.
   *
   * @return the NullAway version
   */
  public abstract Property<String> getNullAwayVersion();

}
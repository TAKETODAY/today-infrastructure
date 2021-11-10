/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.env;

import java.io.Serial;
import java.util.Set;

/**
 * Exception thrown when required properties are not found.
 *
 * @author Chris Beams
 * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
 * @see ConfigurablePropertyResolver#validateRequiredProperties()
 * @since 4.0
 */
public class MissingRequiredPropertiesException extends IllegalStateException {
  @Serial
  private static final long serialVersionUID = 1L;

  private final Set<String> missingRequiredProperties;

  public MissingRequiredPropertiesException(Set<String> missingRequiredProperties) {
    this.missingRequiredProperties = missingRequiredProperties;
  }

  @Override
  public String getMessage() {
    return "The following properties were declared as required but could not be resolved: " +
            getMissingRequiredProperties();
  }

  /**
   * Return the set of properties marked as required but not present
   * upon validation.
   *
   * @see ConfigurablePropertyResolver#setRequiredProperties(String...)
   * @see ConfigurablePropertyResolver#validateRequiredProperties()
   */
  public Set<String> getMissingRequiredProperties() {
    return this.missingRequiredProperties;
  }

}

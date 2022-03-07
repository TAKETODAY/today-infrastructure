/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jmx.export.metadata;

/**
 * Base class for all JMX metadata classes.
 *
 * @author Rob Harrop
 * @since 4.0
 */
public abstract class AbstractJmxAttribute {

  private String description = "";

  private int currencyTimeLimit = -1;

  /**
   * Set a description for this attribute.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Return a description for this attribute.
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Set a currency time limit for this attribute.
   */
  public void setCurrencyTimeLimit(int currencyTimeLimit) {
    this.currencyTimeLimit = currencyTimeLimit;
  }

  /**
   * Return a currency time limit for this attribute.
   */
  public int getCurrencyTimeLimit() {
    return this.currencyTimeLimit;
  }

}

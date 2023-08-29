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

package cn.taketoday.context.properties.sample.simple;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Configuration properties with various description styles.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("description")
public class DescriptionProperties {

  /**
   * A simple description.
   */
  private String simple;

  /**
   * This is a lengthy description that spans across multiple lines to showcase that the
   * line separators are cleaned automatically.
   */
  private String multiLine;

  public String getSimple() {
    return this.simple;
  }

  public void setSimple(String simple) {
    this.simple = simple;
  }

  public String getMultiLine() {
    return this.multiLine;
  }

  public void setMultiLine(String multiLine) {
    this.multiLine = multiLine;
  }

}

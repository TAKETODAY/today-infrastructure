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

package cn.taketoday.context.properties.sample.generic;

import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Demonstrate properties with a wildcard type.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("wildcard")
public class WildcardConfig {

  private Map<String, ? extends Number> stringToNumber;

  private List<? super Integer> integers;

  public Map<String, ? extends Number> getStringToNumber() {
    return this.stringToNumber;
  }

  public void setStringToNumber(Map<String, ? extends Number> stringToNumber) {
    this.stringToNumber = stringToNumber;
  }

  public List<? super Integer> getIntegers() {
    return this.integers;
  }

  public void setIntegers(List<? super Integer> integers) {
    this.integers = integers;
  }

}

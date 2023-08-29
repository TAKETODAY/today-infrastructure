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

package cn.taketoday.context.properties.sample.specific;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Demonstrate that an unrelated setter is not taken into account to detect the deprecated
 * flag.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("not.deprecated")
public class DeprecatedUnrelatedMethodPojo {

  private Integer counter;

  private boolean flag;

  public Integer getCounter() {
    return this.counter;
  }

  public void setCounter(Integer counter) {
    this.counter = counter;
  }

  @Deprecated
  public void setCounter(String counterAsString) {
    this.counter = Integer.valueOf(counterAsString);
  }

  public boolean isFlag() {
    return this.flag;
  }

  public void setFlag(boolean flag) {
    this.flag = flag;
  }

  @Deprecated
  public void setFlag(Boolean flag) {
    this.flag = flag;
  }

}

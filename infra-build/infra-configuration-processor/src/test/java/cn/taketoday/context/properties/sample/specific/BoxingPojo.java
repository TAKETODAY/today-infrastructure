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
 * Demonstrate the use of boxing/unboxing. Even if the type does not strictly match, it
 * should still be detected.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("boxing")
public class BoxingPojo {

  private boolean flag;

  private Boolean anotherFlag;

  private Integer counter;

  public boolean isFlag() {
    return this.flag;
  }

  // Setter use Boolean
  public void setFlag(Boolean flag) {
    this.flag = flag;
  }

  public boolean isAnotherFlag() {
    return Boolean.TRUE.equals(this.anotherFlag);
  }

  public void setAnotherFlag(boolean anotherFlag) {
    this.anotherFlag = anotherFlag;
  }

  public Integer getCounter() {
    return this.counter;
  }

  // Setter use int
  public void setCounter(int counter) {
    this.counter = counter;
  }

}

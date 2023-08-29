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

package cn.taketoday.context.properties.sample.inheritance;

public class ChildProperties extends BaseProperties {

  private long longValue;

  private final NestInChild childNest = new NestInChild();

  public long getLongValue() {
    return this.longValue;
  }

  public void setLongValue(long longValue) {
    this.longValue = longValue;
  }

  public NestInChild getChildNest() {
    return this.childNest;
  }

  public static class NestInChild {

    private boolean boolValue;

    private int intValue;

    public boolean isBoolValue() {
      return this.boolValue;
    }

    public void setBoolValue(boolean boolValue) {
      this.boolValue = boolValue;
    }

    public int getIntValue() {
      return this.intValue;
    }

    public void setIntValue(int intValue) {
      this.intValue = intValue;
    }

  }

}

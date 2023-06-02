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

package cn.taketoday.framework.jackson;

import cn.taketoday.util.ObjectUtils;

/**
 * Sample object used for tests.
 *
 * @author Phillip Webb
 * @author Paul Aly
 */
public final class NameAndAge extends Name {

  private final int age;

  public NameAndAge(String name, int age) {
    super(name);
    this.age = age;
  }

  public int getAge() {
    return this.age;
  }

  public String asKey() {
    return this.name + " is " + this.age;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof NameAndAge) {
      NameAndAge other = (NameAndAge) obj;
      boolean rtn = true;
      rtn = rtn && ObjectUtils.nullSafeEquals(this.name, other.name);
      rtn = rtn && ObjectUtils.nullSafeEquals(this.age, other.age);
      return rtn;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
    result = prime * result + ObjectUtils.nullSafeHashCode(this.age);
    return result;
  }

}

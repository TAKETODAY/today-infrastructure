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

package cn.taketoday.bytecode.reflect;

import java.lang.reflect.Member;

import cn.taketoday.lang.Assert;
import cn.taketoday.reflect.Accessor;

@SuppressWarnings({ "rawtypes" })
abstract public class FastMemberAccessor implements Accessor {

  protected final int index;
  protected final MethodAccess fc;
  protected final Member member;

  protected FastMemberAccessor(MethodAccess fc, Member member, int index) {
    Assert.notNull(fc, "FastClass");
    Assert.notNull(member, "Member");

    this.fc = fc;
    this.index = index;
    this.member = member;
  }

  abstract public Class[] getParameterTypes();

  abstract public Class[] getExceptionTypes();

  public int getIndex() {
    return index;
  }

  public String getName() {
    return member.getName();
  }

  public Class getDeclaringClass() {
    return fc.getDeclaringClass();
  }

  public int getModifiers() {
    return member.getModifiers();
  }

  @Override
  public String toString() {
    return member.toString();
  }

  @Override
  public int hashCode() {
    return member.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return o == this || (o instanceof FastMemberAccessor && member.equals(((FastMemberAccessor) o).member));
  }
}

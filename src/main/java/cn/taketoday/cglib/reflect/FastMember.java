/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.reflect;

import java.lang.reflect.Member;
import java.util.Objects;

@SuppressWarnings("all")
abstract public class FastMember {

  protected final int index;
  protected final FastClass fc;
  protected final Member member;

  protected FastMember(FastClass fc, Member member, int index) {
    this.index = index;
    this.fc = Objects.requireNonNull(fc);
    this.member = Objects.requireNonNull(member);
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
    return fc.getJavaClass();
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
    return o == this || (o instanceof FastMember && member.equals(((FastMember) o).member));
  }
}

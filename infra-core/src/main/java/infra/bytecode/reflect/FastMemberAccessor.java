/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.reflect;

import java.lang.reflect.Member;

import infra.lang.Assert;
import infra.reflect.Accessor;

@SuppressWarnings({ "rawtypes" })
public abstract class FastMemberAccessor implements Accessor {

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

  public abstract Class[] getParameterTypes();

  public abstract Class[] getExceptionTypes();

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

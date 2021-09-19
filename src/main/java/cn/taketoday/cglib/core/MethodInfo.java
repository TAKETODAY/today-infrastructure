/*
 * Copyright 2004 The Apache Software Foundation
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
package cn.taketoday.cglib.core;

import java.lang.reflect.Member;

import cn.taketoday.asm.Attribute;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.commons.MethodSignature;

/**
 * @author TODAY <br>
 * 2019-09-03 19:01
 */
public abstract class MethodInfo {

  protected MethodInfo() { }

  abstract public ClassInfo getClassInfo();

  abstract public int getModifiers();

  abstract public MethodSignature getSignature();

  abstract public Type[] getExceptionTypes();

  @Override
  public boolean equals(Object o) {
    return (o == this) || ((o instanceof MethodInfo) && getSignature().equals(((MethodInfo) o).getSignature()));
  }

  @Override
  public int hashCode() {
    return getSignature().hashCode();
  }

  @Override
  public String toString() {
    // TODO: include modifiers, exceptions
    return getSignature().toString();
  }

  /**
   * @since 4.0
   */
  public boolean isConstructor() {
    return MethodSignature.CONSTRUCTOR_NAME.equals(getSignature().getName());
  }

  // static

  public static MethodInfo from(final Member member) {
    return from(member, member.getModifiers());
  }

  public static MethodInfo from(final Member member, final int modifiers) {
    final MethodSignature sig = MethodSignature.from(member);
    return new MethodInfo() {
      private ClassInfo ci;

      public ClassInfo getClassInfo() {
        if (ci == null)
          ci = ClassInfo.from(member.getDeclaringClass());
        return ci;
      }

      public int getModifiers() {
        return modifiers;
      }

      public MethodSignature getSignature() {
        return sig;
      }

      public Type[] getExceptionTypes() {
        return Type.getExceptionTypes(member);
      }

      public Attribute getAttribute() {
        return null;
      }
    };
  }
}

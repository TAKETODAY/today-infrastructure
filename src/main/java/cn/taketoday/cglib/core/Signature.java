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
package cn.taketoday.cglib.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import cn.taketoday.asm.Type;
import cn.taketoday.core.Constant;

/**
 * A representation of a method signature, containing the method name, return
 * type, and parameter types.
 */
public class Signature {

  private final String name;
  private final String desc;

  public Signature(Method method) {
    this(method.getName(), Type.getMethodDescriptor(method));
  }

  public Signature(Constructor<?> constructor) {
    this("<init>", Type.getConstructorDescriptor(constructor));
  }

  public Signature(String name, String desc) {
    // TODO: better error checking
    if (name.indexOf('(') >= 0) {
      throw new IllegalArgumentException("Name '" + name + "' is invalid");
    }
    this.name = name;
    this.desc = desc;
  }

  public Signature(String name, Type returnType, Type[] argumentTypes) {
    this(name, Type.getMethodDescriptor(returnType, argumentTypes));
  }

  public String getName() {
    return name;
  }

  public String getDescriptor() {
    return desc;
  }

  public Type getReturnType() {
    return Type.forReturnType(desc);
  }

  public Type[] getArgumentTypes() {
    return Type.getArgumentTypes(desc);
  }

  @Override
  public String toString() {
    return name.concat(desc);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Signature) {
      final Signature other = (Signature) o;
      return name.equals(other.name) && desc.equals(other.desc);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode() ^ desc.hashCode();
  }

  // static

  @SuppressWarnings("rawtypes")
  public static Signature fromMember(Member member) {
    if (member instanceof Method) {
      return new Signature(member.getName(), Type.getMethodDescriptor((Method) member));
    }
    if (member instanceof Constructor) {
      Type[] types = TypeUtils.getTypes(((Constructor) member).getParameterTypes());
      return new Signature(Constant.CONSTRUCTOR_NAME, Type.getMethodDescriptor(Type.VOID_TYPE, types));
    }
    throw new IllegalArgumentException("Cannot get signature of a field");
  }

}

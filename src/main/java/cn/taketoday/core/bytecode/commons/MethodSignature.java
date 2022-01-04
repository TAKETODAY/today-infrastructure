// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.core.bytecode.commons;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import cn.taketoday.core.bytecode.Type;

/**
 * A named method descriptor.
 * <p>
 * A representation of a method signature, containing the method name, return
 * type, and parameter types.
 *
 * @author Juozas Baliuka
 * @author Chris Nokleberg
 * @author Eric Bruneton
 * @author TODAY
 */
public class MethodSignature {
  public static final String CONSTRUCTOR_NAME = "<init>";
  public static final String STATIC_CLASS_INIT = "<clinit>";
  public static final MethodSignature SIG_STATIC = new MethodSignature(STATIC_CLASS_INIT, "()V");
  public static final MethodSignature EMPTY_CONSTRUCTOR = new MethodSignature(CONSTRUCTOR_NAME, "()V");

  // Number
  public static final MethodSignature INT_VALUE = MethodSignature.from("int intValue()");
  public static final MethodSignature LONG_VALUE = MethodSignature.from("long longValue()");
  public static final MethodSignature CHAR_VALUE = MethodSignature.from("char charValue()");
  public static final MethodSignature FLOAT_VALUE = MethodSignature.from("float floatValue()");
  public static final MethodSignature DOUBLE_VALUE = MethodSignature.from("double doubleValue()");
  public static final MethodSignature BOOLEAN_VALUE = MethodSignature.from("boolean booleanValue()");

  // Object
  public static final MethodSignature HASH_CODE = MethodSignature.from("int hashCode()");
  public static final MethodSignature GET_CLASS = MethodSignature.from("Class getClass()");
  public static final MethodSignature TO_STRING = MethodSignature.from("String toString()");
  public static final MethodSignature EQUALS = MethodSignature.from("boolean equals(Object)");

  public static final MethodSignature constructWithString = MethodSignature.forConstructor("String");

  /** The method name. */
  private final String name;

  /** The method descriptor. */
  private final String descriptor;

  // for cache
  private volatile Type returnType;
  private volatile Type[] argumentTypes;

  /**
   * Constructs a new {@link MethodSignature}.
   *
   * @param name the method's name.
   * @param descriptor the method's descriptor.
   */
  public MethodSignature(final String name, final String descriptor) {
    this.name = name;
    this.descriptor = descriptor;
  }

  /**
   * Constructs a new {@link MethodSignature}.
   *
   * @param name the method's name.
   * @param returnType the method's return type.
   * @param argumentTypes the method's argument types.
   * @since 4.0
   */
  public MethodSignature(final Type returnType, final String name, final Type... argumentTypes) {
    this(name, Type.getMethodDescriptor(returnType, argumentTypes));
  }

  /**
   * Creates a new {@link MethodSignature}.
   *
   * @param method a java.lang.reflect method descriptor
   * @return a {@link MethodSignature} corresponding to the given Java method declaration.
   */
  public static MethodSignature from(final Method method) {
    return new MethodSignature(method.getName(), Type.getMethodDescriptor(method));
  }

  /**
   * Creates a new {@link MethodSignature}.
   *
   * @param constructor a java.lang.reflect constructor descriptor
   * @return a {@link MethodSignature} corresponding to the given Java constructor declaration.
   */
  public static MethodSignature from(final Constructor<?> constructor) {
    return new MethodSignature(CONSTRUCTOR_NAME, Type.getConstructorDescriptor(constructor));
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings("rawtypes")
  public static MethodSignature from(Member member) {
    if (member instanceof Method) {
      return from((Method) member);
    }
    if (member instanceof Constructor) {
      return from((Constructor) member);
    }
    throw new IllegalArgumentException("Cannot get signature of " + member);
  }

  /**
   * @param parameterTypes a Java parameterTypes name.
   * @return Constructor Signature
   * @see #CONSTRUCTOR_NAME
   * @since 4.0
   */
  public static MethodSignature forConstructor(final String parameterTypes) {
    String descriptor = Type.getDescriptor(parameterTypes);
    return new MethodSignature(CONSTRUCTOR_NAME, '(' + descriptor + ")V");
  }

  /**
   * @param parameterTypes a Java parameterTypes.
   * @return Constructor Signature
   * @see #CONSTRUCTOR_NAME
   * @since 4.0
   */
  public static MethodSignature forConstructor(Type... parameterTypes) {
    StringBuilder descriptor = new StringBuilder(parameterTypes.length * 8);
    for (final Type type : parameterTypes) {
      descriptor.append(type.getDescriptor());
    }
    return new MethodSignature(CONSTRUCTOR_NAME, '(' + descriptor.toString() + ")V");
  }

  /**
   * Returns a {@link MethodSignature} corresponding to the given Java method declaration.
   *
   * @param method a Java method declaration, without argument names, of the form "returnType name
   * (argumentType1, ... argumentTypeN)", where the types are in plain Java (e.g. "int",
   * "float", "java.util.List", ...). Classes of the java.lang package can be specified by their
   * unqualified name; all other classes names must be fully qualified.
   * @return a {@link MethodSignature} corresponding to the given Java method declaration.
   * @throws IllegalArgumentException if <code>method</code> could not get parsed.
   */
  public static MethodSignature from(final String method) {
    return from(method, false);
  }

  /**
   * Returns a {@link MethodSignature} corresponding to the given Java method declaration.
   *
   * @param method a Java method declaration, without argument names, of the form "returnType name
   * (argumentType1, ... argumentTypeN)", where the types are in plain Java (e.g. "int",
   * "float", "java.util.List", ...). Classes of the java.lang package may be specified by their
   * unqualified name, depending on the defaultPackage argument; all other classes names must be
   * fully qualified.
   * @param defaultPackage true if unqualified class names belong to the default package, or false
   * if they correspond to java.lang classes. For instance "Object" means "Object" if this
   * option is true, or "java.lang.Object" otherwise.
   * @return a {@link MethodSignature} corresponding to the given Java method declaration.
   * @throws IllegalArgumentException if <code>method</code> could not get parsed.
   */
  public static MethodSignature from(final String method, final boolean defaultPackage) {
    final int spaceIndex = method.indexOf(' ');
    int argumentStartIndex = method.indexOf('(', spaceIndex) + 1;
    final int endIndex = method.indexOf(')', argumentStartIndex);
    if (spaceIndex == -1 || argumentStartIndex == 0 || endIndex == -1) {
      throw new IllegalArgumentException();
    }

    final String returnType = method.substring(0, spaceIndex);
    final String methodName =
            method.substring(spaceIndex + 1, argumentStartIndex - 1).trim();
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');

    final String descriptor = Type.getDescriptor(method, argumentStartIndex, endIndex, defaultPackage);
    stringBuilder.append(descriptor);
    stringBuilder.append(')').append(Type.getDescriptor(returnType, defaultPackage));
    return new MethodSignature(methodName, stringBuilder.toString());
  }

  /**
   * Returns the name of the method described by this object.
   *
   * @return the name of the method described by this object.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the descriptor of the method described by this object.
   *
   * @return the descriptor of the method described by this object.
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * Returns the return type of the method described by this object.
   *
   * @return the return type of the method described by this object.
   */
  public Type getReturnType() {
    Type returnType = this.returnType;
    if (returnType == null) {
      synchronized(this) {
        returnType = this.returnType;
        if (returnType == null) {
          returnType = Type.forReturnType(descriptor);
          this.returnType = returnType;
        }
      }
    }
    return returnType;
  }

  /**
   * Returns the argument types of the method described by this object.
   *
   * @return the argument types of the method described by this object.
   */
  public Type[] getArgumentTypes() {
    Type[] argumentTypes = this.argumentTypes;
    if (argumentTypes == null) {
      synchronized(this) {
        argumentTypes = this.argumentTypes;
        if (argumentTypes == null) {
          argumentTypes = Type.getArgumentTypes(descriptor);
          this.argumentTypes = argumentTypes;
        }
      }
    }
    return argumentTypes;
  }

  @Override
  public String toString() {
    return name + descriptor;
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof MethodSignature otherMethod) {
      return name.equals(otherMethod.name)
              && descriptor.equals(otherMethod.descriptor);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode() ^ descriptor.hashCode();
  }

}

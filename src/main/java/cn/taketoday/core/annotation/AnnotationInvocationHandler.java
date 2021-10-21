/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.annotation;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Map;

/**
 * InvocationHandler for dynamic proxy implementation of Annotation.
 *
 * @author TODAY 2021/10/10 21:47
 * @since 4.0
 */
public final class AnnotationInvocationHandler implements InvocationHandler, Serializable {
  private static final long serialVersionUID = 1L;
  private final Class<? extends Annotation> type;
  private final AnnotationAttributes attributes;

  AnnotationInvocationHandler(Class<? extends Annotation> type, AnnotationAttributes attributes) {
    this.type = type;
    this.attributes = attributes;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    String member = method.getName();
    Class<?>[] paramTypes = method.getParameterTypes();

    // Handle Object and Annotation methods
    if (ReflectionUtils.isEqualsMethod(method)) {
      return equalsImpl(args[0]);
    }
    if (paramTypes.length != 0) {
      throw new AssertionError("Too many parameters for an annotation method");
    }
    switch (member) {
      case "toString":
        return toStringImpl();
      case "hashCode":
        return hashCodeImpl();
      case "annotationType":
        return type;
      default:
        break;
    }

    // Handle annotation member accessors
    Object result = attributes.getAttribute(member, method.getReturnType());

    if (result == null) {
      throw new IncompleteAnnotationException(type, member);
    }

    if (result.getClass().isArray() && Array.getLength(result) != 0) {
      result = cloneArray(result);
    }
    return result;
  }

  /**
   * This method, which clones its array argument, would not be necessary
   * if Cloneable had a public clone method.
   */
  private Object cloneArray(Object array) {
    Class<?> type = array.getClass();

    if (type == byte[].class) {
      byte[] byteArray = (byte[]) array;
      return byteArray.clone();
    }
    if (type == char[].class) {
      char[] charArray = (char[]) array;
      return charArray.clone();
    }
    if (type == double[].class) {
      double[] doubleArray = (double[]) array;
      return doubleArray.clone();
    }
    if (type == float[].class) {
      float[] floatArray = (float[]) array;
      return floatArray.clone();
    }
    if (type == int[].class) {
      int[] intArray = (int[]) array;
      return intArray.clone();
    }
    if (type == long[].class) {
      long[] longArray = (long[]) array;
      return longArray.clone();
    }
    if (type == short[].class) {
      short[] shortArray = (short[]) array;
      return shortArray.clone();
    }
    if (type == boolean[].class) {
      boolean[] booleanArray = (boolean[]) array;
      return booleanArray.clone();
    }

    Object[] objectArray = (Object[]) array;
    return objectArray.clone();
  }

  /**
   * Implementation of dynamicProxy.toString()
   */
  private String toStringImpl() {
    StringBuilder result = new StringBuilder(128);
    result.append('@');
    result.append(type.getName());
    result.append('(');
    boolean firstMember = true;
    for (Map.Entry<String, Object> e : attributes.entrySet()) {
      if (firstMember) {
        firstMember = false;
      }
      else {
        result.append(", ");
      }
      result.append(e.getKey());
      result.append('=');
      result.append(memberValueToString(e.getValue()));
    }
    result.append(')');
    return result.toString();
  }

  /**
   * Translates a member value (in "dynamic proxy return form") into a string
   */
  private static String memberValueToString(Object value) {
    Class<?> type = value.getClass();
    if (!type.isArray()) {// primitive, string, class, enum const,
      // or annotation
      return value.toString();
    }
    if (type == byte[].class)
      return Arrays.toString((byte[]) value);
    if (type == char[].class)
      return Arrays.toString((char[]) value);
    if (type == double[].class)
      return Arrays.toString((double[]) value);
    if (type == float[].class)
      return Arrays.toString((float[]) value);
    if (type == int[].class)
      return Arrays.toString((int[]) value);
    if (type == long[].class)
      return Arrays.toString((long[]) value);
    if (type == short[].class)
      return Arrays.toString((short[]) value);
    if (type == boolean[].class)
      return Arrays.toString((boolean[]) value);
    return Arrays.toString((Object[]) value);
  }

  /**
   * Implementation of dynamicProxy.equals(Object o)
   */
  private Boolean equalsImpl(Object o) {
    if (o == this) {
      return true;
    }

    if (!type.isInstance(o)) {
      return false;
    }
    AnnotationAttributes attributes = this.attributes;
    for (Method memberMethod : getMemberMethods()) {
      String member = memberMethod.getName();
      Object ourValue = attributes.get(member);
      Object hisValue;
      AnnotationInvocationHandler hisHandler = asOneOfUs(o);
      if (hisHandler != null) {
        hisValue = hisHandler.attributes.get(member);
      }
      else {
        try {
          hisValue = memberMethod.invoke(o);
        }
        catch (InvocationTargetException e) {
          return false;
        }
        catch (IllegalAccessException e) {
          throw new AssertionError(e);
        }
      }
      if (!memberValueEquals(ourValue, hisValue)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns an object's invocation handler if that object is a dynamic
   * proxy with a handler of type AnnotationInvocationHandler.
   * Returns null otherwise.
   */
  private AnnotationInvocationHandler asOneOfUs(Object o) {
    if (Proxy.isProxyClass(o.getClass())) {
      InvocationHandler handler = Proxy.getInvocationHandler(o);
      if (handler instanceof AnnotationInvocationHandler) {
        return (AnnotationInvocationHandler) handler;
      }
    }
    return null;
  }

  /**
   * Returns true iff the two member values in "dynamic proxy return form"
   * are equal using the appropriate equality function depending on the
   * member type.  The two values will be of the same type unless one of
   * the containing annotations is ill-formed.  If one of the containing
   * annotations is ill-formed, this method will return false unless the
   * two members are identical object references.
   */
  private static boolean memberValueEquals(Object v1, Object v2) {
    Class<?> type = v1.getClass();

    // Check for primitive, string, class, enum const, annotation,
    // or ExceptionProxy
    if (!type.isArray()) {
      return v1.equals(v2);
    }
    // Check for array of string, class, enum const, annotation,
    // or ExceptionProxy
    if (v1 instanceof Object[] && v2 instanceof Object[]) {
      return Arrays.equals((Object[]) v1, (Object[]) v2);
    }

    // Check for ill formed annotation(s)

    if (v2 == null || v2.getClass() != type) {
      return false;
    }

    // Deal with array of primitives
    if (type == byte[].class)
      return Arrays.equals((byte[]) v1, (byte[]) v2);
    if (type == char[].class)
      return Arrays.equals((char[]) v1, (char[]) v2);
    if (type == double[].class)
      return Arrays.equals((double[]) v1, (double[]) v2);
    if (type == float[].class)
      return Arrays.equals((float[]) v1, (float[]) v2);
    if (type == int[].class)
      return Arrays.equals((int[]) v1, (int[]) v2);
    if (type == long[].class)
      return Arrays.equals((long[]) v1, (long[]) v2);
    if (type == short[].class)
      return Arrays.equals((short[]) v1, (short[]) v2);
    assert type == boolean[].class;
    return Arrays.equals((boolean[]) v1, (boolean[]) v2);
  }

  /**
   * Returns the member methods for our annotation type.  These are
   * obtained lazily and cached, as they're expensive to obtain
   * and we only need them if our equals method is invoked (which should
   * be rare).
   */
  private Method[] getMemberMethods() {
    Method[] memberMethods = this.memberMethods;
    if (memberMethods == null) {
      memberMethods = AccessController.doPrivileged(
              new PrivilegedAction<Method[]>() {
                public Method[] run() {
                  final Method[] mm = type.getDeclaredMethods();
                  validateAnnotationMethods(mm);
                  AccessibleObject.setAccessible(mm, true);
                  return mm;
                }
              });
      this.memberMethods = memberMethods;
    }
    return memberMethods;
  }

  private transient volatile Method[] memberMethods = null;

  /**
   * Validates that a method is structurally appropriate for an
   * annotation type. As of Java SE 8, annotation types cannot
   * contain static methods and the declared methods of an
   * annotation type must take zero arguments and there are
   * restrictions on the return type.
   */
  private void validateAnnotationMethods(Method[] memberMethods) {
    /*
     * Specification citations below are from JLS
     * 9.6.1. Annotation Type Elements
     */
    boolean valid = true;
    for (Method method : memberMethods) {
      /*
       * "By virtue of the AnnotationTypeElementDeclaration
       * production, a method declaration in an annotation type
       * declaration cannot have formal parameters, type
       * parameters, or a throws clause.
       *
       * "By virtue of the AnnotationTypeElementModifier
       * production, a method declaration in an annotation type
       * declaration cannot be default or static."
       */
      if (method.getModifiers() != (Modifier.PUBLIC | Modifier.ABSTRACT)
              || method.isDefault()
              || method.getParameterCount() != 0
              || method.getExceptionTypes().length != 0) {
        valid = false;
        break;
      }

      /*
       * "It is a compile-time error if the return type of a
       * method declared in an annotation type is not one of the
       * following: a primitive type, String, Class, any
       * parameterized invocation of Class, an enum type
       * (section 8.9), an annotation type, or an array type
       * (chapter 10) whose element type is one of the preceding
       * types."
       */
      Class<?> returnType = method.getReturnType();
      if (returnType.isArray()) {
        returnType = returnType.getComponentType();
        if (returnType.isArray()) { // Only single dimensional arrays
          valid = false;
          break;
        }
      }

      if (!((returnType.isPrimitive() && returnType != void.class)
              || returnType == String.class
              || returnType == Class.class
              || returnType.isEnum()
              || returnType.isAnnotation())) {
        valid = false;
        break;
      }

      /*
       * "It is a compile-time error if any method declared in an
       * annotation type has a signature that is
       * override-equivalent to that of any public or protected
       * method declared in class Object or in the interface
       * annotation.Annotation."
       *
       * The methods in Object or Annotation meeting the other
       * criteria (no arguments, contrained return type, etc.)
       * above are:
       *
       * String toString()
       * int hashCode()
       * Class<? extends Annotation> annotationType()
       */
      String methodName = method.getName();
      if ((methodName.equals("toString") && returnType == String.class) ||
              (methodName.equals("hashCode") && returnType == int.class) ||
              (methodName.equals("annotationType") && returnType == Class.class)) {
        valid = false;
        break;
      }
    }
    if (!valid)
      throw new AnnotationFormatError("Malformed method on an annotation type");
  }

  /**
   * Implementation of dynamicProxy.hashCode()
   */
  private int hashCodeImpl() {
    int result = 0;
    for (Map.Entry<String, Object> e : attributes.entrySet()) {
      result += (127 * e.getKey().hashCode()) ^ memberValueHashCode(e.getValue());
    }
    return result;
  }

  /**
   * Computes hashCode of a member value (in "dynamic proxy return form")
   */
  private static int memberValueHashCode(Object value) {
    Class<?> type = value.getClass();
    if (!type.isArray()) {// primitive, string, class, enum const,
      // or annotation
      return value.hashCode();
    }
    if (type == byte[].class)
      return Arrays.hashCode((byte[]) value);
    if (type == char[].class)
      return Arrays.hashCode((char[]) value);
    if (type == double[].class)
      return Arrays.hashCode((double[]) value);
    if (type == float[].class)
      return Arrays.hashCode((float[]) value);
    if (type == int[].class)
      return Arrays.hashCode((int[]) value);
    if (type == long[].class)
      return Arrays.hashCode((long[]) value);
    if (type == short[].class)
      return Arrays.hashCode((short[]) value);
    if (type == boolean[].class)
      return Arrays.hashCode((boolean[]) value);
    return Arrays.hashCode((Object[]) value);
  }

}

/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.tree.AnnotationNode;
import cn.taketoday.asm.tree.ClassNode;
import cn.taketoday.asm.tree.FieldNode;
import cn.taketoday.asm.tree.MethodNode;
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
import cn.taketoday.context.utils.AnnotationUtils;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @author TODAY 2021/8/1 18:00
 * @since 4.0
 */
public class ClassMetaReader {
  private static final HashMap<String, ClassNode> classNodeCache = new HashMap<>(128); // class-name to ClassNode
  private static final ConcurrentHashMap<String, AnnotationAttributes> annotationDefaultCache // class-name to AnnotationAttributes
          = new ConcurrentHashMap<>(128);

  public static ClassNode read(Class<?> classToRead) {
    return read(classToRead, ClassUtils.getClassLoader());
  }

  public static ClassNode read(Class<?> key, ClassLoader classLoader) {
    return read(key.getName(), classLoader);
  }

  public static ClassNode read(String className) {
    return read(className, ClassUtils.getClassLoader());
  }

  public static ClassNode read(String className, ClassLoader classLoader) {
    return classNodeCache.computeIfAbsent(className, classToRead -> {
      String classFile = getClassFile(classToRead);
      try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {
        ClassReader classReader = new ClassReader(resourceAsStream);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        return classNode;
      }
      catch (IOException e) {
        throw new IllegalStateException("'" + classFile + "' read failed", e);
      }
    });
  }

  public static String getClassFile(Class<?> classToRead) {
    return getClassFile(classToRead.getName());
  }

  public static String getClassFile(String classNameToRead) {
    return classNameToRead
            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
            .concat(Constant.CLASS_FILE_SUFFIX);
  }

  public static AnnotationAttributes readAnnotation(AnnotationNode annotationNode) {
    if (annotationNode != null) {
      String desc = annotationNode.desc;
      if (desc == null) {
        // exclude array
        return null;
      }
      String className = desc.substring(1, desc.length() - 1).replace('/', '.');
      AnnotationAttributes attributes = new AnnotationAttributes(className);
      // read default values
      applyDefaults(attributes, className);

      // override default values
      List<Object> values = annotationNode.values;
      if (values != null) {
        for (int i = 0, n = values.size(); i < n; i += 2) {
          String name = (String) values.get(i);
          Object value = values.get(i + 1);
          if (value instanceof AnnotationNode) {
            // nested Annotation
            value = readAnnotation((AnnotationNode) value);
          }
          attributes.put(name, value);
        }
      }
      return attributes;
    }
    return null;
  }

  private static void applyDefaults(AnnotationAttributes attributes, String aClass) {
    attributes.putAll(readDefaultAttributes(aClass));
  }

  public static AnnotationAttributes readDefaultAttributes(Class<?> aClass) {
    return readDefaultAttributes(aClass.getName());
  }

  public static AnnotationAttributes readDefaultAttributes(String className) {
    return annotationDefaultCache.computeIfAbsent(className, annotationType -> {
      ClassNode classNode = read(annotationType);
      AnnotationAttributes defaultAttributes = new AnnotationAttributes();
      for (final MethodNode method : classNode.methods) {
        Object defaultValue = method.annotationDefault;
        if (defaultValue != null) {
          // must not be null
          if (defaultValue instanceof AnnotationNode) {
            defaultValue = readAnnotation((AnnotationNode) defaultValue);
          }
          defaultAttributes.put(method.name, defaultValue);
        }
      }
      return defaultAttributes;
    });
  }

  /**
   * read class Annotations
   *
   * @param classNode
   *         class structure source
   */
  public static AnnotationAttributes[] readAnnotations(ClassNode classNode) {
    if (classNode != null) {
      List<AnnotationNode> visibleAnnotations = classNode.visibleAnnotations;
      if (visibleAnnotations != null) {
        AnnotationAttributes[] annotationAttributes = new AnnotationAttributes[visibleAnnotations.size()];
        int i = 0;
        for (final AnnotationNode visibleAnnotation : visibleAnnotations) {
          annotationAttributes[i++] = readAnnotation(visibleAnnotation);
        }
        return annotationAttributes;
      }
    }
    return null;
  }

  public static AnnotationAttributes[] readAnnotation(AnnotatedElement annotated) {
    List<AnnotationNode> annotationNode = getAnnotationNode(annotated);
    if (annotationNode == null) {
      // read from java reflect API
      Annotation[] annotations = annotated.getDeclaredAnnotations();
      AnnotationAttributes[] annotationAttributes = new AnnotationAttributes[annotations.length];
      int i = 0;
      for (final Annotation annotation : annotations) {
        annotationAttributes[i++] = AnnotationUtils.getAnnotationAttributes(annotation);
      }
      return annotationAttributes;
    }
    AnnotationAttributes[] annotationAttributes = new AnnotationAttributes[annotationNode.size()];
    int i = 0;
    for (final AnnotationNode node : annotationNode) {
      annotationAttributes[i++] = readAnnotation(node);
    }
    return annotationAttributes;
  }

  public static List<AnnotationNode> getAnnotationNode(AnnotatedElement annotated) {
    if (annotated instanceof Class) {
      ClassNode classNode = read((Class<?>) annotated);
      return classNode.visibleAnnotations;
    }
    if (annotated instanceof Executable) {
      MethodNode methodNode = getMethodNode((Executable) annotated);
      return methodNode.visibleAnnotations;
    }
    if (annotated instanceof Field) {
      // java reflect field
      Field field = (Field) annotated;
      Class<?> declaringClass = field.getDeclaringClass();
      String descriptor = null;
      ClassNode classNode = read(declaringClass);
      for (final FieldNode fieldNode : classNode.fields) {
        if (Objects.equals(field.getName(), fieldNode.name)) {
          if (descriptor == null) {
            descriptor = Type.getDescriptor(field.getType());
          }
          if (Objects.equals(fieldNode.desc, descriptor)) {
            return fieldNode.visibleAnnotations;
          }
        }
      }
    }
    if (annotated instanceof Parameter) {
      Parameter parameter = (Parameter) annotated;
      MethodNode methodNode = getMethodNode(parameter.getDeclaringExecutable());
      List<AnnotationNode>[] annotations = methodNode.visibleParameterAnnotations;
      return annotations[ClassUtils.getParameterIndex(parameter)];
    }
    return null;
  }

  private static MethodNode getMethodNode(Executable executable) {
    ClassNode classNode = read(executable.getDeclaringClass());
    boolean isConstructor = executable instanceof Constructor;
    String name = isConstructor ? "<init>" : executable.getName();
    for (final MethodNode method : classNode.methods) {
      if (Objects.equals(name, method.name)) {
        String descriptor = getDescriptor(executable, isConstructor);
        if (Objects.equals(method.desc, descriptor)) {
          return method;
        }
      }
    }
    throw new IllegalStateException("cannot read executable annotations");
  }

  private static String getDescriptor(AnnotatedElement annotated, boolean isConstructor) {
    if (isConstructor) {
      return Type.getConstructorDescriptor((Constructor<?>) annotated);
    }
    else {
      return Type.getMethodDescriptor((Method) annotated);
    }
  }

  // proxy

  public static <T extends Annotation> T getAnnotation(Class<T> type, AnnotatedElement annotated) {
    AnnotationAttributes[] annotationAttributes = readAnnotation(annotated);
    AnnotationAttributes attributes = null;
    for (final AnnotationAttributes attribute : annotationAttributes) {
      if (attribute.annotationType() == type) {
        attributes = attribute;
        break;
      }
    }
    if (attributes == null) {
      return null;
    }
    return getAnnotation(type, attributes);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T getAnnotation(Class<T> type, Map<String, Object> attributes) {
    return (T) Proxy.newProxyInstance(
            type.getClassLoader(), new Class<?>[] { type },
            new AnnotationInvocationHandler(type, attributes));
  }

  public static void clearCache() {
    classNodeCache.clear();
    annotationDefaultCache.clear();
  }

  /**
   * InvocationHandler for dynamic proxy implementation of Annotation.
   */
  static final class AnnotationInvocationHandler implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 1L;
    private final Class<? extends Annotation> type;
    private final Map<String, Object> attributes;

    AnnotationInvocationHandler(Class<? extends Annotation> type, Map<String, Object> attributes) {
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
      Object result = attributes.get(member);

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
        if (firstMember)
          firstMember = false;
        else
          result.append(", ");

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
      if (!type.isArray())    // primitive, string, class, enum const,
        // or annotation
        return value.toString();

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
      if (o == this)
        return true;

      if (!type.isInstance(o))
        return false;
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
        if (!memberValueEquals(ourValue, hisValue))
          return false;
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
        if (handler instanceof AnnotationInvocationHandler)
          return (AnnotationInvocationHandler) handler;
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
      if (!type.isArray())
        return v1.equals(v2);

      // Check for array of string, class, enum const, annotation,
      // or ExceptionProxy
      if (v1 instanceof Object[] && v2 instanceof Object[])
        return Arrays.equals((Object[]) v1, (Object[]) v2);

      // Check for ill formed annotation(s)
      if (v2.getClass() != type)
        return false;

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
        if (method.getModifiers() != (Modifier.PUBLIC | Modifier.ABSTRACT) ||
                method.isDefault() ||
                method.getParameterCount() != 0 ||
                method.getExceptionTypes().length != 0) {
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

        if (!((returnType.isPrimitive() && returnType != void.class) ||
                returnType == java.lang.String.class ||
                returnType == java.lang.Class.class ||
                returnType.isEnum() ||
                returnType.isAnnotation())) {
          valid = false;
          break;
        }

        /*
         * "It is a compile-time error if any method declared in an
         * annotation type has a signature that is
         * override-equivalent to that of any public or protected
         * method declared in class Object or in the interface
         * java.lang.annotation.Annotation."
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
        if ((methodName.equals("toString") && returnType == java.lang.String.class) ||
                (methodName.equals("hashCode") && returnType == int.class) ||
                (methodName.equals("annotationType") && returnType == java.lang.Class.class)) {
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
      if (!type.isArray())    // primitive, string, class, enum const,
        // or annotation
        return value.hashCode();

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

}

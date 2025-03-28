/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.bytecode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import infra.bytecode.commons.MethodSignature;
import infra.lang.Constant;
import infra.lang.NonNull;
import infra.lang.Nullable;

/**
 * A Java field or method type. This class can be used to make it easier to manipulate type and
 * method descriptors.
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public final class Type {

  /** The sort of the {@code void} type. See {@link #getSort}. */
  public static final int VOID = 0;

  /** The sort of the {@code boolean} type. See {@link #getSort}. */
  public static final int BOOLEAN = 1;

  /** The sort of the {@code char} type. See {@link #getSort}. */
  public static final int CHAR = 2;

  /** The sort of the {@code byte} type. See {@link #getSort}. */
  public static final int BYTE = 3;

  /** The sort of the {@code short} type. See {@link #getSort}. */
  public static final int SHORT = 4;

  /** The sort of the {@code int} type. See {@link #getSort}. */
  public static final int INT = 5;

  /** The sort of the {@code float} type. See {@link #getSort}. */
  public static final int FLOAT = 6;

  /** The sort of the {@code long} type. See {@link #getSort}. */
  public static final int LONG = 7;

  /** The sort of the {@code double} type. See {@link #getSort}. */
  public static final int DOUBLE = 8;

  /** The sort of array reference types. See {@link #getSort}. */
  public static final int ARRAY = 9;

  /** The sort of object reference types. See {@link #getSort}. */
  public static final int OBJECT = 10;

  /** The sort of method types. See {@link #getSort}. */
  public static final int METHOD = 11;

  /** The (private) sort of object reference types represented with an internal name. */
  private static final int INTERNAL = 12;

  /** The descriptors of the primitive types. */
  private static final String PRIMITIVE_DESCRIPTORS = "VZCBSIFJD";

  /** The {@code void} type. */
  public static final Type VOID_TYPE = new Type(VOID, PRIMITIVE_DESCRIPTORS, VOID, VOID + 1);

  /** The {@code boolean} type. */
  public static final Type BOOLEAN_TYPE =
          new Type(BOOLEAN, PRIMITIVE_DESCRIPTORS, BOOLEAN, BOOLEAN + 1);

  /** The {@code char} type. */
  public static final Type CHAR_TYPE = new Type(CHAR, PRIMITIVE_DESCRIPTORS, CHAR, CHAR + 1);

  /** The {@code byte} type. */
  public static final Type BYTE_TYPE = new Type(BYTE, PRIMITIVE_DESCRIPTORS, BYTE, BYTE + 1);

  /** The {@code short} type. */
  public static final Type SHORT_TYPE = new Type(SHORT, PRIMITIVE_DESCRIPTORS, SHORT, SHORT + 1);

  /** The {@code int} type. */
  public static final Type INT_TYPE = new Type(INT, PRIMITIVE_DESCRIPTORS, INT, INT + 1);

  /** The {@code float} type. */
  public static final Type FLOAT_TYPE = new Type(FLOAT, PRIMITIVE_DESCRIPTORS, FLOAT, FLOAT + 1);

  /** The {@code long} type. */
  public static final Type LONG_TYPE = new Type(LONG, PRIMITIVE_DESCRIPTORS, LONG, LONG + 1);

  /** The {@code double} type. */
  public static final Type DOUBLE_TYPE =
          new Type(DOUBLE, PRIMITIVE_DESCRIPTORS, DOUBLE, DOUBLE + 1);

  /** The descriptors of the primitive Java types (plus void). */
  private static final Map<String, String> PRIMITIVE_TYPE_DESCRIPTORS = Map.of(
          "void", "V",
          "byte", "B",
          "char", "C",
          "double", "D",
          "float", "F",
          "int", "I",
          "long", "J",
          "short", "S",
          "boolean", "Z"
  );

  public static final Type TYPE_TYPE = Type.forClass(Type.class);
  public static final Type TYPE_CONSTANT = Type.forClass(Constant.class);
  public static final Type TYPE_OBJECT_ARRAY = Type.forInternalName("[Ljava/lang/Object;");
  public static final Type TYPE_CLASS_ARRAY = Type.forInternalName("[Ljava/lang/Class;");
  public static final Type TYPE_STRING_ARRAY = Type.forInternalName("[Ljava/lang/String;");
  public static final Type TYPE_ERROR = Type.forInternalName("java/lang/Error");
  public static final Type TYPE_SYSTEM = Type.forInternalName("java/lang/System");
  public static final Type TYPE_LONG = Type.forInternalName("java/lang/Long");
  public static final Type TYPE_BYTE = Type.forInternalName("java/lang/Byte");
  public static final Type TYPE_CLASS = Type.forInternalName("java/lang/Class");
  public static final Type TYPE_FLOAT = Type.forInternalName("java/lang/Float");
  public static final Type TYPE_SHORT = Type.forInternalName("java/lang/Short");
  public static final Type TYPE_DOUBLE = Type.forInternalName("java/lang/Double");
  public static final Type TYPE_STRING = Type.forInternalName("java/lang/String");
  public static final Type TYPE_NUMBER = Type.forInternalName("java/lang/Number");
  public static final Type TYPE_BOOLEAN = Type.forInternalName("java/lang/Boolean");
  public static final Type TYPE_INTEGER = Type.forInternalName("java/lang/Integer");
  public static final Type TYPE_CHARACTER = Type.forInternalName("java/lang/Character");
  public static final Type TYPE_THROWABLE = Type.forInternalName("java/lang/Throwable");
  public static final Type TYPE_CLASS_LOADER = Type.forInternalName("java/lang/ClassLoader");
  public static final Type TYPE_RUNTIME_EXCEPTION = Type.forInternalName("java/lang/RuntimeException");
  public static final Type TYPE_SIGNATURE = Type.forClass(MethodSignature.class);

  /** The type of the java.lang.Object class. */
  public static final Type TYPE_OBJECT = Type.forInternalName("java/lang/Object");
  public static final Type[] EMPTY_ARRAY = {};

  // -----------------------------------------------------------------------------------------------
  // Fields
  // -----------------------------------------------------------------------------------------------

  /**
   * The sort of this type. Either {@link #VOID}, {@link #BOOLEAN}, {@link #CHAR}, {@link #BYTE},
   * {@link #SHORT}, {@link #INT}, {@link #FLOAT}, {@link #LONG}, {@link #DOUBLE}, {@link #ARRAY},
   * {@link #OBJECT}, {@link #METHOD} or {@link #INTERNAL}.
   */
  private final int sort;

  /**
   * A buffer containing the value of this field or method type. This value is an internal name for
   * {@link #OBJECT} and {@link #INTERNAL} types, and a field or method descriptor in the other
   * cases.
   *
   * <p>For {@link #OBJECT} types, this field also contains the descriptor: the characters in
   * [{@link #valueBegin},{@link #valueEnd}) contain the internal name, and those in [{@link
   * #valueBegin} - 1, {@link #valueEnd} + 1) contain the descriptor.
   */
  private final String valueBuffer;

  /**
   * The beginning index, inclusive, of the value of this Java field or method type in {@link
   * #valueBuffer}. This value is an internal name for {@link #OBJECT} and {@link #INTERNAL} types,
   * and a field or method descriptor in the other cases.
   */
  private final int valueBegin;

  /**
   * The end index, exclusive, of the value of this Java field or method type in {@link
   * #valueBuffer}. This value is an internal name for {@link #OBJECT} and {@link #INTERNAL} types,
   * and a field or method descriptor in the other cases.
   */
  private final int valueEnd;

  /**
   * Constructs a reference type.
   *
   * @param sort the sort of this type, see {@link #sort}.
   * @param valueBuffer a buffer containing the value of this field or method type.
   * @param valueBegin the beginning index, inclusive, of the value of this field or method type in
   * valueBuffer.
   * @param valueEnd the end index, exclusive, of the value of this field or method type in
   * valueBuffer.
   */
  private Type(final int sort, final String valueBuffer, final int valueBegin, final int valueEnd) {
    this.sort = sort;
    this.valueBuffer = valueBuffer;
    this.valueBegin = valueBegin;
    this.valueEnd = valueEnd;
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to get Type(s) from a descriptor, a reflected Method or Constructor, other types, etc.
  // -----------------------------------------------------------------------------------------------

  /**
   * @since 4.0
   */
  public static Type parse(String s) {
    return forDescriptor(getDescriptor(s));
  }

  /**
   * @since 4.0
   */
  @Nullable
  public static String resolvePrimitiveTypeDescriptor(String type) {
    return PRIMITIVE_TYPE_DESCRIPTORS.get(type);
  }

  /**
   * Returns the {@link Type} corresponding to the given type descriptor.
   *
   * @param typeDescriptor a field or method type descriptor.
   * @return the {@link Type} corresponding to the given type descriptor.
   */
  public static Type forDescriptor(final String typeDescriptor) {
    return getTypeInternal(typeDescriptor, 0, typeDescriptor.length());
  }

  /**
   * Returns the {@link Type} corresponding to the given class.
   *
   * @param clazz a class.
   * @return the {@link Type} corresponding to the given class.
   */
  public static Type forClass(final Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (clazz == int.class) {
        return INT_TYPE;
      }
      else if (clazz == void.class) {
        return VOID_TYPE;
      }
      else if (clazz == boolean.class) {
        return BOOLEAN_TYPE;
      }
      else if (clazz == byte.class) {
        return BYTE_TYPE;
      }
      else if (clazz == char.class) {
        return CHAR_TYPE;
      }
      else if (clazz == short.class) {
        return SHORT_TYPE;
      }
      else if (clazz == double.class) {
        return DOUBLE_TYPE;
      }
      else if (clazz == float.class) {
        return FLOAT_TYPE;
      }
      else if (clazz == long.class) {
        return LONG_TYPE;
      }
      else {
        throw new AssertionError();
      }
    }
    else {
      return forDescriptor(getDescriptor(clazz));
    }
  }

  /**
   * Returns the method {@link Type} corresponding to the given constructor.
   *
   * @param constructor a {@link Constructor} object.
   * @return the method {@link Type} corresponding to the given constructor.
   */
  public static Type forConstructor(final Constructor<?> constructor) {
    return forDescriptor(getConstructorDescriptor(constructor));
  }

  /**
   * Returns the method {@link Type} corresponding to the given method.
   *
   * @param method a {@link Method} object.
   * @return the method {@link Type} corresponding to the given method.
   */
  public static Type forMethod(final Method method) {
    return forDescriptor(getMethodDescriptor(method));
  }

  /**
   * Returns the type of the elements of this array type. This method should only be used for an
   * array type.
   *
   * @return Returns the type of the elements of this array type.
   */
  public Type getElementType() {
    final int numDimensions = getDimensions();
    return getTypeInternal(valueBuffer, valueBegin + numDimensions, valueEnd);
  }

  /**
   * Returns the {@link Type} corresponding to the given internal name.
   *
   * @param internalName an internal name.
   * @return the {@link Type} corresponding to the given internal name.
   */
  public static Type forInternalName(final String internalName) {
    return new Type(
            internalName.charAt(0) == '[' ? ARRAY : INTERNAL, internalName, 0, internalName.length());
  }

  /**
   * @since 4.0
   */
  public static Type[] forInternalNames(@Nullable String[] names) {
    if (names == null) {
      return null;
    }
    Type[] types = new Type[names.length];
    for (int i = 0; i < names.length; i++) {
      types[i] = forInternalName(names[i]);
    }
    return types;
  }

  /**
   * Returns the {@link Type}s corresponding to the given internal name.
   *
   * @param internalNames internal name. if null returns null
   * @return the {@link Type}s corresponding to the given internal name.
   */
  public static Type[] forObjectTypes(@Nullable String[] internalNames) {
    if (internalNames == null) {
      return null;
    }
    Type[] ret = new Type[internalNames.length];
    int i = 0;
    for (final String internalName : internalNames) {
      ret[i++] = forInternalName(internalName);
    }
    return ret;
  }

  /**
   * @param member Member
   * @return the {@link Type}s corresponding to the given Executable's ExceptionTypes.
   * @throws IllegalArgumentException not a Executable
   * @see Executable#getExceptionTypes()
   * @since 4.0
   */
  public static Type[] forExceptionTypes(Member member) {
    if (member instanceof Executable) {
      return Type.getTypes(((Executable) member).getExceptionTypes());
    }
    throw new IllegalArgumentException(member + " is not a Executable");
  }

  /**
   * Returns the {@link Type} corresponding to the given method descriptor. Equivalent to <code>
   * Type.getType(methodDescriptor)</code>.
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} corresponding to the given method descriptor.
   */
  public static Type forMethod(final String methodDescriptor) {
    return new Type(METHOD, methodDescriptor, 0, methodDescriptor.length());
  }

  /**
   * Returns the method {@link Type} corresponding to the given argument and return types.
   *
   * @param returnType the return type of the method.
   * @param argumentTypes the argument types of the method.
   * @return the method {@link Type} corresponding to the given argument and return types.
   */
  public static Type forMethod(final Type returnType, final Type... argumentTypes) {
    return forDescriptor(getMethodDescriptor(returnType, argumentTypes));
  }

  /**
   * Returns the {@link Type} values corresponding to the argument types of the given method
   * descriptor.
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} values corresponding to the argument types of the given method
   * descriptor.
   */
  public static Type[] forArgumentTypes(final String methodDescriptor) {
    // First step: compute the number of argument types in methodDescriptor.
    int numArgumentTypes = getArgumentCount(methodDescriptor);

    // Second step: create a Type instance for each argument type.
    Type[] argumentTypes = new Type[numArgumentTypes];
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Parse and create the argument types, one at each loop iteration.
    int currentArgumentTypeIndex = 0;
    while (methodDescriptor.charAt(currentOffset) != ')') {
      final int currentArgumentTypeOffset = currentOffset;
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      argumentTypes[currentArgumentTypeIndex++] =
              getTypeInternal(methodDescriptor, currentArgumentTypeOffset, currentOffset);
    }
    return argumentTypes;
  }

  /**
   * Returns the {@link Type} values corresponding to the argument types of the given method.
   *
   * @param method a method.
   * @return the {@link Type} values corresponding to the argument types of the given method.
   */
  public static Type[] forArgumentTypes(final Method method) {
    Class<?>[] classes = method.getParameterTypes();
    Type[] types = new Type[classes.length];
    for (int i = classes.length - 1; i >= 0; --i) {
      types[i] = forClass(classes[i]);
    }
    return types;
  }

  /**
   * Returns the {@link Type} corresponding to the return type of the given method descriptor.
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} corresponding to the return type of the given method descriptor.
   */
  public static Type forReturnType(final String methodDescriptor) {
    return getTypeInternal(
            methodDescriptor, getReturnTypeOffset(methodDescriptor), methodDescriptor.length());
  }

  /**
   * Returns the {@link Type} corresponding to the return type of the given method.
   *
   * @param method a method.
   * @return the {@link Type} corresponding to the return type of the given method.
   */
  public static Type forReturnType(final Method method) {
    return forClass(method.getReturnType());
  }

  /**
   * Returns the start index of the return type of the given method descriptor.
   *
   * @param methodDescriptor a method descriptor.
   * @return the start index of the return type of the given method descriptor.
   */
  static int getReturnTypeOffset(final String methodDescriptor) {
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Skip the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
    }
    return currentOffset + 1;
  }

  /**
   * Returns the {@link Type} corresponding to the given field or method descriptor.
   *
   * @param descriptorBuffer a buffer containing the field or method descriptor.
   * @param descriptorBegin the beginning index, inclusive, of the field or method descriptor in
   * descriptorBuffer.
   * @param descriptorEnd the end index, exclusive, of the field or method descriptor in
   * descriptorBuffer.
   * @return the {@link Type} corresponding to the given type descriptor.
   */
  private static Type getTypeInternal(
          final String descriptorBuffer, final int descriptorBegin, final int descriptorEnd) {
    return switch (descriptorBuffer.charAt(descriptorBegin)) {
      case 'V' -> VOID_TYPE;
      case 'Z' -> BOOLEAN_TYPE;
      case 'C' -> CHAR_TYPE;
      case 'B' -> BYTE_TYPE;
      case 'S' -> SHORT_TYPE;
      case 'I' -> INT_TYPE;
      case 'F' -> FLOAT_TYPE;
      case 'J' -> LONG_TYPE;
      case 'D' -> DOUBLE_TYPE;
      case '[' -> new Type(ARRAY, descriptorBuffer, descriptorBegin, descriptorEnd);
      case 'L' -> new Type(OBJECT, descriptorBuffer, descriptorBegin + 1, descriptorEnd - 1);
      case '(' -> new Type(METHOD, descriptorBuffer, descriptorBegin, descriptorEnd);
      default -> throw new IllegalArgumentException("Invalid descriptor: " + descriptorBuffer);
    };
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to get class names, internal names or descriptors.
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the return type of methods of this type. This method should only be used for method
   * types.
   *
   * @return the return type of methods of this type.
   */
  public Type getReturnType() {
    return forReturnType(getDescriptor());
  }

  /**
   * Returns the argument types of methods of this type. This method should only be used for method
   * types.
   *
   * @return the argument types of methods of this type.
   */
  public Type[] getArgumentTypes() {
    return forArgumentTypes(getDescriptor());
  }

  /**
   * Returns the binary name of the class corresponding to this type. This method must not be used
   * on method types.
   *
   * @return the binary name of the class corresponding to this type.
   */
  public String getClassName() {
    return switch (sort) {
      case INT -> "int";
      case VOID -> "void";
      case CHAR -> "char";
      case BYTE -> "byte";
      case LONG -> "long";
      case SHORT -> "short";
      case FLOAT -> "float";
      case DOUBLE -> "double";
      case BOOLEAN -> "boolean";
      case OBJECT, INTERNAL -> valueBuffer.substring(valueBegin, valueEnd).replace('/', '.');
      case ARRAY -> getElementType().getClassName() + "[]".repeat(Math.max(0, getDimensions()));
      default -> throw new AssertionError();
    };
  }

  /**
   * Returns the internal name of the class corresponding to this object or array type. The internal
   * name of a class is its fully qualified name (as returned by Class.getName(), where '.' are
   * replaced by '/'). This method should only be used for an object or array type.
   *
   * @return the internal name of the class corresponding to this object type.
   */
  public String getInternalName() {
    return valueBuffer.substring(valueBegin, valueEnd);
  }

  /**
   * Returns the internal name of the given class. The internal name of a class is its fully
   * qualified name, as returned by Class.getName(), where '.' are replaced by '/'.
   *
   * @param clazz an object or array class.
   * @return the internal name of the given class.
   */
  public static String getInternalName(final Class<?> clazz) {
    return clazz.getName().replace('.', '/');
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to get the descriptor
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the descriptor corresponding to this type.
   *
   * @return the descriptor corresponding to this type.
   */
  public String getDescriptor() {
    if (sort == OBJECT) {
      return valueBuffer.substring(valueBegin - 1, valueEnd + 1);
    }
    else if (sort == INTERNAL) {
      return 'L' + valueBuffer.substring(valueBegin, valueEnd) + ';';
    }
    else {
      return valueBuffer.substring(valueBegin, valueEnd);
    }
  }

  /**
   * Returns the descriptor corresponding to the given class.
   *
   * @param clazz an object class, a primitive class or an array class.
   * @return the descriptor corresponding to the given class.
   */
  public static String getDescriptor(final Class<?> clazz) {
    StringBuilder stringBuilder = new StringBuilder();
    appendDescriptor(clazz, stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * Returns the descriptor corresponding to the given constructor.
   *
   * @param constructor a {@link Constructor} object.
   * @return the descriptor of the given constructor.
   */
  public static String getConstructorDescriptor(final Constructor<?> constructor) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = constructor.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    return stringBuilder.append(")V").toString();
  }

  /**
   * Returns the descriptor corresponding to the given argument and return types.
   *
   * @param returnType the return type of the method.
   * @param argumentTypes the argument types of the method.
   * @return the descriptor corresponding to the given argument and return types.
   */
  public static String getMethodDescriptor(final Type returnType, final Type... argumentTypes) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    for (Type argumentType : argumentTypes) {
      argumentType.appendDescriptor(stringBuilder);
    }
    stringBuilder.append(')');
    returnType.appendDescriptor(stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * Returns the descriptor corresponding to the given method.
   *
   * @param method a {@link Method} object.
   * @return the descriptor of the given method.
   */
  public static String getMethodDescriptor(final Method method) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = method.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    stringBuilder.append(')');
    appendDescriptor(method.getReturnType(), stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * <pre>
   *  Object -> Ljava/lang/Object;
   *  Object, Object ,Class -> Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Class;
   * </pre>
   *
   * @since 4.0
   */
  public static String getDescriptor(String parameterTypes) {
    return getDescriptor(parameterTypes, 0, parameterTypes.length(), false);
  }

  /**
   * <pre>
   *  Object -> Ljava/lang/Object;
   *  Object, Object ,Class -> Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Class;
   * </pre>
   */
  @NonNull
  public static String getDescriptor(
          String parameterTypes, int startIdx, int endIdx, boolean defaultPackage) {
    StringBuilder argDescriptor = new StringBuilder(parameterTypes.length() + 16);
    int splitIndex = parameterTypes.indexOf(',');// ,'s index
    while (splitIndex != -1) {
      // array -> Object, Object, Class
      argDescriptor.append(
              getDescriptor(parameterTypes.substring(startIdx, splitIndex).trim(), defaultPackage)
      );
      startIdx = splitIndex + 1;
      splitIndex = parameterTypes.indexOf(',', startIdx);
    }

    argDescriptor.append(
            getDescriptor(parameterTypes.substring(startIdx, endIdx).trim(), defaultPackage)
    );
    return argDescriptor.toString();
  }

  /**
   * Returns the descriptor corresponding to the given type name.
   *
   * @param type a Java type name.
   * @param defaultPackage true if unqualified class names belong to the default package, or false
   * if they correspond to java.lang classes. For instance "Object" means "Object" if this
   * option is true, or "java.lang.Object" otherwise.
   * @return the descriptor corresponding to the given type name.
   */
  public static String getDescriptor(final String type, final boolean defaultPackage) {
    if (Constant.BLANK.equals(type)) {
      return type;
    }

    StringBuilder stringBuilder = new StringBuilder();
    int arrayBracketsIndex = 0;
    while ((arrayBracketsIndex = type.indexOf("[]", arrayBracketsIndex) + 1) > 0) {
      stringBuilder.append('[');
    }

    String elementType = type.substring(0, type.length() - stringBuilder.length() * 2);
    String descriptor = PRIMITIVE_TYPE_DESCRIPTORS.get(elementType);
    if (descriptor != null) {
      stringBuilder.append(descriptor);
    }
    else {
      stringBuilder.append('L');
      if (elementType.indexOf('.') < 0) {
        if (!defaultPackage) {
          stringBuilder.append("java/lang/");
        }
        stringBuilder.append(elementType);
      }
      else {
        stringBuilder.append(elementType.replace('.', '/'));
      }
      stringBuilder.append(';');
    }
    return stringBuilder.toString();
  }

  /**
   * Appends the descriptor corresponding to this type to the given string buffer.
   *
   * @param stringBuilder the string builder to which the descriptor must be appended.
   */
  private void appendDescriptor(final StringBuilder stringBuilder) {
    if (sort == OBJECT) {
      stringBuilder.append(valueBuffer, valueBegin - 1, valueEnd + 1);
    }
    else if (sort == INTERNAL) {
      stringBuilder.append('L').append(valueBuffer, valueBegin, valueEnd).append(';');
    }
    else {
      stringBuilder.append(valueBuffer, valueBegin, valueEnd);
    }
  }

  /**
   * Appends the descriptor of the given class to the given string builder.
   *
   * @param clazz the class whose descriptor must be computed.
   * @param stringBuilder the string builder to which the descriptor must be appended.
   */
  private static void appendDescriptor(final Class<?> clazz, final StringBuilder stringBuilder) {
    Class<?> currentClass = clazz;
    while (currentClass.isArray()) {
      stringBuilder.append('[');
      currentClass = currentClass.getComponentType();
    }
    if (currentClass.isPrimitive()) {
      char descriptor;
      if (currentClass == int.class) {
        descriptor = 'I';
      }
      else if (currentClass == void.class) {
        descriptor = 'V';
      }
      else if (currentClass == boolean.class) {
        descriptor = 'Z';
      }
      else if (currentClass == byte.class) {
        descriptor = 'B';
      }
      else if (currentClass == char.class) {
        descriptor = 'C';
      }
      else if (currentClass == short.class) {
        descriptor = 'S';
      }
      else if (currentClass == double.class) {
        descriptor = 'D';
      }
      else if (currentClass == float.class) {
        descriptor = 'F';
      }
      else if (currentClass == long.class) {
        descriptor = 'J';
      }
      else {
        throw new AssertionError();
      }
      stringBuilder.append(descriptor);
    }
    else {
      stringBuilder.append('L').append(getInternalName(currentClass)).append(';');
    }
  }

  /**
   * @since 4.0
   */
  public Type getComponentType() {
    if (isArray()) {
      return forDescriptor(getDescriptor().substring(1));
    }
    throw new IllegalArgumentException("Type " + this + " is not an array");
  }

  /**
   * @since 4.0
   */
  public String emulateClassGetName() {
    if (isArray()) {
      return getDescriptor().replace('/', '.');
    }
    return getClassName();
  }

  /**
   * @since 4.0
   */
  @Nullable
  public static String[] toInternalNames(@Nullable Type... types) {
    if (types == null) {
      return null;
    }
    String[] names = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      names[i] = types[i].getInternalName();
    }
    return names;
  }

  /**
   * get Boxed Type
   *
   * @return Boxed Type
   * @since 4.0
   */
  public Type getBoxedType() {
    return switch (getSort()) {
      case Type.CHAR -> Type.TYPE_CHARACTER;
      case Type.BOOLEAN -> Type.TYPE_BOOLEAN;
      case Type.DOUBLE -> Type.TYPE_DOUBLE;
      case Type.FLOAT -> Type.TYPE_FLOAT;
      case Type.LONG -> Type.TYPE_LONG;
      case Type.INT -> Type.TYPE_INTEGER;
      case Type.SHORT -> Type.TYPE_SHORT;
      case Type.BYTE -> Type.TYPE_BYTE;
      default -> this;
    };
  }

  /**
   * @since 4.0
   */
  public Type getUnboxedType() {
    if (Type.TYPE_INTEGER.equals(this)) {
      return Type.INT_TYPE;
    }
    else if (Type.TYPE_BOOLEAN == this) {
      return Type.BOOLEAN_TYPE;
    }
    else if (Type.TYPE_DOUBLE.equals(this)) {
      return Type.DOUBLE_TYPE;
    }
    else if (Type.TYPE_LONG.equals(this)) {
      return Type.LONG_TYPE;
    }
    else if (Type.TYPE_CHARACTER.equals(this)) {
      return Type.CHAR_TYPE;
    }
    else if (Type.TYPE_BYTE.equals(this)) {
      return Type.BYTE_TYPE;
    }
    else if (Type.TYPE_FLOAT.equals(this)) {
      return Type.FLOAT_TYPE;
    }
    else if (Type.TYPE_SHORT.equals(this)) {
      return Type.SHORT_TYPE;
    }
    else {
      return this;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to get the sort, dimension, size, and opcodes corresponding to a Type or descriptor.
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the sort of this type.
   *
   * @return {@link #VOID}, {@link #BOOLEAN}, {@link #CHAR}, {@link #BYTE}, {@link #SHORT}, {@link
   * #INT}, {@link #FLOAT}, {@link #LONG}, {@link #DOUBLE}, {@link #ARRAY}, {@link #OBJECT} or
   * {@link #METHOD}.
   */
  public int getSort() {
    return sort == INTERNAL ? OBJECT : sort;
  }

  /**
   * Returns the number of dimensions of this array type. This method should only be used for an
   * array type.
   *
   * @return the number of dimensions of this array type.
   */
  public int getDimensions() {
    int numDimensions = 1;
    while (valueBuffer.charAt(valueBegin + numDimensions) == '[') {
      numDimensions++;
    }
    return numDimensions;
  }

  /**
   * Returns the size of values of this type. This method must not be used for method types.
   *
   * @return the size of values of this type, i.e., 2 for {@code long} and {@code double}, 0 for
   * {@code void} and 1 otherwise.
   */
  public int getSize() {
    return switch (sort) {
      case VOID -> 0;
      case LONG, DOUBLE -> 2;
      case BOOLEAN, CHAR, BYTE, SHORT, INT, FLOAT, ARRAY, OBJECT, INTERNAL -> 1;
      default -> throw new AssertionError();
    };
  }

  /**
   * Returns the number of arguments of this method type. This method should only be used for method
   * types.
   *
   * @return the number of arguments of this method type. Each argument counts for 1, even long and
   * double ones. The implicit @literal{this} argument is not counted.
   */
  public int getArgumentCount() {
    return getArgumentCount(getDescriptor());
  }

  /**
   * Returns the number of arguments in the given method descriptor.
   *
   * @param methodDescriptor a method descriptor.
   * @return the number of arguments in the given method descriptor. Each argument counts for 1,
   * even long and double ones. The implicit @literal{this} argument is not counted.
   */
  public static int getArgumentCount(final String methodDescriptor) {
    int argumentCount = 0;
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Parse the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      ++argumentCount;
    }
    return argumentCount;
  }

  /**
   * Returns the size of the arguments and of the return value of methods of this type. This method
   * should only be used for method types.
   *
   * @return the size of the arguments of the method (plus one for the implicit this argument),
   * argumentsSize, and the size of its return value, returnSize, packed into a single int i =
   * {@code (argumentsSize &lt;&lt; 2) | returnSize} (argumentsSize is therefore equal to {@code
   * i &gt;&gt; 2}, and returnSize to {@code i &amp; 0x03}). Long and double values have size 2,
   * the others have size 1.
   */
  public int getArgumentsAndReturnSizes() {
    return getArgumentsAndReturnSizes(getDescriptor());
  }

  /**
   * Computes the size of the arguments and of the return value of a method.
   *
   * @param methodDescriptor a method descriptor.
   * @return the size of the arguments of the method (plus one for the implicit this argument),
   * argumentsSize, and the size of its return value, returnSize, packed into a single int i =
   * {@code (argumentsSize &lt;&lt; 2) | returnSize} (argumentsSize is therefore equal to {@code
   * i &gt;&gt; 2}, and returnSize to {@code i &amp; 0x03}). Long and double values have size 2,
   * the others have size 1.
   */
  public static int getArgumentsAndReturnSizes(final String methodDescriptor) {
    int argumentsSize = 1;
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    int currentChar = methodDescriptor.charAt(currentOffset);
    // Parse the argument types and compute their size, one at a each loop iteration.
    while (currentChar != ')') {
      if (currentChar == 'J' || currentChar == 'D') {
        currentOffset++;
        argumentsSize += 2;
      }
      else {
        while (methodDescriptor.charAt(currentOffset) == '[') {
          currentOffset++;
        }
        if (methodDescriptor.charAt(currentOffset++) == 'L') {
          // Skip the argument descriptor content.
          int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
          currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
        }
        argumentsSize += 1;
      }
      currentChar = methodDescriptor.charAt(currentOffset);
    }
    currentChar = methodDescriptor.charAt(currentOffset + 1);
    if (currentChar == 'V') {
      return argumentsSize << 2;
    }
    else {
      int returnSize = (currentChar == 'J' || currentChar == 'D') ? 2 : 1;
      return argumentsSize << 2 | returnSize;
    }
  }

  /**
   * Returns a JVM instruction opcode adapted to this {@link Type}. This method must not be used for
   * method types.
   *
   * @param opcode a JVM instruction opcode. This opcode must be one of ILOAD, ISTORE, IALOAD,
   * IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR and
   * IRETURN.
   * @return an opcode that is similar to the given opcode, but adapted to this {@link Type}. For
   * example, if this type is {@code float} and {@code opcode} is IRETURN, this method returns
   * FRETURN.
   */
  public int getOpcode(final int opcode) {
    if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
      return switch (sort) {
        case INT -> opcode;
        case CHAR -> opcode + (Opcodes.CALOAD - Opcodes.IALOAD);
        case LONG -> opcode + (Opcodes.LALOAD - Opcodes.IALOAD);
        case SHORT -> opcode + (Opcodes.SALOAD - Opcodes.IALOAD);
        case FLOAT -> opcode + (Opcodes.FALOAD - Opcodes.IALOAD);
        case DOUBLE -> opcode + (Opcodes.DALOAD - Opcodes.IALOAD);
        case BOOLEAN, BYTE -> opcode + (Opcodes.BALOAD - Opcodes.IALOAD);
        case ARRAY, OBJECT, INTERNAL -> opcode + (Opcodes.AALOAD - Opcodes.IALOAD);
        case METHOD, VOID -> throw new UnsupportedOperationException();
        default -> throw new AssertionError();
      };
    }
    else {
      switch (sort) {
        case VOID:
          if (opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return Opcodes.RETURN;
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
          return opcode;
        case FLOAT:
          return opcode + (Opcodes.FRETURN - Opcodes.IRETURN);
        case LONG:
          return opcode + (Opcodes.LRETURN - Opcodes.IRETURN);
        case DOUBLE:
          return opcode + (Opcodes.DRETURN - Opcodes.IRETURN);
        case ARRAY:
        case OBJECT:
        case INTERNAL:
          if (opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return opcode + (Opcodes.ARETURN - Opcodes.IRETURN);
        case METHOD:
          throw new UnsupportedOperationException();
        default:
          throw new AssertionError();
      }
    }
  }

  /**
   * To {@link Type} array
   *
   * @param items array item
   * @return {@link Type} array
   */
  public static Type[] array(final Type... items) {
    return items;
  }

  /**
   * @since 4.0
   */
  public static Type[] getTypes(@Nullable final Class<?>... items) {
    if (items == null) {
      return null;
    }
    int i = 0;
    Type[] ret = new Type[items.length];
    for (final Class<?> item : items) {
      ret[i++] = forClass(item);
    }
    return ret;
  }

  /**
   * @since 4.0
   */
  public static Type[] getTypes(String... items) {
    if (items == null) {
      return null;
    }
    int i = 0;
    Type[] ret = new Type[items.length];
    for (final String item : items) {
      ret[i++] = forDescriptor(item);
    }
    return ret;
  }

  /**
   * @since 4.0
   */
  public static int getStackSize(Type[] types) {
    int size = 0;
    for (final Type type : types) {
      size += type.getSize();
    }
    return size;
  }
  // isArray

  /**
   * Determines if this {@code Type} object represents an array class.
   *
   * @return {@code true} if this object represents an array class;
   * {@code false} otherwise.
   * @since 4.0
   */
  public boolean isArray() {
    return sort == ARRAY;
  }

  /**
   * Determines if the specified {@code Type} object represents a
   * primitive type.
   *
   * <p> There are nine predefined {@code Class} objects to represent
   * the eight primitive types and void.  These are created by the Java
   * Virtual Machine, and have the same names as the primitive types that
   * they represent, namely {@code boolean}, {@code byte},
   * {@code char}, {@code short}, {@code int},
   * {@code long}, {@code float}, and {@code double}.
   *
   * <p> These objects may only be accessed via the following public static
   * final variables, and are the only {@code Class} objects for which
   * this method returns {@code true}.
   *
   * @return true if and only if this class represents a primitive type
   * @see java.lang.Boolean#TYPE
   * @see java.lang.Character#TYPE
   * @see java.lang.Byte#TYPE
   * @see java.lang.Short#TYPE
   * @see java.lang.Integer#TYPE
   * @see java.lang.Long#TYPE
   * @see java.lang.Float#TYPE
   * @see java.lang.Double#TYPE
   * @see java.lang.Void#TYPE
   * @since 4.0
   */
  public boolean isPrimitive() {
    final int sort = getSort();
    return sort != ARRAY && sort != OBJECT;
  }

  // -----------------------------------------------------------------------------------------------
  // Equals, hashCode and toString.
  // -----------------------------------------------------------------------------------------------

  /**
   * Tests if the given object is equal to this type.
   *
   * @param object the object to be compared to this type.
   * @return {@literal true} if the given object is equal to this type.
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Type other)) {
      return false;
    }
    if ((sort == INTERNAL ? OBJECT : sort) != (other.sort == INTERNAL ? OBJECT : other.sort)) {
      return false;
    }
    int begin = valueBegin;
    int end = valueEnd;
    int otherBegin = other.valueBegin;
    // Compare the values.
    if (end - begin != other.valueEnd - otherBegin) {
      return false;
    }
    for (int i = begin, j = otherBegin; i < end; i++, j++) {
      if (this.valueBuffer.charAt(i) != other.valueBuffer.charAt(j)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a hash code value for this type.
   *
   * @return a hash code value for this type.
   */
  @Override
  public int hashCode() {
    int hashCode = 13 * (sort == INTERNAL ? OBJECT : sort);
    if (sort >= ARRAY) {
      for (int i = valueBegin; i < valueEnd; i++) {
        hashCode = 17 * (hashCode + valueBuffer.charAt(i));
      }
    }
    return hashCode;
  }

  /**
   * Returns a string representation of this type.
   *
   * @return the descriptor of this type.
   */
  @Override
  public String toString() {
    return getDescriptor();
  }

}

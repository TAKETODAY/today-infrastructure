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
package infra.util;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TimeZone;

import infra.core.conversion.ConversionException;
import infra.core.conversion.support.DefaultConversionService;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.lang.Contract;
import infra.lang.Nullable;

/**
 * Miscellaneous object utility methods.
 *
 * <p>Mainly for internal use within the framework.
 *
 * <p>Thanks to Alex Ruiz for contributing several enhancements to this class!
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClassUtils
 * @see CollectionUtils
 * @see StringUtils
 * @since 2019-08-23 00:16
 */
public abstract class ObjectUtils {

  private static final String NULL_STRING = "null";
  private static final String ARRAY_START = "{";
  private static final String ARRAY_END = "}";
  private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
  private static final String ARRAY_ELEMENT_SEPARATOR = ", ";

  private static final String NON_EMPTY_ARRAY = ARRAY_START + "..." + ARRAY_END;
  private static final String EMPTY_COLLECTION = "[]";
  private static final String NON_EMPTY_COLLECTION = "[...]";

  /**
   * Return whether the given throwable is a checked exception:
   * that is, neither a RuntimeException nor an Error.
   *
   * @param ex the throwable to check
   * @return whether the throwable is a checked exception
   * @see java.lang.Exception
   * @see java.lang.RuntimeException
   * @see java.lang.Error
   * @since 4.0
   */
  public static boolean isCheckedException(Throwable ex) {
    return !(ex instanceof RuntimeException || ex instanceof Error);
  }

  /**
   * Check whether the given exception is compatible with the specified
   * exception types, as declared in a throws clause.
   *
   * @param ex the exception to check
   * @param declaredExceptions the exception types declared in the throws clause
   * @return whether the given exception is compatible
   * @since 4.0
   */
  public static boolean isCompatibleWithThrowsClause(Throwable ex, @Nullable Class<?>... declaredExceptions) {
    if (!isCheckedException(ex)) {
      return true;
    }
    if (declaredExceptions != null) {
      for (Class<?> declaredException : declaredExceptions) {
        if (declaredException.isInstance(ex)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Determine whether the given object is an array:
   * either an Object array or a primitive array.
   *
   * @param obj the object to check
   * @since 3.0
   */
  public static boolean isArray(@Nullable Object obj) {
    return obj != null && obj.getClass().isArray();
  }

  /**
   * Determine whether the given array is empty:
   * i.e. {@code null} or of zero length.
   *
   * @param array the array to check
   * @see #isEmpty(Object)
   */
  public static boolean isEmpty(@Nullable Object[] array) {
    return array == null || array.length == 0;
  }

  /**
   * Determine whether the given array is not empty:
   * i.e. {@code null} or of zero length.
   *
   * @param array the array to check
   * @see #isEmpty(Object[])
   */
  public static boolean isNotEmpty(@Nullable Object[] array) {
    return array != null && array.length != 0;
  }

  /**
   * Determine whether the given object is empty.
   * <p>This method supports the following object types.
   * <ul>
   * <li>{@code Optional}: considered empty if not {@link Optional#isPresent()}</li>
   * <li>{@code Array}: considered empty if its length is zero</li>
   * <li>{@link CharSequence}: considered empty if its length is zero</li>
   * <li>{@link Collection}: delegates to {@link Collection#isEmpty()}</li>
   * <li>{@link Map}: delegates to {@link Map#isEmpty()}</li>
   * </ul>
   * <p>If the given object is non-null and not one of the aforementioned
   * supported types, this method returns {@code false}.
   *
   * @param obj the object to check
   * @return {@code true} if the object is {@code null} or <em>empty</em>
   * @see Optional#isPresent()
   * @see ObjectUtils#isEmpty(Object[])
   * @see StringUtils#isEmpty(CharSequence)
   * @see CollectionUtils#isEmpty(java.util.Collection)
   * @see CollectionUtils#isEmpty(java.util.Map)
   */
  @Contract("null -> true")
  public static boolean isEmpty(@Nullable Object obj) {
    if (obj == null) {
      return true;
    }
    if (obj instanceof Optional) {
      return ((Optional<?>) obj).isEmpty();
    }
    if (obj instanceof CharSequence) {
      return ((CharSequence) obj).isEmpty();
    }
    if (obj instanceof Collection) {
      return ((Collection<?>) obj).isEmpty();
    }
    if (obj instanceof Map) {
      return ((Map<?, ?>) obj).isEmpty();
    }
    return obj.getClass().isArray() && Array.getLength(obj) == 0;
  }

  public static boolean isNotEmpty(@Nullable Object obj) {
    return !isEmpty(obj);
  }

  /**
   * Unwrap the given object which is potentially a {@link java.util.Optional}.
   *
   * @param obj the candidate object
   * @return either the value held within the {@code Optional}, {@code null}
   * if the {@code Optional} is empty, or simply the given object as-is
   * @since 3.0
   */
  @Nullable
  public static Object unwrapOptional(@Nullable Object obj) {
    if (obj instanceof Optional<?> optional) {
      if (optional.isEmpty()) {
        return null;
      }
      Object result = optional.get();
      Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
      return result;
    }
    return obj;
  }
  //

  /**
   * To array object
   *
   * @param source String array
   * @param targetClass Target class
   * @return An array object
   * @throws ConversionException If can't convert source to target type object
   */
  public static Object toArrayObject(String[] source, Class<?> targetClass) {

    // @since 2.1.6 fix: String[].class can't be resolve
    if (String[].class == targetClass) {
      return source;
    }
    int length = source.length;
    if (int[].class == targetClass) {
      int[] newInstance = new int[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Integer.parseInt(source[j]);
      return newInstance;
    }
    else if (Integer[].class == targetClass) {
      Integer[] newInstance = new Integer[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Integer.valueOf(source[j]);
      return newInstance;
    }
    else if (long[].class == targetClass) {
      long[] newInstance = new long[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Long.parseLong(source[j]);
      return newInstance;
    }
    else if (Long[].class == targetClass) {
      Long[] newInstance = new Long[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Long.valueOf(source[j]);
      return newInstance;
    }
    else if (short[].class == targetClass) {
      short[] newInstance = new short[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Short.parseShort(source[j]);
      return newInstance;
    }
    else if (Short[].class == targetClass) {
      Short[] newInstance = new Short[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Short.valueOf(source[j]);
      return newInstance;
    }
    else if (byte[].class == targetClass) {
      byte[] newInstance = new byte[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Byte.parseByte(source[j]);
      return newInstance;
    }
    else if (Byte[].class == targetClass) {
      Byte[] newInstance = new Byte[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Byte.valueOf(source[j]);
      return newInstance;
    }
    else if (float[].class == targetClass) {
      float[] newInstance = new float[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Float.parseFloat(source[j]);
      return newInstance;
    }
    else if (Float[].class == targetClass) {
      Float[] newInstance = new Float[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Float.valueOf(source[j]);
      return newInstance;
    }
    else if (double[].class == targetClass) {
      double[] newInstance = new double[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Double.parseDouble(source[j]);
      return newInstance;
    }
    else if (Double[].class == targetClass) {
      Double[] newInstance = new Double[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Double.valueOf(source[j]);
      return newInstance;
    }
    // fix @since 2.1.6
    if (targetClass.isArray()) {
      targetClass = targetClass.getComponentType();
    }
    Object newInstance = Array.newInstance(targetClass, length);
    DefaultConversionService conversionService = DefaultConversionService.getSharedInstance();
    for (short i = 0; i < length; i++) {
      Array.set(newInstance, i, conversionService.convert(source[i], targetClass));
    }
    return newInstance;
  }

  public static <T> T parseArray(String[] source, Class<T> targetClass) {
    return targetClass.cast(toArrayObject(source, targetClass));
  }

  /**
   * Check whether the given array contains the given element.
   *
   * @param array the array to check (may be {@code null},
   * in which case the return value will always be {@code false})
   * @param element the element to check for
   * @return whether the element has been found in the given array
   * @since 3.0
   */
  public static boolean containsElement(@Nullable Object[] array, Object element) {
    if (array != null) {
      for (Object candidate : array) {
        if (ObjectUtils.nullSafeEquals(candidate, element)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check whether the given array of enum constants contains a constant with the given name,
   * ignoring case when determining a match.
   *
   * @param enumValues the enum values to check, typically obtained via {@code MyEnum.values()}
   * @param constant the constant name to find (must not be null or empty string)
   * @return whether the constant has been found in the given array
   * @since 4.0
   */
  public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
    return containsConstant(enumValues, constant, false);
  }

  /**
   * Check whether the given array of enum constants contains a constant with the given name.
   *
   * @param enumValues the enum values to check, typically obtained via {@code MyEnum.values()}
   * @param constant the constant name to find (must not be null or empty string)
   * @param caseSensitive whether case is significant in determining a match
   * @return whether the constant has been found in the given array
   * @since 4.0
   */
  public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
    for (Enum<?> candidate : enumValues) {
      if (caseSensitive
              ? candidate.toString().equals(constant)
              : candidate.toString().equalsIgnoreCase(constant)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Case insensitive alternative to {@link Enum#valueOf(Class, String)}.
   *
   * @param <E> the concrete Enum type
   * @param enumValues the array of all Enum constants in question, usually per {@code Enum.values()}
   * @param constant the constant to get the enum value of
   * @throws IllegalArgumentException if the given constant is not found in the given array
   * of enum values. Use {@link #containsConstant(Enum[], String)} as a guard to avoid this exception.
   */
  public static <E extends Enum<?>> E caseInsensitiveValueOf(E[] enumValues, String constant) {
    for (E candidate : enumValues) {
      if (candidate.toString().equalsIgnoreCase(constant)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException(
            "Constant [%s] does not exist in enum type %s".formatted(constant, enumValues.getClass().getComponentType().getName()));
  }

  /**
   * Append the given object to the given array, returning a new array
   * consisting of the input array contents plus the given object.
   *
   * @param array the array to append to (can be {@code null})
   * @param obj the object to append
   * @return the new array (of the same component type; never {@code null})
   * @since 3.0
   */
  public static <A, O extends A> A[] addObjectToArray(@Nullable A[] array, @Nullable O obj) {
    return addObjectToArray(array, obj, (array != null ? array.length : 0));
  }

  /**
   * Add the given object to the given array at the specified position, returning
   * a new array consisting of the input array contents plus the given object.
   *
   * @param array the array to add to (can be {@code null})
   * @param obj the object to append
   * @param position the position at which to add the object
   * @return the new array (of the same component type; never {@code null})
   * @since 4.0
   */
  public static <A, O extends A> A[] addObjectToArray(@Nullable A[] array, @Nullable O obj, int position) {
    Class<?> componentType = Object.class;
    if (array != null) {
      componentType = array.getClass().getComponentType();
    }
    else if (obj != null) {
      componentType = obj.getClass();
    }
    int newArrayLength = (array != null ? array.length + 1 : 1);
    @SuppressWarnings("unchecked")
    A[] newArray = (A[]) Array.newInstance(componentType, newArrayLength);
    if (array != null) {
      System.arraycopy(array, 0, newArray, 0, position);
      System.arraycopy(array, position, newArray, position + 1, array.length - position);
    }
    newArray[position] = obj;
    return newArray;
  }

  /**
   * Convert the given array (which may be a primitive array) to an
   * object array (if necessary of primitive wrapper objects).
   * <p>A {@code null} source value will be converted to an
   * empty Object array.
   *
   * @param source the (potentially primitive) array
   * @return the corresponding object array (never {@code null})
   * @throws IllegalArgumentException if the parameter is not an array
   * @since 3.0
   */
  public static Object[] toObjectArray(@Nullable Object source) {
    if (source instanceof Object[]) {
      return (Object[]) source;
    }
    if (source == null) {
      return Constant.EMPTY_OBJECTS;
    }
    if (!source.getClass().isArray()) {
      throw new IllegalArgumentException("Source is not an array: " + source);
    }
    int length = Array.getLength(source);
    if (length == 0) {
      return Constant.EMPTY_OBJECTS;
    }
    Class<?> wrapperType = Array.get(source, 0).getClass();
    Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
    for (int i = 0; i < length; i++) {
      newArray[i] = Array.get(source, i);
    }
    return newArray;
  }

  //---------------------------------------------------------------------
  // Convenience methods for content-based equality/hash-code handling
  //---------------------------------------------------------------------

  /**
   * Determine if the given objects are equal, returning {@code true} if
   * both are {@code null} or {@code false} if only one is {@code null}.
   * <p>Compares arrays with {@code Arrays.equals}, performing an equality
   * check based on the array elements rather than the array reference.
   *
   * @param o1 first Object to compare
   * @param o2 second Object to compare
   * @return whether the given objects are equal
   * @see Object#equals(Object)
   * @see java.util.Arrays#equals
   */
  public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
    if (o1 == o2) {
      return true;
    }
    if (o1 == null || o2 == null) {
      return false;
    }
    if (o1.equals(o2)) {
      return true;
    }
    return o1.getClass().isArray()
            && o2.getClass().isArray()
            && arrayEquals(o1, o2);
  }

  /**
   * Compare the given arrays with {@code Arrays.equals}, performing an equality
   * check based on the array elements rather than the array reference.
   *
   * @param o1 first array to compare
   * @param o2 second array to compare
   * @return whether the given objects are equal
   * @see #nullSafeEquals(Object, Object)
   * @see java.util.Arrays#equals
   */
  private static boolean arrayEquals(Object o1, Object o2) {
    if (o1 instanceof Object[] && o2 instanceof Object[]) {
      return Arrays.equals((Object[]) o1, (Object[]) o2);
    }
    if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
      return Arrays.equals((boolean[]) o1, (boolean[]) o2);
    }
    if (o1 instanceof byte[] && o2 instanceof byte[]) {
      return Arrays.equals((byte[]) o1, (byte[]) o2);
    }
    if (o1 instanceof char[] && o2 instanceof char[]) {
      return Arrays.equals((char[]) o1, (char[]) o2);
    }
    if (o1 instanceof double[] && o2 instanceof double[]) {
      return Arrays.equals((double[]) o1, (double[]) o2);
    }
    if (o1 instanceof float[] && o2 instanceof float[]) {
      return Arrays.equals((float[]) o1, (float[]) o2);
    }
    if (o1 instanceof int[] && o2 instanceof int[]) {
      return Arrays.equals((int[]) o1, (int[]) o2);
    }
    if (o1 instanceof long[] && o2 instanceof long[]) {
      return Arrays.equals((long[]) o1, (long[]) o2);
    }
    if (o1 instanceof short[] && o2 instanceof short[]) {
      return Arrays.equals((short[]) o1, (short[]) o2);
    }
    return false;
  }

  /**
   * Return a hash code for the given elements, delegating to
   * {@link #nullSafeHashCode(Object)} for each element. Contrary
   * to {@link Objects#hash(Object...)}, this method can handle an
   * element that is an array.
   *
   * @param elements the elements to be hashed
   * @return a hash value of the elements
   * @since 4.0
   */
  public static int nullSafeHash(@Nullable Object... elements) {
    if (elements == null) {
      return 0;
    }
    int result = 1;
    for (Object element : elements) {
      result = 31 * result + nullSafeHashCode(element);
    }
    return result;
  }

  /**
   * Return a hash code for the given object; typically the value of
   * {@code Object#hashCode()}}. If the object is an array,
   * this method will delegate to any of the {@code Arrays.hashCode}
   * methods. If the object is {@code null}, this method returns 0.
   *
   * @see Object#hashCode()
   * @see Arrays
   */
  public static int nullSafeHashCode(@Nullable Object obj) {
    if (obj == null) {
      return 0;
    }
    if (obj.getClass().isArray()) {
      if (obj instanceof Object[] objects) {
        return Arrays.hashCode(objects);
      }
      if (obj instanceof boolean[] booleans) {
        return Arrays.hashCode(booleans);
      }
      if (obj instanceof byte[] bytes) {
        return Arrays.hashCode(bytes);
      }
      if (obj instanceof char[] chars) {
        return Arrays.hashCode(chars);
      }
      if (obj instanceof double[] doubles) {
        return Arrays.hashCode(doubles);
      }
      if (obj instanceof float[] floats) {
        return Arrays.hashCode(floats);
      }
      if (obj instanceof int[] ints) {
        return Arrays.hashCode(ints);
      }
      if (obj instanceof long[] longs) {
        return Arrays.hashCode(longs);
      }
      if (obj instanceof short[] shorts) {
        return Arrays.hashCode(shorts);
      }
    }
    return obj.hashCode();
  }

  //---------------------------------------------------------------------
  // Convenience methods for toString output
  //---------------------------------------------------------------------

  public static String toHexString(@Nullable Object obj) {
    return obj == null
            ? NULL_STRING
            : new StringBuilder()
                    .append(obj.getClass().getName())
                    .append('@')
                    .append(Integer.toHexString(obj.hashCode())).toString();
  }

  /**
   * Return a String representation of an object's overall identity.
   *
   * @param obj the object (may be {@code null})
   * @return the object's identity as String representation,
   * or an empty String if the object was {@code null}
   */
  public static String identityToString(@Nullable Object obj) {
    if (obj == null) {
      return Constant.BLANK;
    }
    return obj.getClass().getName() + "@" + getIdentityHexString(obj);
  }

  /**
   * Return a hex String form of an object's identity hash code.
   *
   * @param obj the object
   * @return the object's identity code in hex notation
   */
  public static String getIdentityHexString(Object obj) {
    return Integer.toHexString(System.identityHashCode(obj));
  }

  /**
   * Return a content-based String representation if {@code obj} is
   * not {@code null}; otherwise returns an empty String.
   * <p>Differs from {@link #nullSafeToString(Object)} in that it returns
   * an empty String rather than "null" for a {@code null} value.
   *
   * @param obj the object to build a display String for
   * @return a display String representation of {@code obj}
   * @see #nullSafeToString(Object)
   */
  public static String getDisplayString(@Nullable Object obj) {
    if (obj == null) {
      return Constant.BLANK;
    }
    return nullSafeToString(obj);
  }

  /**
   * Determine the class name for the given object.
   * <p>Returns a {@code "null"} String if {@code obj} is {@code null}.
   *
   * @param obj the object to introspect (may be {@code null})
   * @return the corresponding class name
   */
  public static String nullSafeClassName(@Nullable Object obj) {
    return (obj != null ? obj.getClass().getName() : NULL_STRING);
  }

  /**
   * Return a String representation of the specified Object.
   * <p>Builds a String representation of the contents in case of an array.
   * Returns a {@code "null"} String if {@code obj} is {@code null}.
   *
   * @param obj the object to build a String representation for
   * @return a String representation of {@code obj}
   */
  public static String nullSafeToString(@Nullable Object obj) {
    if (obj == null) {
      return NULL_STRING;
    }
    if (obj instanceof String) {
      return (String) obj;
    }
    if (obj instanceof Object[]) {
      return nullSafeToString((Object[]) obj);
    }
    if (obj instanceof boolean[]) {
      return nullSafeToString((boolean[]) obj);
    }
    if (obj instanceof byte[]) {
      return nullSafeToString((byte[]) obj);
    }
    if (obj instanceof char[]) {
      return nullSafeToString((char[]) obj);
    }
    if (obj instanceof double[]) {
      return nullSafeToString((double[]) obj);
    }
    if (obj instanceof float[]) {
      return nullSafeToString((float[]) obj);
    }
    if (obj instanceof int[]) {
      return nullSafeToString((int[]) obj);
    }
    if (obj instanceof long[]) {
      return nullSafeToString((long[]) obj);
    }
    if (obj instanceof short[]) {
      return nullSafeToString((short[]) obj);
    }
    String str = obj.toString();
    return (str != null ? str : Constant.BLANK);
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable Object[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (Object o : array) {
      stringJoiner.add(String.valueOf(o));
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable boolean[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (boolean b : array) {
      stringJoiner.add(String.valueOf(b));
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable byte[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (byte b : array) {
      stringJoiner.add(String.valueOf(b));
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable char[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (char c : array) {
      stringJoiner.add('\'' + String.valueOf(c) + '\'');
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable double[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (double d : array) {
      stringJoiner.add(String.valueOf(d));
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable float[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (float f : array) {
      stringJoiner.add(String.valueOf(f));
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable int[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (int i : array) {
      stringJoiner.add(String.valueOf(i));
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable long[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (long l : array) {
      stringJoiner.add(String.valueOf(l));
    }
    return stringJoiner.toString();
  }

  /**
   * Return a String representation of the contents of the specified array.
   * <p>The String representation consists of a list of the array's elements,
   * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
   * by the characters {@code ", "} (a comma followed by a space).
   * Returns a {@code "null"} String if {@code array} is {@code null}.
   *
   * @param array the array to build a String representation for
   * @return a String representation of {@code array}
   */
  public static String nullSafeToString(@Nullable short[] array) {
    if (array == null) {
      return NULL_STRING;
    }
    int length = array.length;
    if (length == 0) {
      return EMPTY_ARRAY;
    }
    StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
    for (short s : array) {
      stringJoiner.add(String.valueOf(s));
    }
    return stringJoiner.toString();
  }

  /**
   * Generate a null-safe, concise string representation of the supplied object
   * as described below.
   * <p>Favor this method over {@link #nullSafeToString(Object)} when you need
   * the length of the generated string to be limited.
   * <p>Returns:
   * <ul>
   * <li>{@code "null"} if {@code obj} is {@code null}</li>
   * <li>{@code"Optional.empty"} if {@code obj} is an empty {@link Optional}</li>
   * <li>{@code"Optional[<concise-string>]"} if {@code obj} is a non-empty {@code Optional},
   * where {@code <concise-string>} is the result of invoking {@link #nullSafeConciseToString}
   * on the object contained in the {@code Optional}</li>
   * <li>{@code "{}"} if {@code obj} is an empty array or {@link Map}</li>
   * <li>{@code "{...}"} if {@code obj} is a non-empty array or {@link Map}</li>
   * <li>{@code "[]"} if {@code obj} is an empty {@link Collection}</li>
   * <li>{@code "[...]"} if {@code obj} is a non-empty {@link Collection}</li>
   * <li>{@linkplain Class#getName() Class name} if {@code obj} is a {@link Class}</li>
   * <li>{@linkplain Charset#name() Charset name} if {@code obj} is a {@link Charset}</li>
   * <li>{@linkplain TimeZone#getID() TimeZone ID} if {@code obj} is a {@link TimeZone}</li>
   * <li>{@linkplain ZoneId#getId() Zone ID} if {@code obj} is a {@link ZoneId}</li>
   * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string}
   * if {@code obj} is a {@link String} or {@link CharSequence}</li>
   * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string}
   * if {@code obj} is a <em>simple value type</em> whose {@code toString()} method
   * returns a non-null value</li>
   * <li>Otherwise, a string representation of the object's type name concatenated
   * with {@code "@"} and a hex string form of the object's identity hash code</li>
   * </ul>
   * <p>In the context of this method, a <em>simple value type</em> is any of the following:
   * primitive wrapper (excluding {@link Void}), {@link Enum}, {@link Number},
   * {@link java.util.Date Date}, {@link java.time.temporal.Temporal Temporal},
   * {@link java.io.File File}, {@link java.nio.file.Path Path},
   * {@link java.net.URI URI}, {@link java.net.URL URL},
   * {@link java.net.InetAddress InetAddress}, {@link java.util.Currency Currency},
   * {@link java.util.Locale Locale}, {@link java.util.UUID UUID},
   * {@link java.util.regex.Pattern Pattern}.
   *
   * @param obj the object to build a string representation for
   * @return a concise string representation of the supplied object
   * @see #nullSafeToString(Object)
   * @see StringUtils#truncate(CharSequence)
   * @see ClassUtils#isSimpleValueType(Class)
   * @since 4.0
   */
  public static String nullSafeConciseToString(@Nullable Object obj) {
    if (obj == null) {
      return NULL_STRING;
    }
    if (obj instanceof Optional<?> optional) {
      return (optional.isEmpty() ? "Optional.empty" :
              "Optional[%s]".formatted(nullSafeConciseToString(optional.get())));
    }
    if (obj.getClass().isArray()) {
      return (Array.getLength(obj) == 0 ? EMPTY_ARRAY : NON_EMPTY_ARRAY);
    }
    if (obj instanceof Collection<?> collection) {
      return (collection.isEmpty() ? EMPTY_COLLECTION : NON_EMPTY_COLLECTION);
    }
    if (obj instanceof Map<?, ?> map) {
      // EMPTY_ARRAY and NON_EMPTY_ARRAY are also used for maps.
      return (map.isEmpty() ? EMPTY_ARRAY : NON_EMPTY_ARRAY);
    }
    if (obj instanceof Class<?> clazz) {
      return clazz.getName();
    }
    if (obj instanceof Charset charset) {
      return charset.name();
    }
    if (obj instanceof TimeZone timeZone) {
      return timeZone.getID();
    }
    if (obj instanceof ZoneId zoneId) {
      return zoneId.getId();
    }
    if (obj instanceof CharSequence charSequence) {
      return StringUtils.truncate(charSequence);
    }
    Class<?> type = obj.getClass();
    if (ClassUtils.isSimpleValueType(type)) {
      String str = obj.toString();
      if (str != null) {
        return StringUtils.truncate(str);
      }
    }
    return type.getTypeName() + "@" + getIdentityHexString(obj);
  }

  /**
   * Return a String from none-null object's {@code toString}.
   *
   * @param obj the object to build a String representation for
   * @return a String representation of {@code obj}
   */
  @Nullable
  public static String toString(@Nullable Object obj) {
    return obj == null ? null : obj.toString();
  }

  /**
   * get object class name
   *
   * @param obj target
   * @return class name
   * @throws NullPointerException if obj is null
   */
  public static String getClassName(Object obj) {
    return obj.getClass().getName();
  }

}

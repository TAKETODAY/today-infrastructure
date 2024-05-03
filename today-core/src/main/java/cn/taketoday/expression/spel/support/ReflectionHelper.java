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

package cn.taketoday.expression.spel.support;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ReflectiveMethodInvoker;

/**
 * Utility methods used by the reflection resolver code to discover the appropriate
 * methods/constructors and fields that should be used in expressions.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ReflectionHelper {

  /**
   * Cache for equivalent methods in a public declaring class in the type
   * hierarchy of the method's declaring class.
   */
  private static final ConcurrentReferenceHashMap<Method, Class<?>>
          publicDeclaringClassCache = new ConcurrentReferenceHashMap<>(256);

  /**
   * Compare argument arrays and return information about whether they match.
   * A supplied type converter and conversionAllowed flag allow for matches to take
   * into account that a type may be transformed into a different type by the converter.
   *
   * @param expectedArgTypes the types the method/constructor is expecting
   * @param suppliedArgTypes the types that are being supplied at the point of invocation
   * @param typeConverter a registered type converter
   * @return a MatchInfo object indicating what kind of match it was,
   * or {@code null} if it was not a match
   */
  @Nullable
  static ArgumentsMatchInfo compareArguments(List<TypeDescriptor> expectedArgTypes,
          List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

    Assert.isTrue(expectedArgTypes.size() == suppliedArgTypes.size(),
            "Expected argument types and supplied argument types should be arrays of same length");

    ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;
    for (int i = 0; i < expectedArgTypes.size() && match != null; i++) {
      TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
      TypeDescriptor expectedArg = expectedArgTypes.get(i);
      // The user may supply null - and that will be ok unless a primitive is expected
      if (suppliedArg == null) {
        if (expectedArg.isPrimitive()) {
          match = null;
        }
      }
      else if (!expectedArg.equals(suppliedArg)) {
        if (suppliedArg.isAssignableTo(expectedArg)) {
          if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
            match = ArgumentsMatchKind.CLOSE;
          }
        }
        else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
          match = ArgumentsMatchKind.REQUIRES_CONVERSION;
        }
        else {
          match = null;
        }
      }
    }
    return (match != null ? new ArgumentsMatchInfo(match) : null);
  }

  /**
   * Based on {@link ReflectiveMethodInvoker#getTypeDifferenceWeight(Class[], Object[])} but operates on TypeDescriptors.
   */
  public static int getTypeDifferenceWeight(List<TypeDescriptor> paramTypes, List<TypeDescriptor> argTypes) {
    int result = 0;
    int size = paramTypes.size();
    for (int i = 0; i < size; i++) {
      TypeDescriptor paramType = paramTypes.get(i);
      TypeDescriptor argType = (i < argTypes.size() ? argTypes.get(i) : null);
      if (argType == null) {
        if (paramType.isPrimitive()) {
          return Integer.MAX_VALUE;
        }
      }
      else {
        Class<?> paramTypeClazz = paramType.getType();
        if (!ClassUtils.isAssignable(paramTypeClazz, argType.getType())) {
          return Integer.MAX_VALUE;
        }
        if (paramTypeClazz.isPrimitive()) {
          paramTypeClazz = Object.class;
        }
        Class<?> superClass = argType.getType().getSuperclass();
        while (superClass != null) {
          if (paramTypeClazz.equals(superClass)) {
            result = result + 2;
            superClass = null;
          }
          else if (ClassUtils.isAssignable(paramTypeClazz, superClass)) {
            result = result + 2;
            superClass = superClass.getSuperclass();
          }
          else {
            superClass = null;
          }
        }
        if (paramTypeClazz.isInterface()) {
          result = result + 1;
        }
      }
    }
    return result;
  }

  /**
   * Compare argument arrays and return information about whether they match.
   * A supplied type converter and conversionAllowed flag allow for matches to
   * take into account that a type may be transformed into a different type by the
   * converter. This variant of compareArguments also allows for a varargs match.
   *
   * @param expectedArgTypes the types the method/constructor is expecting
   * @param suppliedArgTypes the types that are being supplied at the point of invocation
   * @param typeConverter a registered type converter
   * @return a MatchInfo object indicating what kind of match it was,
   * or {@code null} if it was not a match
   */
  @Nullable
  static ArgumentsMatchInfo compareArgumentsVarargs(List<TypeDescriptor> expectedArgTypes,
          List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

    Assert.isTrue(CollectionUtils.isNotEmpty(expectedArgTypes),
            "Expected arguments must at least include one array (the varargs parameter)");
    Assert.isTrue(expectedArgTypes.get(expectedArgTypes.size() - 1).isArray(),
            "Final expected argument should be array type (the varargs parameter)");

    ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;

    // Check up until the varargs argument:

    // Deal with the arguments up to 'expected number' - 1 (that is everything but the varargs argument)
    int argCountUpToVarargs = expectedArgTypes.size() - 1;
    for (int i = 0; i < argCountUpToVarargs && match != null; i++) {
      TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
      TypeDescriptor expectedArg = expectedArgTypes.get(i);
      if (suppliedArg == null) {
        if (expectedArg.isPrimitive()) {
          match = null;
        }
      }
      else {
        if (!expectedArg.equals(suppliedArg)) {
          if (suppliedArg.isAssignableTo(expectedArg)) {
            if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
              match = ArgumentsMatchKind.CLOSE;
            }
          }
          else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
            match = ArgumentsMatchKind.REQUIRES_CONVERSION;
          }
          else {
            match = null;
          }
        }
      }
    }

    // If already confirmed it cannot be a match, then return
    if (match == null) {
      return null;
    }

    if (suppliedArgTypes.size() == expectedArgTypes.size()
            && expectedArgTypes.get(expectedArgTypes.size() - 1).equals(suppliedArgTypes.get(suppliedArgTypes.size() - 1))) {
      // Special case: there is one parameter left and it is an array and it matches the varargs
      // expected argument - that is a match, the caller has already built the array. Proceed with it.
    }
    else {
      // Now... we have the final argument in the method we are checking as a match and we have 0
      // or more other arguments left to pass to it.
      TypeDescriptor varargsDesc = expectedArgTypes.get(expectedArgTypes.size() - 1);
      TypeDescriptor elementDesc = varargsDesc.getElementDescriptor();
      Assert.state(elementDesc != null, "No element type");
      Class<?> varargsParamType = elementDesc.getType();

      // All remaining parameters must be of this type or convertible to this type
      for (int i = expectedArgTypes.size() - 1; i < suppliedArgTypes.size(); i++) {
        TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
        if (suppliedArg == null) {
          if (varargsParamType.isPrimitive()) {
            match = null;
          }
        }
        else {
          if (varargsParamType != suppliedArg.getType()) {
            if (ClassUtils.isAssignable(varargsParamType, suppliedArg.getType())) {
              if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
                match = ArgumentsMatchKind.CLOSE;
              }
            }
            else if (typeConverter.canConvert(suppliedArg, TypeDescriptor.valueOf(varargsParamType))) {
              match = ArgumentsMatchKind.REQUIRES_CONVERSION;
            }
            else {
              match = null;
            }
          }
        }
      }
    }

    return (match != null ? new ArgumentsMatchInfo(match) : null);
  }

  // TODO could do with more refactoring around argument handling and varargs

  /**
   * Convert a supplied set of arguments into the requested types. If the parameterTypes are related to
   * a varargs method then the final entry in the parameterTypes array is going to be an array itself whose
   * component type should be used as the conversion target for extraneous arguments. (For example, if the
   * parameterTypes are {Integer, String[]} and the input arguments are {Integer, boolean, float} then both
   * the boolean and float must be converted to strings). This method does *not* repackage the arguments
   * into a form suitable for the varargs invocation - a subsequent call to setupArgumentsForVarargsInvocation handles that.
   *
   * @param converter the converter to use for type conversions
   * @param arguments the arguments to convert to the requested parameter types
   * @param method the target Method
   * @return true if some kind of conversion occurred on the argument
   * @throws SpelEvaluationException if there is a problem with conversion
   */
  public static boolean convertAllArguments(TypeConverter converter, Object[] arguments, Method method)
          throws SpelEvaluationException {

    Integer varargsPosition = (method.isVarArgs() ? method.getParameterCount() - 1 : null);
    return convertArguments(converter, arguments, method, varargsPosition);
  }

  /**
   * Takes an input set of argument values and converts them to the types specified as the
   * required parameter types. The arguments are converted 'in-place' in the input array.
   *
   * @param converter the type converter to use for attempting conversions
   * @param arguments the actual arguments that need conversion
   * @param executable the target Method or Constructor
   * @param varargsPosition the known position of the varargs argument, if any
   * ({@code null} if not varargs)
   * @return {@code true} if some kind of conversion occurred on an argument
   * @throws EvaluationException if a problem occurs during conversion
   */
  static boolean convertArguments(TypeConverter converter, Object[] arguments,
          Executable executable, @Nullable Integer varargsPosition) throws EvaluationException {

    boolean conversionOccurred = false;
    if (varargsPosition == null) {
      for (int i = 0; i < arguments.length; i++) {
        TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forExecutable(executable, i));
        Object argument = arguments[i];
        arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
        conversionOccurred |= (argument != arguments[i]);
      }
    }
    else {
      // Convert everything up to the varargs position
      for (int i = 0; i < varargsPosition; i++) {
        TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forExecutable(executable, i));
        Object argument = arguments[i];
        arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
        conversionOccurred |= (argument != arguments[i]);
      }

      MethodParameter methodParam = MethodParameter.forExecutable(executable, varargsPosition);

      // If the target is varargs and there is just one more argument, then convert it here.
      if (varargsPosition == arguments.length - 1) {
        Object argument = arguments[varargsPosition];
        TypeDescriptor targetType = new TypeDescriptor(methodParam);
        TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
        if (argument == null) {
          // Perform the equivalent of GenericConversionService.convertNullSource() for a single argument.
          if (targetType.getElementDescriptor().getObjectType() == Optional.class) {
            arguments[varargsPosition] = Optional.empty();
            conversionOccurred = true;
          }
        }
        // If the argument type is equal to the varargs element type, there is no need to
        // convert it or wrap it in an array. For example, using StringToArrayConverter to
        // convert a String containing a comma would result in the String being split and
        // repackaged in an array when it should be used as-is.
        else if (!sourceType.equals(targetType.getElementDescriptor())) {
          arguments[varargsPosition] = converter.convertValue(argument, sourceType, targetType);
        }
        // Possible outcomes of the above if-else block:
        // 1) the input argument was null, and nothing was done.
        // 2) the input argument was null; the varargs element type is Optional; and the argument was converted to Optional.empty().
        // 3) the input argument was correct type but not wrapped in an array, and nothing was done.
        // 4) the input argument was already compatible (i.e., array of valid type), and nothing was done.
        // 5) the input argument was the wrong type and got converted and wrapped in an array.
        if (argument != arguments[varargsPosition] &&
                !isFirstEntryInArray(argument, arguments[varargsPosition])) {
          conversionOccurred = true; // case 5
        }
      }
      // Otherwise, convert remaining arguments to the varargs element type.
      else {
        TypeDescriptor targetType = new TypeDescriptor(methodParam).getElementDescriptor();
        Assert.state(targetType != null, "No element type");
        for (int i = varargsPosition; i < arguments.length; i++) {
          Object argument = arguments[i];
          arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
          conversionOccurred |= (argument != arguments[i]);
        }
      }
    }
    return conversionOccurred;
  }

  /**
   * Takes an input set of argument values and converts them to the types specified as the
   * required parameter types. The arguments are converted 'in-place' in the input array.
   *
   * @param converter the type converter to use for attempting conversions
   * @param arguments the actual arguments that need conversion
   * @param methodHandle the target MethodHandle
   * @param varargsPosition the known position of the varargs argument, if any
   * ({@code null} if not varargs)
   * @return {@code true} if some kind of conversion occurred on an argument
   * @throws EvaluationException if a problem occurs during conversion
   */
  public static boolean convertAllMethodHandleArguments(TypeConverter converter,
          Object[] arguments, MethodHandle methodHandle, @Nullable Integer varargsPosition) throws EvaluationException {

    boolean conversionOccurred = false;
    final MethodType methodHandleArgumentTypes = methodHandle.type();
    if (varargsPosition == null) {
      for (int i = 0; i < arguments.length; i++) {
        Class<?> argumentClass = methodHandleArgumentTypes.parameterType(i);
        ResolvableType resolvableType = ResolvableType.forClass(argumentClass);
        TypeDescriptor targetType = new TypeDescriptor(resolvableType, argumentClass, (Annotation[]) null);

        Object argument = arguments[i];
        arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
        conversionOccurred |= (argument != arguments[i]);
      }
    }
    else {
      // Convert everything up to the varargs position
      for (int i = 0; i < varargsPosition; i++) {
        Class<?> argumentClass = methodHandleArgumentTypes.parameterType(i);
        ResolvableType resolvableType = ResolvableType.forClass(argumentClass);
        TypeDescriptor targetType = new TypeDescriptor(resolvableType, argumentClass, (Annotation[]) null);

        Object argument = arguments[i];
        arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
        conversionOccurred |= (argument != arguments[i]);
      }

      final Class<?> varArgClass = methodHandleArgumentTypes.lastParameterType().getComponentType();
      ResolvableType varArgResolvableType = ResolvableType.forClass(varArgClass);
      TypeDescriptor varArgContentType = new TypeDescriptor(varArgResolvableType, varArgClass, (Annotation[]) null);

      // If the target is varargs and there is just one more argument, then convert it here.
      if (varargsPosition == arguments.length - 1) {
        Object argument = arguments[varargsPosition];
        TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
        if (argument == null) {
          // Perform the equivalent of GenericConversionService.convertNullSource() for a single argument.
          if (varArgContentType.getElementDescriptor().getObjectType() == Optional.class) {
            arguments[varargsPosition] = Optional.empty();
            conversionOccurred = true;
          }
        }
        // If the argument type is equal to the varargs element type, there is no need to
        // convert it or wrap it in an array. For example, using StringToArrayConverter to
        // convert a String containing a comma would result in the String being split and
        // repackaged in an array when it should be used as-is.
        else if (!sourceType.equals(varArgContentType.getElementDescriptor())) {
          arguments[varargsPosition] = converter.convertValue(argument, sourceType, varArgContentType);
        }
        // Possible outcomes of the above if-else block:
        // 1) the input argument was null, and nothing was done.
        // 2) the input argument was null; the varargs element type is Optional; and the argument was converted to Optional.empty().
        // 3) the input argument was correct type but not wrapped in an array, and nothing was done.
        // 4) the input argument was already compatible (i.e., array of valid type), and nothing was done.
        // 5) the input argument was the wrong type and got converted and wrapped in an array.
        if (argument != arguments[varargsPosition] &&
                !isFirstEntryInArray(argument, arguments[varargsPosition])) {
          conversionOccurred = true; // case 5
        }
      }
      // Otherwise, convert remaining arguments to the varargs element type.
      else {
        Assert.state(varArgContentType != null, "No element type");
        for (int i = varargsPosition; i < arguments.length; i++) {
          Object argument = arguments[i];
          arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), varArgContentType);
          conversionOccurred |= (argument != arguments[i]);
        }
      }
    }
    return conversionOccurred;
  }

  /**
   * Check if the supplied value is the first entry in the array represented by the possibleArray value.
   *
   * @param value the value to check for in the array
   * @param possibleArray an array object that may have the supplied value as the first element
   * @return true if the supplied value is the first entry in the array
   */
  private static boolean isFirstEntryInArray(Object value, @Nullable Object possibleArray) {
    if (possibleArray == null) {
      return false;
    }
    Class<?> type = possibleArray.getClass();
    if (!type.isArray() || Array.getLength(possibleArray) == 0 ||
            !ClassUtils.isAssignableValue(type.getComponentType(), value)) {
      return false;
    }
    Object arrayValue = Array.get(possibleArray, 0);
    return (type.getComponentType().isPrimitive() ? arrayValue.equals(value) : arrayValue == value);
  }

  /**
   * Package up the arguments so that they correctly match what is expected in requiredParameterTypes.
   * <p>For example, if requiredParameterTypes is {@code (int, String[])} because the second parameter
   * was declared {@code String...}, then if arguments is {@code [1,"a","b"]} then it must be
   * repackaged as {@code [1,new String[]{"a","b"}]} in order to match the expected types.
   *
   * @param requiredParameterTypes the types of the parameters for the invocation
   * @param args the arguments to be setup ready for the invocation
   * @return a repackaged array of arguments where any varargs setup has been done
   */
  public static Object[] setupArgumentsForVarargsInvocation(Class<?>[] requiredParameterTypes, Object... args) {
    Assert.notEmpty(requiredParameterTypes, "Required parameter types array must not be empty");

    int parameterCount = requiredParameterTypes.length;
    Class<?> lastRequiredParameterType = requiredParameterTypes[parameterCount - 1];
    Assert.isTrue(lastRequiredParameterType.isArray(),
            "The last required parameter type must be an array to support varargs invocation");

    int argumentCount = args.length;
    Object lastArgument = (argumentCount > 0 ? args[argumentCount - 1] : null);

    // Check if repackaging is needed...
    if (parameterCount != argumentCount || !lastRequiredParameterType.isInstance(lastArgument)) {
      // Create an array for the leading arguments plus the varargs array argument.
      Object[] newArgs = new Object[parameterCount];
      // Copy all leading arguments to the new array, omitting the varargs array argument.
      System.arraycopy(args, 0, newArgs, 0, newArgs.length - 1);

      // Now sort out the final argument, which is the varargs one. Before entering this method,
      // the arguments should have been converted to the box form of the required type.
      int varargsArraySize = 0;  // zero size array if nothing to pass as the varargs parameter
      if (argumentCount >= parameterCount) {
        varargsArraySize = argumentCount - (parameterCount - 1);
      }
      Class<?> componentType = lastRequiredParameterType.componentType();
      Object varargsArray = Array.newInstance(componentType, varargsArraySize);
      for (int i = 0; i < varargsArraySize; i++) {
        Array.set(varargsArray, i, args[parameterCount - 1 + i]);
      }
      // Finally, add the varargs array to the new arguments array.
      newArgs[newArgs.length - 1] = varargsArray;
      return newArgs;
    }

    return args;
  }

  /**
   * Find the first public class or interface in the method's class hierarchy
   * that declares the supplied method.
   * <p>Sometimes the reflective method discovery logic finds a suitable method
   * that can easily be called via reflection but cannot be called from generated
   * code when compiling the expression because of visibility restrictions. For
   * example, if a non-public class overrides {@code toString()}, this method
   * will traverse up the type hierarchy to find the first public type that
   * declares the method (if there is one). For {@code toString()}, it may
   * traverse as far as {@link Object}.
   *
   * @param method the method to process
   * @return the public class or interface that declares the method, or
   * {@code null} if no such public type could be found
   */
  @Nullable
  public static Class<?> findPublicDeclaringClass(Method method) {
    return publicDeclaringClassCache.computeIfAbsent(method, key -> {
      // If the method is already defined in a public type, return that type.
      if (Modifier.isPublic(key.getDeclaringClass().getModifiers())) {
        return key.getDeclaringClass();
      }
      Method interfaceMethod = ReflectionUtils.getInterfaceMethodIfPossible(key, null);
      // If we found an interface method whose type is public, return the interface type.
      if (!interfaceMethod.equals(key)) {
        if (Modifier.isPublic(interfaceMethod.getDeclaringClass().getModifiers())) {
          return interfaceMethod.getDeclaringClass();
        }
      }
      // Attempt to search the type hierarchy.
      Class<?> superclass = key.getDeclaringClass().getSuperclass();
      if (superclass != null) {
        return findPublicDeclaringClass(superclass, key.getName(), key.getParameterTypes());
      }
      // Otherwise, no public declaring class found.
      return null;
    });
  }

  @Nullable
  private static Class<?> findPublicDeclaringClass(
          Class<?> declaringClass, String methodName, Class<?>[] parameterTypes) {

    if (Modifier.isPublic(declaringClass.getModifiers())) {
      try {
        declaringClass.getDeclaredMethod(methodName, parameterTypes);
        return declaringClass;
      }
      catch (NoSuchMethodException ex) {
        // Continue below...
      }
    }

    Class<?> superclass = declaringClass.getSuperclass();
    if (superclass != null) {
      return findPublicDeclaringClass(superclass, methodName, parameterTypes);
    }
    return null;
  }

  /**
   * Arguments match kinds.
   */
  enum ArgumentsMatchKind {

    /** An exact match is where the parameter types exactly match what the method/constructor is expecting. */
    EXACT,

    /** A close match is where the parameter types either exactly match or are assignment-compatible. */
    CLOSE,

    /** A conversion match is where the type converter must be used to transform some of the parameter types. */
    REQUIRES_CONVERSION
  }

  /**
   * An instance of ArgumentsMatchInfo describes what kind of match was achieved
   * between two sets of arguments - the set that a method/constructor is expecting
   * and the set that are being supplied at the point of invocation. If the kind
   * indicates that conversion is required for some of the arguments then the arguments
   * that require conversion are listed in the argsRequiringConversion array.
   */
  static class ArgumentsMatchInfo {

    private final ArgumentsMatchKind kind;

    ArgumentsMatchInfo(ArgumentsMatchKind kind) {
      this.kind = kind;
    }

    public boolean isExactMatch() {
      return (this.kind == ArgumentsMatchKind.EXACT);
    }

    public boolean isCloseMatch() {
      return (this.kind == ArgumentsMatchKind.CLOSE);
    }

    public boolean isMatchRequiringConversion() {
      return (this.kind == ArgumentsMatchKind.REQUIRES_CONVERSION);
    }

    @Override
    public String toString() {
      return "ArgumentMatchInfo: " + this.kind;
    }
  }

}

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.support;

import java.beans.PropertyEditor;
import java.lang.reflect.Method;

import cn.taketoday.beans.BeanWrapperImpl;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ReflectiveMethodInvoker;

/**
 * Subclass of {@link ReflectiveMethodInvoker} that tries to convert the given
 * arguments for the actual target method via a {@link TypeConverter}.
 *
 * <p>Supports flexible argument conversions, in particular for
 * invoking a specific overloaded method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanWrapperImpl#convertIfNecessary
 * @since 4.0 2022/2/18 11:12
 */
public class ArgumentConvertingMethodInvoker extends ReflectiveMethodInvoker {

  @Nullable
  private TypeConverter typeConverter;

  private boolean useDefaultConverter = true;

  /**
   * Set a TypeConverter to use for argument type conversion.
   * <p>Default is a {@link cn.taketoday.beans.SimpleTypeConverter}.
   * Can be overridden with any TypeConverter implementation, typically
   * a pre-configured SimpleTypeConverter or a BeanWrapperImpl instance.
   *
   * @see cn.taketoday.beans.SimpleTypeConverter
   * @see cn.taketoday.beans.BeanWrapperImpl
   */
  public void setTypeConverter(@Nullable TypeConverter typeConverter) {
    this.typeConverter = typeConverter;
    this.useDefaultConverter = (typeConverter == null);
  }

  /**
   * Return the TypeConverter used for argument type conversion.
   * <p>Can be cast to {@link cn.taketoday.beans.PropertyEditorRegistry}
   * if direct access to the underlying PropertyEditors is desired
   * (provided that the present TypeConverter actually implements the
   * PropertyEditorRegistry interface).
   */
  @Nullable
  public TypeConverter getTypeConverter() {
    if (this.typeConverter == null && this.useDefaultConverter) {
      this.typeConverter = getDefaultTypeConverter();
    }
    return this.typeConverter;
  }

  /**
   * Obtain the default TypeConverter for this method invoker.
   * <p>Called if no explicit TypeConverter has been specified.
   * The default implementation builds a
   * {@link cn.taketoday.beans.SimpleTypeConverter}.
   * Can be overridden in subclasses.
   */
  protected TypeConverter getDefaultTypeConverter() {
    return new SimpleTypeConverter();
  }

  /**
   * Register the given custom property editor for all properties of the given type.
   * <p>Typically used in conjunction with the default
   * {@link cn.taketoday.beans.SimpleTypeConverter}; will work with any
   * TypeConverter that implements the PropertyEditorRegistry interface as well.
   *
   * @param requiredType type of the property
   * @param propertyEditor editor to register
   * @see #setTypeConverter
   * @see cn.taketoday.beans.PropertyEditorRegistry#registerCustomEditor
   */
  public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
    TypeConverter converter = getTypeConverter();
    if (!(converter instanceof PropertyEditorRegistry)) {
      throw new IllegalStateException(
              "TypeConverter does not implement PropertyEditorRegistry interface: " + converter);
    }
    ((PropertyEditorRegistry) converter).registerCustomEditor(requiredType, propertyEditor);
  }

  /**
   * This implementation looks for a method with matching parameter types.
   *
   * @see #doFindMatchingMethod
   */
  @Override
  protected Method findMatchingMethod() {
    Method matchingMethod = super.findMatchingMethod();
    // Second pass: look for method where arguments can be converted to parameter types.
    if (matchingMethod == null) {
      // Interpret argument array as individual method arguments.
      matchingMethod = doFindMatchingMethod(getArguments());
    }
    if (matchingMethod == null) {
      // Interpret argument array as single method argument of array type.
      matchingMethod = doFindMatchingMethod(new Object[] { getArguments() });
    }
    return matchingMethod;
  }

  /**
   * Actually find a method with matching parameter type, i.e. where each
   * argument value is assignable to the corresponding parameter type.
   *
   * @param arguments the argument values to match against method parameters
   * @return a matching method, or {@code null} if none
   */
  @Nullable
  protected Method doFindMatchingMethod(Object[] arguments) {
    TypeConverter converter = getTypeConverter();
    if (converter != null) {
      String targetMethod = getTargetMethod();
      Method matchingMethod = null;
      int argCount = arguments.length;
      Class<?> targetClass = getTargetClass();
      Assert.state(targetClass != null, "No target class set");
      Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
      int minTypeDiffWeight = Integer.MAX_VALUE;
      Object[] argumentsToUse = null;
      for (Method candidate : candidates) {
        if (candidate.getName().equals(targetMethod)) {
          // Check if the inspected method has the correct number of parameters.
          int parameterCount = candidate.getParameterCount();
          if (parameterCount == argCount) {
            Class<?>[] paramTypes = candidate.getParameterTypes();
            Object[] convertedArguments = new Object[argCount];
            boolean match = true;
            for (int j = 0; j < argCount && match; j++) {
              // Verify that the supplied argument is assignable to the method parameter.
              try {
                convertedArguments[j] = converter.convertIfNecessary(arguments[j], paramTypes[j]);
              }
              catch (TypeMismatchException ex) {
                // Ignore -> simply doesn't match.
                match = false;
              }
            }
            if (match) {
              int typeDiffWeight = getTypeDifferenceWeight(paramTypes, convertedArguments);
              if (typeDiffWeight < minTypeDiffWeight) {
                minTypeDiffWeight = typeDiffWeight;
                matchingMethod = candidate;
                argumentsToUse = convertedArguments;
              }
            }
          }
        }
      }
      if (matchingMethod != null) {
        setArguments(argumentsToUse);
        return matchingMethod;
      }
    }
    return null;
  }

}

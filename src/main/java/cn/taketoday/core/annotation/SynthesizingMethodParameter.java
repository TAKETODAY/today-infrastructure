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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.util.ReflectionUtils;

/**
 * A {@link MethodParameter} variant which synthesizes annotations that
 * declare attribute aliases via {@link AliasFor @AliasFor}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see AnnotationUtils#synthesizeAnnotation
 * @see AnnotationUtils#synthesizeAnnotationArray
 * @since 4.0
 */
public class SynthesizingMethodParameter extends MethodParameter {

  /**
   * Create a new {@code SynthesizingMethodParameter} for the given method,
   * with nesting level 1.
   *
   * @param method the Method to specify a parameter for
   * @param parameterIndex the index of the parameter: -1 for the method
   * return type; 0 for the first method parameter; 1 for the second method
   * parameter, etc.
   */
  public SynthesizingMethodParameter(Method method, int parameterIndex) {
    super(method, parameterIndex);
  }

  /**
   * Create a new {@code SynthesizingMethodParameter} for the given method.
   *
   * @param method the Method to specify a parameter for
   * @param parameterIndex the index of the parameter: -1 for the method
   * return type; 0 for the first method parameter; 1 for the second method
   * parameter, etc.
   * @param nestingLevel the nesting level of the target type
   * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
   * nested List, whereas 2 would indicate the element of the nested List)
   */
  public SynthesizingMethodParameter(Method method, int parameterIndex, int nestingLevel) {
    super(method, parameterIndex, nestingLevel);
  }

  /**
   * Create a new {@code SynthesizingMethodParameter} for the given constructor,
   * with nesting level 1.
   *
   * @param constructor the Constructor to specify a parameter for
   * @param parameterIndex the index of the parameter
   */
  public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex) {
    super(constructor, parameterIndex);
  }

  /**
   * Create a new {@code SynthesizingMethodParameter} for the given constructor.
   *
   * @param constructor the Constructor to specify a parameter for
   * @param parameterIndex the index of the parameter
   * @param nestingLevel the nesting level of the target type
   * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
   * nested List, whereas 2 would indicate the element of the nested List)
   */
  public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
    super(constructor, parameterIndex, nestingLevel);
  }

  /**
   * Copy constructor, resulting in an independent {@code SynthesizingMethodParameter}
   * based on the same metadata and cache state that the original object was in.
   *
   * @param original the original SynthesizingMethodParameter object to copy from
   */
  protected SynthesizingMethodParameter(SynthesizingMethodParameter original) {
    super(original);
  }

  @Override
  protected <A extends Annotation> A adaptAnnotation(A annotation) {
    return AnnotationUtils.synthesizeAnnotation(annotation, getAnnotatedElement());
  }

  @Override
  protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
    return AnnotationUtils.synthesizeAnnotationArray(annotations, getAnnotatedElement());
  }

  @Override
  public SynthesizingMethodParameter clone() {
    return new SynthesizingMethodParameter(this);
  }

  /**
   * Create a new SynthesizingMethodParameter for the given method or constructor.
   * <p>This is a convenience factory method for scenarios where a
   * Method or Constructor reference is treated in a generic fashion.
   *
   * @param executable the Method or Constructor to specify a parameter for
   * @param parameterIndex the index of the parameter
   * @return the corresponding SynthesizingMethodParameter instance
   */
  public static SynthesizingMethodParameter forExecutable(Executable executable, int parameterIndex) {
    if (executable instanceof Method) {
      return new SynthesizingMethodParameter((Method) executable, parameterIndex);
    }
    else if (executable instanceof Constructor) {
      return new SynthesizingMethodParameter((Constructor<?>) executable, parameterIndex);
    }
    else {
      throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
    }
  }

  /**
   * Create a new SynthesizingMethodParameter for the given parameter descriptor.
   * <p>This is a convenience factory method for scenarios where a
   * Java 8 {@link Parameter} descriptor is already available.
   *
   * @param parameter the parameter descriptor
   * @return the corresponding SynthesizingMethodParameter instance
   */
  public static SynthesizingMethodParameter forParameter(Parameter parameter) {
    return forExecutable(parameter.getDeclaringExecutable(), ReflectionUtils.getParameterIndex(parameter));
  }

}

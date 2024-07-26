/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.ReflectionHints;

/**
 * A simple {@link ReflectiveProcessor} implementation that registers only a
 * reflection hint for the annotated type. Can be sub-classed to customize
 * processing for a given {@link AnnotatedElement} type.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class SimpleReflectiveProcessor implements ReflectiveProcessor {

  @Override
  public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
    if (element instanceof Class<?> type) {
      registerTypeHint(hints, type);
    }
    else if (element instanceof Constructor<?> constructor) {
      registerConstructorHint(hints, constructor);
    }
    else if (element instanceof Field field) {
      registerFieldHint(hints, field);
    }
    else if (element instanceof Method method) {
      registerMethodHint(hints, method);
    }
  }

  /**
   * Register {@link ReflectionHints} against the specified {@link Class}.
   *
   * @param hints the reflection hints instance to use
   * @param type the class to process
   */
  protected void registerTypeHint(ReflectionHints hints, Class<?> type) {
    hints.registerType(type);
  }

  /**
   * Register {@link ReflectionHints} against the specified {@link Constructor}.
   *
   * @param hints the reflection hints instance to use
   * @param constructor the constructor to process
   */
  protected void registerConstructorHint(ReflectionHints hints, Constructor<?> constructor) {
    hints.registerConstructor(constructor, ExecutableMode.INVOKE);
  }

  /**
   * Register {@link ReflectionHints} against the specified {@link Field}.
   *
   * @param hints the reflection hints instance to use
   * @param field the field to process
   */
  protected void registerFieldHint(ReflectionHints hints, Field field) {
    hints.registerField(field);
  }

  /**
   * Register {@link ReflectionHints} against the specified {@link Method}.
   *
   * @param hints the reflection hints instance to use
   * @param method the method to process
   */
  protected void registerMethodHint(ReflectionHints hints, Method method) {
    hints.registerMethod(method, ExecutableMode.INVOKE);
  }

}

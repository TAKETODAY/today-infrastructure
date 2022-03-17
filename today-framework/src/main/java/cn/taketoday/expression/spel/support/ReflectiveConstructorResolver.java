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

package cn.taketoday.expression.spel.support;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.ConstructorExecutor;
import cn.taketoday.expression.ConstructorResolver;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.lang.Nullable;

/**
 * A constructor resolver that uses reflection to locate the constructor that should be invoked.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ReflectiveConstructorResolver implements ConstructorResolver {

  /**
   * Locate a constructor on the type. There are three kinds of match that might occur:
   * <ol>
   * <li>An exact match where the types of the arguments match the types of the constructor
   * <li>An in-exact match where the types we are looking for are subtypes of those defined on the constructor
   * <li>A match where we are able to convert the arguments into those expected by the constructor, according to the
   * registered type converter.
   * </ol>
   */
  @Override
  @Nullable
  public ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
          throws AccessException {

    try {
      TypeConverter typeConverter = context.getTypeConverter();
      Class<?> type = context.getTypeLocator().findType(typeName);
      Constructor<?>[] ctors = type.getConstructors();

      Arrays.sort(ctors, Comparator.comparingInt(Constructor::getParameterCount));

      Constructor<?> closeMatch = null;
      Constructor<?> matchRequiringConversion = null;

      for (Constructor<?> ctor : ctors) {
        int paramCount = ctor.getParameterCount();
        List<TypeDescriptor> paramDescriptors = new ArrayList<>(paramCount);
        for (int i = 0; i < paramCount; i++) {
          paramDescriptors.add(new TypeDescriptor(new MethodParameter(ctor, i)));
        }
        ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
        if (ctor.isVarArgs() && argumentTypes.size() >= paramCount - 1) {
          // *sigh* complicated
          // Basically.. we have to have all parameters match up until the varargs one, then the rest of what is
          // being provided should be
          // the same type whilst the final argument to the method must be an array of that (oh, how easy...not) -
          // or the final parameter
          // we are supplied does match exactly (it is an array already).
          matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
        }
        else if (paramCount == argumentTypes.size()) {
          // worth a closer look
          matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
        }
        if (matchInfo != null) {
          if (matchInfo.isExactMatch()) {
            return new ReflectiveConstructorExecutor(ctor);
          }
          else if (matchInfo.isCloseMatch()) {
            closeMatch = ctor;
          }
          else if (matchInfo.isMatchRequiringConversion()) {
            matchRequiringConversion = ctor;
          }
        }
      }

      if (closeMatch != null) {
        return new ReflectiveConstructorExecutor(closeMatch);
      }
      else if (matchRequiringConversion != null) {
        return new ReflectiveConstructorExecutor(matchRequiringConversion);
      }
      else {
        return null;
      }
    }
    catch (EvaluationException ex) {
      throw new AccessException("Failed to resolve constructor", ex);
    }
  }

}

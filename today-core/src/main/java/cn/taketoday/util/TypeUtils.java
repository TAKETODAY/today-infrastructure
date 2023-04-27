/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Utility to work with Java 5 generic type parameters.
 * Mainly for internal use within the framework.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY 2021/9/6 22:29
 * @since 4.0
 */
public abstract class TypeUtils {

  /**
   * Check if the right-hand side type may be assigned to the left-hand side
   * type following the Java generics rules.
   *
   * @param lhsType the target type (left-hand side (LHS) type)
   * @param rhsType the value type (right-hand side (RHS) type) that should
   * be assigned to the target type
   * @return {@code true} if {@code rhsType} is assignable to {@code lhsType}
   * @see ClassUtils#isAssignable(Class, Class)
   */
  public static boolean isAssignable(Type lhsType, Type rhsType) {
    Assert.notNull(lhsType, "Left-hand side type must not be null");
    Assert.notNull(rhsType, "Right-hand side type must not be null");

    // all types are assignable to themselves and to class Object
    if (lhsType.equals(rhsType) || Object.class == lhsType) {
      return true;
    }

    if (lhsType instanceof Class<?> lhsClass) {

      // just comparing two classes
      if (rhsType instanceof Class) {
        return ClassUtils.isAssignable(lhsClass, (Class<?>) rhsType);
      }

      if (rhsType instanceof ParameterizedType) {
        Type rhsRaw = ((ParameterizedType) rhsType).getRawType();

        // a parameterized type is always assignable to its raw class type
        if (rhsRaw instanceof Class) {
          return ClassUtils.isAssignable(lhsClass, (Class<?>) rhsRaw);
        }
      }
      else if (lhsClass.isArray() && rhsType instanceof GenericArrayType) {
        Type rhsComponent = ((GenericArrayType) rhsType).getGenericComponentType();

        return isAssignable(lhsClass.getComponentType(), rhsComponent);
      }
    }

    // parameterized types are only assignable to other parameterized types and class types
    if (lhsType instanceof ParameterizedType) {
      if (rhsType instanceof Class) {
        Type lhsRaw = ((ParameterizedType) lhsType).getRawType();

        if (lhsRaw instanceof Class) {
          return ClassUtils.isAssignable((Class<?>) lhsRaw, (Class<?>) rhsType);
        }
      }
      else if (rhsType instanceof ParameterizedType) {
        return isAssignable((ParameterizedType) lhsType, (ParameterizedType) rhsType);
      }
    }

    if (lhsType instanceof GenericArrayType) {
      Type lhsComponent = ((GenericArrayType) lhsType).getGenericComponentType();

      if (rhsType instanceof Class<?> rhsClass) {
        if (rhsClass.isArray()) {
          return isAssignable(lhsComponent, rhsClass.getComponentType());
        }
      }
      else if (rhsType instanceof GenericArrayType) {
        Type rhsComponent = ((GenericArrayType) rhsType).getGenericComponentType();

        return isAssignable(lhsComponent, rhsComponent);
      }
    }

    if (lhsType instanceof WildcardType) {
      return isAssignable((WildcardType) lhsType, rhsType);
    }

    return false;
  }

  private static boolean isAssignable(ParameterizedType lhsType, ParameterizedType rhsType) {
    if (lhsType.equals(rhsType)) {
      return true;
    }

    Type[] lhsTypeArguments = lhsType.getActualTypeArguments();
    Type[] rhsTypeArguments = rhsType.getActualTypeArguments();

    if (lhsTypeArguments.length != rhsTypeArguments.length) {
      return false;
    }

    for (int size = lhsTypeArguments.length, i = 0; i < size; ++i) {
      Type lhsArg = lhsTypeArguments[i];
      Type rhsArg = rhsTypeArguments[i];

      if (!lhsArg.equals(rhsArg)
              && !(lhsArg instanceof WildcardType && isAssignable((WildcardType) lhsArg, rhsArg))) {
        return false;
      }
    }

    return true;
  }

  private static boolean isAssignable(WildcardType lhsType, Type rhsType) {
    Type[] lUpperBounds = getLowerBounds(lhsType);
    Type[] lLowerBounds = getLowerBounds(lhsType);

    if (rhsType instanceof WildcardType rhsWcType) {
      // both the upper and lower bounds of the right-hand side must be
      // completely enclosed in the upper and lower bounds of the left-
      // hand side.
      Type[] rUpperBounds = getUpperBounds(rhsWcType);
      Type[] rLowerBounds = getLowerBounds(rhsWcType);

      for (Type lBound : lUpperBounds) {
        for (Type rBound : rUpperBounds) {
          if (isNotAssignableBound(lBound, rBound)) {
            return false;
          }
        }

        for (Type rBound : rLowerBounds) {
          if (isNotAssignableBound(lBound, rBound)) {
            return false;
          }
        }
      }

      for (Type lBound : lLowerBounds) {
        for (Type rBound : rUpperBounds) {
          if (isNotAssignableBound(rBound, lBound)) {
            return false;
          }
        }

        for (Type rBound : rLowerBounds) {
          if (isNotAssignableBound(rBound, lBound)) {
            return false;
          }
        }
      }
    }
    else {
      for (Type lBound : lUpperBounds) {
        if (isNotAssignableBound(lBound, rhsType)) {
          return false;
        }
      }

      for (Type lBound : lLowerBounds) {
        if (isNotAssignableBound(rhsType, lBound)) {
          return false;
        }
      }
    }

    return true;
  }

  public static boolean isNotAssignableBound(@Nullable Type lhsType, @Nullable Type rhsType) {
    if (rhsType == null) {
      return false;
    }
    if (lhsType == null) {
      return true;
    }
    return !isAssignable(lhsType, rhsType);
  }

  private static Type[] getLowerBounds(WildcardType wildcardType) {
    Type[] lowerBounds = wildcardType.getLowerBounds();

    // supply the implicit lower bound if none are specified
    if (lowerBounds.length == 0) {
      lowerBounds = new Type[] { null };
    }
    return lowerBounds;
  }

  private static Type[] getUpperBounds(WildcardType wildcardType) {
    Type[] upperBounds = wildcardType.getUpperBounds();

    // supply the implicit upper bound if none are specified
    if (upperBounds.length == 0) {
      upperBounds = new Type[] { Object.class };
    }
    return upperBounds;
  }

}

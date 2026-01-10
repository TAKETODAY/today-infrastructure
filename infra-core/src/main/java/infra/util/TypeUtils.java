/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import infra.lang.Assert;

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
    Assert.notNull(lhsType, "Left-hand side type is required");
    Assert.notNull(rhsType, "Right-hand side type is required");

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
    Type[] lUpperBounds = getUpperBounds(lhsType);
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

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

package cn.taketoday.aot.generate;

import cn.taketoday.javapoet.ClassName;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * Determine the access control of a {@link Member} or type signature.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public final class AccessControl {

  private final Class<?> target;

  private final Visibility visibility;

  AccessControl(Class<?> target, Visibility visibility) {
    this.target = target;
    this.visibility = visibility;
  }

  /**
   * Create an {@link AccessControl} for the given member. This considers the
   * member modifier, parameter types, return types and any enclosing classes.
   * The lowest overall {@link Visibility} is used.
   *
   * @param member the source member
   * @return the {@link AccessControl} for the member
   */
  public static AccessControl forMember(Member member) {
    return new AccessControl(member.getDeclaringClass(), Visibility.forMember(member));
  }

  /**
   * Create an {@link AccessControl} for the given {@link ResolvableType}.
   * This considers the type itself as well as any generics.
   *
   * @param resolvableType the source resolvable type
   * @return the {@link AccessControl} for the type
   */
  public static AccessControl forResolvableType(ResolvableType resolvableType) {
    return new AccessControl(resolvableType.toClass(),
            Visibility.forResolvableType(resolvableType));
  }

  /**
   * Create an {@link AccessControl} for the given {@link Class}.
   *
   * @param type the source class
   * @return the {@link AccessControl} for the class
   */
  public static AccessControl forClass(Class<?> type) {
    return new AccessControl(type, Visibility.forClass(type));
  }

  /**
   * Returns the lowest {@link AccessControl} from the given candidates.
   *
   * @param candidates the candidates to check
   * @return the lowest {@link AccessControl} from the candidates
   */
  public static AccessControl lowest(AccessControl... candidates) {
    int index = Visibility.lowestIndex(Arrays.stream(candidates)
            .map(AccessControl::getVisibility).toArray(Visibility[]::new));
    return candidates[index];
  }

  /**
   * Return the lowest {@link Visibility} of this instance.
   *
   * @return the visibility
   */
  public Visibility getVisibility() {
    return this.visibility;
  }

  /**
   * Return whether the member or type signature backed by ths instance is
   * accessible from any package.
   *
   * @return {@code true} if it is public
   */
  public boolean isPublic() {
    return this.visibility == Visibility.PUBLIC;
  }

  /**
   * Specify whether the member or type signature backed by this instance is
   * accessible from the specified {@link ClassName}.
   *
   * @param type the type to check
   * @return {@code true} if it is accessible
   */
  public boolean isAccessibleFrom(ClassName type) {
    if (this.visibility == Visibility.PRIVATE) {
      return false;
    }
    if (this.visibility == Visibility.PUBLIC) {
      return true;
    }
    return this.target.getPackageName().equals(type.packageName());
  }

  /**
   * Access visibility types as determined by the <a href=
   * "https://docs.oracle.com/javase/tutorial/java/javaOO/accesscontrol.html">modifiers</a>
   * on a {@link Member} or {@link ResolvableType}.
   */
  public enum Visibility {

    /**
     * Public visibility. The member or type is visible to all classes.
     */
    PUBLIC,

    /**
     * Protected visibility. The member or type is only visible to classes
     * in the same package or subclasses.
     */
    PROTECTED,

    /**
     * Package-private visibility. The member or type is only visible to classes
     * in the same package.
     */
    PACKAGE_PRIVATE,

    /**
     * Private visibility. The member or type is not visible to other classes.
     */
    PRIVATE;

    private static Visibility forMember(Member member) {
      Assert.notNull(member, "'member' must not be null");
      Visibility visibility = forModifiers(member.getModifiers());
      Visibility declaringClassVisibility = forClass(member.getDeclaringClass());
      visibility = lowest(visibility, declaringClassVisibility);
      if (visibility != PRIVATE) {
        if (member instanceof Field field) {
          Visibility fieldVisibility = forResolvableType(
                  ResolvableType.fromField(field));
          return lowest(visibility, fieldVisibility);
        }
        if (member instanceof Constructor<?> constructor) {
          Visibility parameterVisibility = forParameterTypes(constructor,
                  i -> ResolvableType.forMethodParameter(new MethodParameter(constructor, i)));
          return lowest(visibility, parameterVisibility);
        }
        if (member instanceof Method method) {
          Visibility parameterVisibility = forParameterTypes(method,
                  i -> ResolvableType.forMethodParameter(new MethodParameter(method, i)));
          Visibility returnTypeVisibility = forResolvableType(
                  ResolvableType.forReturnType(method));
          return lowest(visibility, parameterVisibility, returnTypeVisibility);
        }
      }
      return PRIVATE;
    }

    private static Visibility forResolvableType(ResolvableType resolvableType) {
      return forResolvableType(resolvableType, new HashSet<>());
    }

    private static Visibility forResolvableType(ResolvableType resolvableType,
            Set<ResolvableType> seen) {
      if (!seen.add(resolvableType)) {
        return Visibility.PUBLIC;
      }
      Class<?> userClass = ClassUtils.getUserClass(resolvableType.toClass());
      ResolvableType userType = resolvableType.as(userClass);
      Visibility visibility = forClass(userType.toClass());
      for (ResolvableType generic : userType.getGenerics()) {
        visibility = lowest(visibility, forResolvableType(generic, seen));
      }
      return visibility;
    }

    private static Visibility forParameterTypes(Executable executable,
            IntFunction<ResolvableType> resolvableTypeFactory) {
      Visibility visibility = Visibility.PUBLIC;
      Class<?>[] parameterTypes = executable.getParameterTypes();
      for (int i = 0; i < parameterTypes.length; i++) {
        ResolvableType type = resolvableTypeFactory.apply(i);
        visibility = lowest(visibility, forResolvableType(type));
      }
      return visibility;
    }

    private static Visibility forClass(Class<?> clazz) {
      clazz = ClassUtils.getUserClass(clazz);
      Visibility visibility = forModifiers(clazz.getModifiers());
      if (clazz.isArray()) {
        visibility = lowest(visibility, forClass(clazz.getComponentType()));
      }
      Class<?> enclosingClass = clazz.getEnclosingClass();
      if (enclosingClass != null) {
        visibility = lowest(visibility, forClass(clazz.getEnclosingClass()));
      }
      return visibility;
    }

    private static Visibility forModifiers(int modifiers) {
      if (Modifier.isPublic(modifiers)) {
        return PUBLIC;
      }
      if (Modifier.isProtected(modifiers)) {
        return PROTECTED;
      }
      if (Modifier.isPrivate(modifiers)) {
        return PRIVATE;
      }
      return PACKAGE_PRIVATE;
    }

    /**
     * Returns the lowest {@link Visibility} from the given candidates.
     *
     * @param candidates the candidates to check
     * @return the lowest {@link Visibility} from the candidates
     */
    static Visibility lowest(Visibility... candidates) {
      Visibility visibility = PUBLIC;
      for (Visibility candidate : candidates) {
        if (candidate.ordinal() > visibility.ordinal()) {
          visibility = candidate;
        }
      }
      return visibility;
    }

    /**
     * Returns the index of the lowest {@link Visibility} from the given
     * candidates.
     *
     * @param candidates the candidates to check
     * @return the index of the lowest {@link Visibility} from the candidates
     */
    static int lowestIndex(Visibility... candidates) {
      Visibility visibility = PUBLIC;
      int index = 0;
      for (int i = 0; i < candidates.length; i++) {
        Visibility candidate = candidates[i];
        if (candidate.ordinal() > visibility.ordinal()) {
          visibility = candidate;
          index = i;
        }
      }
      return index;
    }

  }
}

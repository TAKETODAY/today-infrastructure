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

package cn.taketoday.context.properties.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Default {@link BindConstructorProvider} implementation.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {

  @Override
  public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
    Constructors constructors = Constructors.getConstructors(
            bindable.getType().resolve(), isNestedConstructorBinding);
    if (constructors.getBind() != null && constructors.isDeducedBindConstructor()) {
      if (bindable.getValue() != null && bindable.getValue().get() != null) {
        return null;
      }
    }
    return constructors.getBind();
  }

  @Override
  public Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
    Constructors constructors = Constructors.getConstructors(type, isNestedConstructorBinding);
    return constructors.getBind();
  }

  /**
   * Data holder for autowired and bind constructors.
   */
  static final class Constructors {

    private static final Constructors NONE = new Constructors(
            false, null, false);

    private final boolean hasAutowired;

    @Nullable
    private final Constructor<?> bind;

    private final boolean deducedBindConstructor;

    private Constructors(boolean hasAutowired, @Nullable Constructor<?> bind, boolean deducedBindConstructor) {
      this.hasAutowired = hasAutowired;
      this.bind = bind;
      this.deducedBindConstructor = deducedBindConstructor;
    }

    boolean hasAutowired() {
      return this.hasAutowired;
    }

    @Nullable
    Constructor<?> getBind() {
      return this.bind;
    }

    boolean isDeducedBindConstructor() {
      return this.deducedBindConstructor;
    }

    static Constructors getConstructors(@Nullable Class<?> type, boolean isNestedConstructorBinding) {
      if (type == null) {
        return NONE;
      }
      boolean hasAutowiredConstructor = isAutowiredPresent(type);
      Constructor<?>[] candidates = getCandidateConstructors(type);
      MergedAnnotations[] candidateAnnotations = getAnnotations(candidates);
      boolean deducedBindConstructor = false;
      Constructor<?> bind = getConstructorBindingAnnotated(type, candidates, candidateAnnotations);
      if (bind == null && !hasAutowiredConstructor) {
        bind = deduceBindConstructor(type, candidates);
        deducedBindConstructor = bind != null;
      }

      if (bind != null || isNestedConstructorBinding) {
        Assert.state(!hasAutowiredConstructor,
                () -> type.getName() + " declares @ConstructorBinding and @Autowired constructor");
      }
      return new Constructors(hasAutowiredConstructor, bind, deducedBindConstructor);
    }

    private static boolean isAutowiredPresent(Class<?> type) {
      if (Stream.of(type.getDeclaredConstructors()).map(MergedAnnotations::from)
              .anyMatch((annotations) -> annotations.isPresent(Autowired.class))) {
        return true;
      }
      Class<?> userClass = ClassUtils.getUserClass(type);
      return userClass != type && isAutowiredPresent(userClass);
    }

    private static Constructor<?>[] getCandidateConstructors(Class<?> type) {
      if (isInnerClass(type)) {
        return new Constructor<?>[0];
      }
      return Arrays.stream(type.getDeclaredConstructors())
              .filter((constructor) -> isNonSynthetic(constructor, type)).toArray(Constructor[]::new);
    }

    private static boolean isInnerClass(Class<?> type) {
      try {
        return type.getDeclaredField("this$0").isSynthetic();
      }
      catch (NoSuchFieldException ex) {
        return false;
      }
    }

    private static boolean isNonSynthetic(Constructor<?> constructor, Class<?> type) {
      return !constructor.isSynthetic();
    }

    private static MergedAnnotations[] getAnnotations(Constructor<?>[] candidates) {
      MergedAnnotations[] candidateAnnotations = new MergedAnnotations[candidates.length];
      for (int i = 0; i < candidates.length; i++) {
        candidateAnnotations[i] = MergedAnnotations.from(candidates[i], SearchStrategy.SUPERCLASS);
      }
      return candidateAnnotations;
    }

    @Nullable
    private static Constructor<?> getConstructorBindingAnnotated(Class<?> type, Constructor<?>[] candidates,
            MergedAnnotations[] mergedAnnotations) {
      Constructor<?> result = null;
      for (int i = 0; i < candidates.length; i++) {
        if (mergedAnnotations[i].isPresent(ConstructorBinding.class)) {
          if (candidates[i].getParameterCount() <= 0) {
            throw new IllegalStateException(
                    type.getName() + " declares @ConstructorBinding on a no-args constructor");
          }
          if (result != null) {
            throw new IllegalStateException(
                    type.getName() + " has more than one @ConstructorBinding constructor");
          }

          result = candidates[i];
        }
      }
      return result;
    }

    @Nullable
    private static Constructor<?> deduceBindConstructor(Class<?> type, Constructor<?>[] candidates) {
      if (candidates.length == 1 && candidates[0].getParameterCount() > 0) {
        if (type.isMemberClass() && Modifier.isPrivate(candidates[0].getModifiers())) {
          return null;
        }
        return candidates[0];
      }
      Constructor<?> result = null;
      for (Constructor<?> candidate : candidates) {
        if (!Modifier.isPrivate(candidate.getModifiers())) {
          if (result != null) {
            return null;
          }
          result = candidate;
        }
      }
      return (result != null && result.getParameterCount() > 0) ? result : null;
    }

  }

}

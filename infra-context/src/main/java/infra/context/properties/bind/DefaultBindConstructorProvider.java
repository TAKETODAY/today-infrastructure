/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties.bind;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import infra.beans.factory.annotation.Autowired;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.util.ClassUtils;

/**
 * Default {@link BindConstructorProvider} implementation.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {

  @SuppressWarnings("NullAway")
  @Nullable
  @Override
  public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
    Constructors constructors = Constructors.getConstructors(
            bindable.getType().resolve(), isNestedConstructorBinding);
    if (constructors.bind != null
            && constructors.deducedBindConstructor
            && !constructors.immutableType) {
      if (bindable.getValue() != null && bindable.getValue().get() != null) {
        return null;
      }
    }
    return constructors.bind;
  }

  @Nullable
  @Override
  public Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
    Constructors constructors = Constructors.getConstructors(type, isNestedConstructorBinding);
    return constructors.bind;
  }

  /**
   * Data holder for autowired and bind constructors.
   */
  static final class Constructors {

    private static final Constructors NONE = new Constructors(
            false, null, false, false);

    public final boolean hasAutowired;

    @Nullable
    public final Constructor<?> bind;

    public final boolean immutableType;
    public final boolean deducedBindConstructor;

    private Constructors(boolean hasAutowired, @Nullable Constructor<?> bind,
            boolean deducedBindConstructor, boolean immutableType) {
      this.hasAutowired = hasAutowired;
      this.bind = bind;
      this.deducedBindConstructor = deducedBindConstructor;
      this.immutableType = immutableType;
    }

    static Constructors getConstructors(@Nullable Class<?> type, boolean isNestedConstructorBinding) {
      if (type == null) {
        return NONE;
      }
      boolean hasAutowiredConstructor = isAutowiredPresent(type);
      Constructor<?>[] candidates = getCandidateConstructors(type);
      MergedAnnotations[] candidateAnnotations = getAnnotations(candidates);
      boolean deducedBindConstructor = false;
      boolean immutableType = type.isRecord();
      Constructor<?> bind = getConstructorBindingAnnotated(type, candidates, candidateAnnotations);
      if (bind == null && !hasAutowiredConstructor) {
        bind = deduceBindConstructor(type, candidates);
        deducedBindConstructor = bind != null;
      }

      if (bind != null || isNestedConstructorBinding) {
        if (hasAutowiredConstructor) {
          throw new IllegalStateException(type.getName() + " declares @ConstructorBinding and @Autowired constructor");
        }
      }
      return new Constructors(hasAutowiredConstructor, bind, deducedBindConstructor, immutableType);
    }

    private static boolean isAutowiredPresent(Class<?> type) {
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        if (MergedAnnotations.from(constructor).isPresent(Autowired.class)) {
          return true;
        }
      }
      Class<?> userClass = ClassUtils.getUserClass(type);
      return userClass != type && isAutowiredPresent(userClass);
    }

    private static Constructor<?>[] getCandidateConstructors(Class<?> type) {
      if (isInnerClass(type)) {
        return new Constructor<?>[0];
      }

      ArrayList<Constructor<?>> constructors = new ArrayList<>();
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        if (!constructor.isSynthetic()) {
          constructors.add(constructor);
        }
      }
      return constructors.toArray(new Constructor[0]);
    }

    private static boolean isInnerClass(Class<?> type) {
      try {
        return type.getDeclaredField("this$0").isSynthetic();
      }
      catch (NoSuchFieldException ex) {
        return false;
      }
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

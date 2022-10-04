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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.properties.bind.BindConstructorProvider;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Nullable;

/**
 * {@link BindConstructorProvider} used when binding
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConfigurationPropertiesBindConstructorProvider implements BindConstructorProvider {

  /**
   * A shared singleton {@link ConfigurationPropertiesBindConstructorProvider} instance.
   */
  public static final ConfigurationPropertiesBindConstructorProvider INSTANCE = new ConfigurationPropertiesBindConstructorProvider();

  @Override
  public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
    return getBindConstructor(bindable.getType().resolve(), isNestedConstructorBinding);
  }

  @Nullable
  Constructor<?> getBindConstructor(@Nullable Class<?> type, boolean isNestedConstructorBinding) {
    if (type == null) {
      return null;
    }
    Constructors constructors = Constructors.getConstructors(type);
    if (constructors.getBind() != null || isNestedConstructorBinding) {
      if (constructors.hasAutowired()) {
        throw new IllegalStateException(type.getName() + " declares @ConstructorBinding and @Autowired constructor");
      }
    }
    return constructors.getBind();
  }

  /**
   * Data holder for autowired and bind constructors.
   */
  private record Constructors(boolean hasAutowired, @Nullable Constructor<?> bind) {

    @Nullable
    Constructor<?> getBind() {
      return this.bind;
    }

    static Constructors getConstructors(Class<?> type) {
      Constructor<?>[] candidates = getCandidateConstructors(type);
      Constructor<?> deducedBind = deduceBindConstructor(candidates);
      if (deducedBind != null) {
        return new Constructors(false, deducedBind);
      }
      boolean hasAutowiredConstructor = false;
      Constructor<?> bind = null;
      for (Constructor<?> candidate : candidates) {
        if (isAutowired(candidate)) {
          hasAutowiredConstructor = true;
          continue;
        }
        bind = findAnnotatedConstructor(type, bind, candidate);
      }
      return new Constructors(hasAutowiredConstructor, bind);
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

    @Nullable
    private static Constructor<?> deduceBindConstructor(Constructor<?>[] constructors) {
      if (constructors.length == 1 && constructors[0].getParameterCount() > 0 && !isAutowired(constructors[0])) {
        return constructors[0];
      }
      return null;
    }

    private static boolean isAutowired(Constructor<?> candidate) {
      return MergedAnnotations.from(candidate).isPresent(Autowired.class);
    }

    @Nullable
    private static Constructor<?> findAnnotatedConstructor(
            Class<?> type, @Nullable Constructor<?> constructor, Constructor<?> candidate) {
      if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
        if (candidate.getParameterCount() <= 0) {
          throw new IllegalStateException(type.getName() + " declares @ConstructorBinding on a no-args constructor");
        }
        if (constructor != null) {
          throw new IllegalStateException(type.getName() + " has more than one @ConstructorBinding constructor");
        }
        constructor = candidate;
      }
      return constructor;
    }

  }

}

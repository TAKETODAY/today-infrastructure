/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/15 16:49
 */
class MethodIntrospectorTests {

  @Test
  void selectMethodsAndClearDeclaredMethodsCacheBetweenInvocations() {
    Class<?> targetType = ActualController.class;

    // Preconditions for this use case.
    assertThat(targetType).isPublic();
    assertThat(targetType.getSuperclass()).isPackagePrivate();

    MethodIntrospector.MetadataLookup<String> metadataLookup = method -> {
      if (MergedAnnotations.from(method, TYPE_HIERARCHY).isPresent(Mapped.class)) {
        return method.getName();
      }
      return null;
    };

    // Start with a clean slate.
    ReflectionUtils.clearCache();

    // Round #1
    Map<Method, String> methods = MethodIntrospector.selectMethods(targetType, metadataLookup);
    assertThat(methods.values()).containsExactlyInAnyOrder("update", "delete");

    // Simulate ConfigurableApplicationContext#refresh() which clears the
    // ReflectionUtils#declaredMethodsCache but NOT the BridgeMethodResolver#cache.
    // As a consequence, ReflectionUtils.getDeclaredMethods(...) will return a
    // new set of methods that are logically equivalent to but not identical
    // to (in terms of object identity) any bridged methods cached in the
    // BridgeMethodResolver cache.
    ReflectionUtils.clearCache();

    // Round #2
    methods = MethodIntrospector.selectMethods(targetType, metadataLookup);
    assertThat(methods.values()).containsExactlyInAnyOrder("update", "delete");
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Mapped {
  }

  interface Controller {

    void unmappedMethod();

    @Mapped
    void update();

    @Mapped
    void delete();
  }

  // Must NOT be public.
  abstract static class AbstractController implements Controller {

    @Override
    public void unmappedMethod() {
    }

    @Override
    public void delete() {
    }
  }

  // MUST be public.
  public static class ActualController extends AbstractController {

    @Override
    public void update() {
    }
  }

}
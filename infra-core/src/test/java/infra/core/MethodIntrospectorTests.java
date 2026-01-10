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

package infra.core;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;

import infra.core.annotation.MergedAnnotations;
import infra.util.ReflectionUtils;

import static infra.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;
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
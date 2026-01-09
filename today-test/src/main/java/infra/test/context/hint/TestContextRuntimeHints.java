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

package infra.test.context.hint;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeHint;
import infra.aot.hint.TypeReference;
import infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import infra.test.context.support.DefaultBootstrapContext;
import infra.test.context.web.WebAppConfiguration;
import infra.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes types and annotations
 * from the <em>Infra TestContext Framework</em> available at runtime.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StandardTestRuntimeHints
 * @since 4.0
 */
class TestContextRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints runtimeHints, ClassLoader classLoader) {
    boolean servletPresent = ClassUtils.isPresent("infra.mock.api.MockApi", classLoader);
    boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure", classLoader);

    ReflectionHints reflectionHints = runtimeHints.reflection();

    // Loaded reflectively in BootstrapUtils
    registerPublicConstructors(reflectionHints,
            DefaultCacheAwareContextLoaderDelegate.class,
            DefaultBootstrapContext.class
    );

    if (groovyPresent) {
      registerDeclaredConstructors(reflectionHints,
              // Loaded reflectively in DelegatingSmartContextLoader
              "infra.test.context.support.GenericGroovyXmlContextLoader"
      );
      if (servletPresent) {
        registerDeclaredConstructors(reflectionHints,
                // Loaded reflectively in WebDelegatingSmartContextLoader
                "infra.test.context.web.GenericGroovyXmlWebContextLoader"
        );
      }
    }

    // Loaded reflectively in BootstrapUtils
    registerAnnotation(reflectionHints,
            WebAppConfiguration.class
    );
  }

  private static void registerPublicConstructors(ReflectionHints reflectionHints, Class<?>... types) {
    reflectionHints.registerTypes(TypeReference.listOf(types),
            TypeHint.builtWith(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
  }

  private static void registerDeclaredConstructors(ReflectionHints reflectionHints, String... classNames) {
    reflectionHints.registerTypes(listOf(classNames),
            TypeHint.builtWith(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
  }

  private static List<TypeReference> listOf(String... classNames) {
    return Arrays.stream(classNames).map(TypeReference::of).toList();
  }

  private static void registerAnnotation(ReflectionHints reflectionHints, Class<? extends Annotation> annotationType) {
    reflectionHints.registerType(annotationType, MemberCategory.INVOKE_DECLARED_METHODS);
  }

}

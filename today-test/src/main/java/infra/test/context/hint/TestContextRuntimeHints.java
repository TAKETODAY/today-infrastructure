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

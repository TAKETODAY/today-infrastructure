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

package infra.context.annotation;

import org.junit.jupiter.api.Test;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.instrument.ClassFileTransformer;
import java.util.function.Supplier;

import infra.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.instrument.classloading.ResourceOverridingShadowingClassLoader;
import infra.instrument.classloading.ShadowingClassLoader;
import infra.util.ReflectionUtils;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for @EnableLoadTimeWeaving
 *
 * @author Chris Beams
 * @since 4.0
 */
public class EnableLoadTimeWeavingTests {

  @Test
  public void enableLTW_withAjWeavingDisabled() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EnableLTWConfig_withAjWeavingDisabled.class);
    ctx.refresh();
    LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
    verifyNoInteractions(loadTimeWeaver);
  }

  @Test
  public void enableLTW_withAjWeavingAutodetect() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EnableLTWConfig_withAjWeavingAutodetect.class);
    ctx.refresh();
    LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
    // no expectations -> a class file transformer should NOT be added
    // because no META-INF/aop.xml is present on the classpath
    verifyNoInteractions(loadTimeWeaver);
  }

  @Test
  public void enableLTW_withAjWeavingEnabled() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(EnableLTWConfig_withAjWeavingEnabled.class);
    ctx.refresh();
    LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
    verify(loadTimeWeaver).addTransformer(isA(ClassFileTransformer.class));
  }

  @Configuration
  @EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.DISABLED)
  static class EnableLTWConfig_withAjWeavingDisabled implements LoadTimeWeavingConfigurer {

    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
      return mock(LoadTimeWeaver.class);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void enableLTW_withAjWeavingEnabled_weavesClassesLoadedByInstrumentableClassLoader() throws Exception {
    try (var ctx = new AnnotationConfigApplicationContext(EnableLTWConfig_withAjWeavingEnabledAndShadowingLoadTimeWeaver.class)) {
      var classLoader = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class).getInstrumentableClassLoader();
      var greeterClass = classLoader.loadClass(Greeter.class.getName());
      assertThat(greeterClass.getClassLoader()).isSameAs(classLoader);
      var greeter = (Supplier<String>) ReflectionUtils.accessibleConstructor(greeterClass).newInstance();
      assertThat(greeter.get()).isEqualTo("Advised: Hello");
    }
  }

  @Configuration
  @EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.ENABLED)
  static class EnableLTWConfig_withAjWeavingEnabledAndShadowingLoadTimeWeaver implements LoadTimeWeavingConfigurer {
    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
      return new ShadowingLoadTimeWeaver();
    }
  }

  /**
   * {@link LoadTimeWeaver} that applies registered transformers to all classes
   * loaded by its instrumentable {@link ClassLoader}.
   * <p>The instrumentable {@code ClassLoader} exposes a test-specific
   * {@code META-INF/aop.xml} file without affecting the rest of the test suite.
   */
  static class ShadowingLoadTimeWeaver implements LoadTimeWeaver {
    private final ResourceOverridingShadowingClassLoader classLoader =
            new ResourceOverridingShadowingClassLoader(getClass().getClassLoader());

    ShadowingLoadTimeWeaver() {
      this.classLoader.override("META-INF/aop.xml", "infra/context/annotation/EnableLoadTimeWeavingTests-aop.xml");
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
      this.classLoader.addTransformer(transformer);
    }

    @Override
    public ClassLoader getInstrumentableClassLoader() {
      return this.classLoader;
    }

    @Override
    public ClassLoader getThrowawayClassLoader() {
      return new ShadowingClassLoader(getClass().getClassLoader());
    }
  }

  static class Greeter implements Supplier<String> {
    @Override
    public String get() {
      return "Hello";
    }
  }

  @Aspect
  static class GreeterAspect {
    @Around("execution(String infra.context.annotation.EnableLoadTimeWeavingTests.Greeter.get())")
    public Object advise(ProceedingJoinPoint pjp) throws Throwable {
      return "Advised: " + pjp.proceed();
    }
  }

  @Configuration
  @EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.AUTODETECT)
  static class EnableLTWConfig_withAjWeavingAutodetect implements LoadTimeWeavingConfigurer {

    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
      return mock(LoadTimeWeaver.class);
    }
  }

  @Configuration
  @EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.ENABLED)
  static class EnableLTWConfig_withAjWeavingEnabled implements LoadTimeWeavingConfigurer {

    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
      return mock(LoadTimeWeaver.class);
    }
  }

}

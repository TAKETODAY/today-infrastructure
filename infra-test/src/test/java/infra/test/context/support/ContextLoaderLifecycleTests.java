/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.test.context.ContextLoadException;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.web.AnnotationConfigWebContextLoader;
import infra.test.context.web.WebMergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ContextLoaderLifecycleTests {

  private final MergedContextConfiguration configuration =
          new MergedContextConfiguration(getClass(), null, null, null, null);

  @Test
  void annotationLoaderSupportsAotProcessingAndRuntimeInitialization() throws Exception {
    AnnotationConfigContextLoader loader = new AnnotationConfigContextLoader();
    try (GenericApplicationContext context = loader.loadContextForAotProcessing(configuration, new RuntimeHints())) {
      assertThat(context.isActive()).isFalse();
    }
    try (GenericApplicationContext context = loader.loadContextForAotRuntime(configuration,
            applicationContext -> ((GenericApplicationContext) applicationContext)
                    .registerBean("message", String.class, () -> "initialized"))) {
      assertThat(context.isActive()).isTrue();
      assertThat(context.getBean("message")).isEqualTo("initialized");
    }
  }

  @Test
  void annotationLoaderWrapsInitializationAndLoadFailures() {
    AnnotationConfigContextLoader loader = new AnnotationConfigContextLoader();
    IllegalStateException failure = new IllegalStateException("initializer failed");
    assertThatExceptionOfType(ContextLoadException.class)
            .isThrownBy(() -> loader.loadContextForAotRuntime(configuration, context -> { throw failure; }))
            .withCause(failure).satisfies(ex -> ((ConfigurableApplicationContext) ex.getApplicationContext()).close());
    GenericXmlContextLoader xmlLoader = new GenericXmlContextLoader();
    MergedContextConfiguration missing = new MergedContextConfiguration(getClass(),
            new String[] { "classpath:missing-coverage-context.xml" }, null, null, xmlLoader);
    assertThatExceptionOfType(ContextLoadException.class).isThrownBy(() -> xmlLoader.loadContext(missing))
            .satisfies(ex -> {
              assertThat(ex.getCause()).hasMessageContaining("missing-coverage-context.xml");
              ((ConfigurableApplicationContext) ex.getApplicationContext()).close();
            });
    assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContextForAotRuntime(null, context -> { }));
    assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContextForAotRuntime(configuration, null));
  }

  @Test
  void webLoaderSupportsAotProcessingAndRuntimeInitialization() throws Exception {
    AnnotationConfigWebContextLoader loader = new AnnotationConfigWebContextLoader();
    WebMergedContextConfiguration web = new WebMergedContextConfiguration(configuration);
    try (ConfigurableApplicationContext context = (ConfigurableApplicationContext)
            loader.loadContextForAotProcessing(web, new RuntimeHints())) {
      assertThat(context.isActive()).isFalse();
    }
    try (GenericApplicationContext context = loader.loadContextForAotRuntime(web,
            applicationContext -> ((GenericApplicationContext) applicationContext)
                    .registerBean("message", String.class, () -> "web initialized"))) {
      assertThat(context.isActive()).isTrue();
      assertThat(context.getBean("message")).isEqualTo("web initialized");
    }
  }

  @Test
  void webLoaderRejectsNonWebConfigurationAndWrapsFailures() {
    AnnotationConfigWebContextLoader loader = new AnnotationConfigWebContextLoader();
    assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContext(configuration))
            .withMessageContaining("non-web merged context configuration");
    assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContextForAotRuntime(configuration, context -> { }))
            .withMessageContaining("non-web merged context configuration");
    assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContextForAotRuntime(null, context -> { }));
    assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContextForAotRuntime(configuration, null));
    IllegalStateException failure = new IllegalStateException("web initializer failed");
    assertThatExceptionOfType(ContextLoadException.class).isThrownBy(() -> loader.loadContextForAotRuntime(
            new WebMergedContextConfiguration(configuration), context -> { throw failure; }))
            .withCause(failure).satisfies(ex -> ((ConfigurableApplicationContext) ex.getApplicationContext()).close());
  }

}

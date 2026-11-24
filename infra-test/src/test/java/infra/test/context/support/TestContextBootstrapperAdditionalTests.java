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

import java.util.List;

import infra.beans.BeanInstantiationException;
import infra.context.ApplicationContext;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactories;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.ContextLoader;
import infra.test.context.ContextHierarchy;
import infra.test.context.TestExecutionListener;
import infra.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

class TestContextBootstrapperAdditionalTests {

  @Test
  void missingBootstrapContextAndLoaderArgumentsAreRejected() {
    DefaultTestContextBootstrapper bootstrapper = new DefaultTestContextBootstrapper();
    assertThatIllegalStateException().isThrownBy(bootstrapper::getBootstrapContext);
    assertThatIllegalArgumentException().isThrownBy(() -> bootstrapper.resolveContextLoader(null, List.of()));
    assertThatIllegalArgumentException().isThrownBy(() -> bootstrapper.resolveContextLoader(getClass(), null));
    assertThatIllegalArgumentException().isThrownBy(() -> bootstrapper.resolveExplicitContextLoaderClass(null));
    assertThat(bootstrapper.resolveExplicitContextLoaderClass(List.of())).isNull();
    assertThat(bootstrapper.resolveContextLoader(getClass(), List.of(new ContextConfigurationAttributes(getClass()))))
            .isInstanceOf(DelegatingSmartContextLoader.class);
  }

  @Test
  void unavailableListenersAreSkippedButOtherConstructorFailuresPropagate() {
    assertThat(bootstrapper(MissingListeners.class).getTestExecutionListeners()).isEmpty();
    assertThatExceptionOfType(BeanInstantiationException.class)
            .isThrownBy(() -> bootstrapper(BrokenListeners.class).getTestExecutionListeners())
            .withCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void missingCustomizerFactoriesAreSkippedAndDuplicatesRemoved() {
    assertThat(bootstrapper(MissingFactories.class).buildMergedContextConfiguration().getContextCustomizers()).isEmpty();
    assertThat(bootstrapper(DuplicateFactories.class).buildMergedContextConfiguration().getContextCustomizers()).hasSize(1);
    assertThat(bootstrapper(ReplacementFactories.class).buildMergedContextConfiguration().getContextCustomizers()).isEmpty();
  }

  @Test
  void legacyLoaderProcessesLocationsAndIgnoresAnnotatedClasses() {
    var configuration = bootstrapper(LegacyConfiguration.class).buildMergedContextConfiguration();
    assertThat(configuration.getLocations()).containsExactly("processed:test.xml");
    assertThat(configuration.getClasses()).isEmpty();
    assertThat(configuration.getContextLoader()).isInstanceOf(LegacyLoader.class);
  }

  @Test
  void unavailableResourceDefaultsWithoutCustomizersAreRejected() {
    assertThatIllegalStateException().isThrownBy(() -> bootstrapper(EmptyConfiguration.class).buildMergedContextConfiguration())
            .withMessageContaining("unable to detect defaults");
  }

  @Test
  void emptyHierarchiesAreRejectedAndEmptyLegacyLocationsRemainEmpty() {
    assertThatIllegalStateException().isThrownBy(() -> bootstrapper(EmptyHierarchy.class).buildMergedContextConfiguration());
    assertThat(bootstrapper(EmptyLegacyConfiguration.class).buildMergedContextConfiguration().getLocations()).isEmpty();
    assertThat(bootstrapper(DefaultListeners.class).getTestExecutionListeners()).isNotEmpty();
  }

  private DefaultTestContextBootstrapper bootstrapper(Class<?> testClass) {
    DefaultTestContextBootstrapper bootstrapper = new DefaultTestContextBootstrapper() {
      @Override
      protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
        return List.of();
      }
    };
    bootstrapper.setBootstrapContext(new DefaultBootstrapContext(testClass, mock(CacheAwareContextLoaderDelegate.class)));
    return bootstrapper;
  }

  @TestExecutionListeners({ MissingClassListener.class, MissingDefinitionListener.class })
  static class MissingListeners { }

  @TestExecutionListeners(BrokenListener.class)
  static class BrokenListeners { }

  @ContextCustomizerFactories({ MissingClassFactory.class, MissingDefinitionFactory.class })
  static class MissingFactories { }

  @ContextCustomizerFactories(ValidFactory.class)
  static class ParentFactories { }

  @ContextCustomizerFactories(ValidFactory.class)
  static class DuplicateFactories extends ParentFactories { }

  @ContextCustomizerFactories(inheritFactories = false, mergeMode = ContextCustomizerFactories.MergeMode.REPLACE_DEFAULTS)
  static class ReplacementFactories extends ParentFactories { }

  @ContextConfiguration(locations = "test.xml", classes = String.class, loader = LegacyLoader.class, inheritLocations = false)
  static class LegacyConfiguration { }

  @ContextConfiguration
  static class EmptyConfiguration { }

  @ContextHierarchy({})
  static class EmptyHierarchy { }

  @ContextConfiguration(loader = EmptyLegacyLoader.class, initializers = DiagnosticLoggingLifecycleTests.Initializer.class)
  static class EmptyLegacyConfiguration { }

  @TestExecutionListeners(mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
  static class DefaultListeners { }

  public static class EmptyLegacyLoader extends LegacyLoader {
    @Override
    public String[] processLocations(Class<?> testClass, String... locations) {
      return new String[0];
    }
  }

  public static class MissingClassListener implements TestExecutionListener {
    public MissingClassListener() throws ClassNotFoundException {
      throw new ClassNotFoundException("optional.Listener");
    }
  }

  public static class MissingDefinitionListener implements TestExecutionListener {
    public MissingDefinitionListener() {
      throw new NoClassDefFoundError("optional/Listener");
    }
  }

  public static class BrokenListener implements TestExecutionListener {
    public BrokenListener() {
      throw new IllegalArgumentException("broken listener");
    }
  }

  public static class MissingClassFactory implements ContextCustomizerFactory {
    public MissingClassFactory() throws ClassNotFoundException {
      throw new ClassNotFoundException("optional.Factory");
    }

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> attributes) {
      return null;
    }
  }

  public static class MissingDefinitionFactory implements ContextCustomizerFactory {
    public MissingDefinitionFactory() {
      throw new NoClassDefFoundError("optional/Factory");
    }

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> attributes) {
      return null;
    }
  }

  public static class ValidFactory implements ContextCustomizerFactory {
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> attributes) {
      return (context, configuration) -> { };
    }
  }

  public static class LegacyLoader implements ContextLoader {
    @Override
    public String[] processLocations(Class<?> testClass, String... locations) {
      return new String[] { "processed:" + locations[0] };
    }

    @Override
    public ApplicationContext loadContext(String... locations) {
      throw new UnsupportedOperationException();
    }
  }

}

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

import java.util.Set;

import infra.aot.hint.RuntimeHints;
import infra.context.ApplicationContext;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.SmartContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DelegatingContextLoaderAdditionalTests {

  private final SmartContextLoader xml = mock(SmartContextLoader.class);

  private final SmartContextLoader annotation = mock(SmartContextLoader.class);

  private final AbstractDelegatingSmartContextLoader loader = new AbstractDelegatingSmartContextLoader() {
    @Override
    protected SmartContextLoader getXmlLoader() {
      return xml;
    }

    @Override
    protected SmartContextLoader getAnnotationConfigLoader() {
      return annotation;
    }
  };

  @Test
  void rejectsMixedResourcesAndMissingConfiguration() {
    ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(getClass());
    attributes.setClasses(getClass());
    attributes.setLocations("test.xml");
    assertThatIllegalArgumentException().isThrownBy(() -> loader.processContextConfiguration(attributes))
            .withMessageContaining("locations AND classes");
    assertThatIllegalArgumentException().isThrownBy(() -> loader.processContextConfiguration(null));
    MergedContextConfiguration mixed = new MergedContextConfiguration(getClass(),
            new String[] { "test.xml" }, new Class<?>[] { getClass() }, null, loader);
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContext(mixed))
            .withMessageContaining("but not both");
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContext(
            new MergedContextConfiguration(getClass(), null, null, null, loader)))
            .withMessageContaining("Neither");
    assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContext((MergedContextConfiguration) null));
  }

  @Test
  void rejectsDefaultsDetectedByWrongLoader() {
    ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(getClass());
    willAnswer(invocation -> {
      attributes.setClasses(getClass());
      return null;
    }).given(xml).processContextConfiguration(attributes);
    assertThatIllegalStateException().isThrownBy(() -> loader.processContextConfiguration(attributes))
            .withMessageContaining("should NOT have detected default configuration classes");
  }

  @Test
  void rejectsLocationsDetectedByAnnotationLoader() {
    ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(getClass());
    willAnswer(invocation -> {
      attributes.setLocations("wrong.xml");
      return null;
    }).given(annotation).processContextConfiguration(attributes);
    assertThatIllegalStateException().isThrownBy(() -> loader.processContextConfiguration(attributes))
            .withMessageContaining("should NOT have detected default locations");
  }

  @Test
  void rejectsAmbiguousDefaults() {
    ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(getClass());
    willAnswer(invocation -> {
      attributes.setLocations("default.xml");
      return null;
    }).given(xml).processContextConfiguration(attributes);
    willAnswer(invocation -> {
      attributes.setClasses(getClass());
      return null;
    }).given(annotation).processContextConfiguration(attributes);
    assertThatIllegalStateException().isThrownBy(() -> loader.processContextConfiguration(attributes))
            .withMessageContaining("both default locations AND default configuration classes");
  }

  @Test
  void delegatesCustomizerOnlyConfigurationToAnnotationLoader() throws Exception {
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(), null, null,
            null, null, (String[]) null, null, Set.of((context, config) -> { }), loader, null, null);
    ApplicationContext expected = mock(ApplicationContext.class);
    given(annotation.loadContext(configuration)).willReturn(expected);
    assertThat(loader.loadContext(configuration)).isSameAs(expected);
    verify(annotation).loadContext(configuration);
  }

  @Test
  void aotOperationsRejectNonAotDelegates() {
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(),
            new String[] { "test.xml" }, null, null, loader);
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContextForAotProcessing(configuration, new RuntimeHints()))
            .withMessageContaining("must be an AotContextLoader");
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContextForAotRuntime(configuration, context -> { }))
            .withMessageContaining("must be an AotContextLoader");
  }

}

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

package infra.freemarker;

import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeHint;
import infra.beans.factory.aot.AotServices;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.mock.env.MockEnvironment;
import infra.ui.template.TemplateAvailabilityProvider;

import static infra.freemarker.FreeMarkerTemplateAvailabilityProvider.FreeMarkerTemplateAvailabilityProperties;
import static infra.freemarker.FreeMarkerTemplateAvailabilityProvider.Hints;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/4/27 15:41
 */
class FreeMarkerTemplateAvailabilityProviderTests {

  private final TemplateAvailabilityProvider provider = new FreeMarkerTemplateAvailabilityProvider();

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final MockEnvironment environment = new MockEnvironment();

  @Test
  void availabilityOfTemplateInDefaultLocation() {
    assertThat(this.provider.isTemplateAvailable("home", this.environment, getClass().getClassLoader(),
            this.resourceLoader))
            .isTrue();
  }

  @Test
  void availabilityOfTemplateThatDoesNotExist() {
    assertThat(this.provider.isTemplateAvailable("whatever", this.environment, getClass().getClassLoader(),
            this.resourceLoader))
            .isFalse();
  }

  @Test
  void availabilityOfTemplateWithCustomLoaderPath() {
    this.environment.setProperty("freemarker.template-loader-path", "classpath:/custom-templates/");
    assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
            this.resourceLoader))
            .isTrue();
  }

  @Test
  void availabilityOfTemplateWithCustomLoaderPathConfiguredAsAList() {
    this.environment.setProperty("freemarker.template-loader-path[0]", "classpath:/custom-templates/");
    assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
            this.resourceLoader))
            .isTrue();
  }

  @Test
  void availabilityOfTemplateWithCustomPrefix() {
    this.environment.setProperty("freemarker.prefix", "prefix/");
    assertThat(this.provider.isTemplateAvailable("prefixed", this.environment, getClass().getClassLoader(),
            this.resourceLoader))
            .isTrue();
  }

  @Test
  void availabilityOfTemplateWithCustomSuffix() {
    this.environment.setProperty("freemarker.suffix", ".freemarker");
    assertThat(this.provider.isTemplateAvailable("suffixed", this.environment, getClass().getClassLoader(),
            this.resourceLoader))
            .isTrue();
  }

  @Test
  void shouldRegisterFreeMarkerTemplateAvailabilityPropertiesRuntimeHints() {
    assertThat(AotServices.factories().load(RuntimeHintsRegistrar.class))
            .hasAtLeastOneElementOfType(Hints.class);
    RuntimeHints hints = new RuntimeHints();
    new Hints().registerHints(hints, getClass().getClassLoader());
    TypeHint typeHint = hints.reflection().getTypeHint(FreeMarkerTemplateAvailabilityProperties.class);
    assertThat(typeHint).isNotNull();
  }

}
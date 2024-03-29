/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.freemarker;

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.template.TemplateAvailabilityProvider;
import cn.taketoday.mock.env.MockEnvironment;

import static cn.taketoday.annotation.config.freemarker.FreeMarkerTemplateAvailabilityProvider.FreeMarkerTemplateAvailabilityProperties;
import static cn.taketoday.annotation.config.freemarker.FreeMarkerTemplateAvailabilityProvider.Hints;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/4 18:07
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
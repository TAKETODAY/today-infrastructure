/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.template.TemplateAvailabilityProvider;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerTemplateAvailabilityProvider}.
 *
 * @author Andy Wilkinson
 */
class FreeMarkerTemplateAvailabilityProviderTests {

  private final TemplateAvailabilityProvider provider = new FreeMarkerTemplateAvailabilityProvider();

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final MockEnvironment environment = new MockEnvironment();

  @Test
  void availabilityOfTemplateInDefaultLocation() {
    assertThat(this.provider.isTemplateAvailable("home", this.environment, getClass().getClassLoader(),
            this.resourceLoader)).isTrue();
  }

  @Test
  void availabilityOfTemplateThatDoesNotExist() {
    assertThat(this.provider.isTemplateAvailable("whatever", this.environment, getClass().getClassLoader(),
            this.resourceLoader)).isFalse();
  }

  @Test
  void availabilityOfTemplateWithCustomLoaderPath() {
    this.environment.setProperty("infra.freemarker.template-loader-path", "classpath:/custom-templates/");
    assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
            this.resourceLoader)).isTrue();
  }

  @Test
  void availabilityOfTemplateWithCustomLoaderPathConfiguredAsAList() {
    this.environment.setProperty("infra.freemarker.template-loader-path[0]", "classpath:/custom-templates/");
    assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
            this.resourceLoader)).isTrue();
  }

  @Test
  void availabilityOfTemplateWithCustomPrefix() {
    this.environment.setProperty("infra.freemarker.prefix", "prefix/");
    assertThat(this.provider.isTemplateAvailable("prefixed", this.environment, getClass().getClassLoader(),
            this.resourceLoader)).isTrue();
  }

  @Test
  void availabilityOfTemplateWithCustomSuffix() {
    this.environment.setProperty("infra.freemarker.suffix", ".freemarker");
    assertThat(this.provider.isTemplateAvailable("suffixed", this.environment, getClass().getClassLoader(),
            this.resourceLoader)).isTrue();
  }

}

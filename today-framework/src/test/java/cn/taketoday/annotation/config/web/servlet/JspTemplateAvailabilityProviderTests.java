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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/14 17:17
 */
class JspTemplateAvailabilityProviderTests {

  private final MockEnvironment environment = new MockEnvironment();
  private final ResourceLoader resourceLoader = new DefaultResourceLoader();
  private final JspTemplateAvailabilityProvider provider = new JspTemplateAvailabilityProvider();

  @Test
  void availabilityOfTemplateThatDoesNotExist() {
    assertThat(isTemplateAvailable("whatever")).isFalse();
  }

  @Test
  void availabilityOfTemplateWithCustomPrefix() {
    environment.setProperty("web.mvc.view.prefix", "classpath:/custom-templates/");
    assertThat(isTemplateAvailable("custom.jsp")).isTrue();
  }

  @Test
  void availabilityOfTemplateWithCustomSuffix() {
    environment.setProperty("web.mvc.view.prefix", "classpath:/custom-templates/");
    environment.setProperty("web.mvc.view.suffix", ".jsp");
    assertThat(isTemplateAvailable("suffixed")).isTrue();
  }

  private boolean isTemplateAvailable(String view) {
    return provider.isTemplateAvailable(view, environment,
            getClass().getClassLoader(), resourceLoader);
  }

}
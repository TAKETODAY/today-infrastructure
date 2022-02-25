/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.view.groovy;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import cn.taketoday.beans.DirectFieldAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for
 * {@link cn.taketoday.web.view.groovy.GroovyMarkupViewResolver}.
 *
 * @author Brian Clozel
 */
public class GroovyMarkupViewResolverTests {

  @Test
  public void viewClass() throws Exception {
    GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
    assertThat(resolver.requiredViewClass()).isEqualTo(GroovyMarkupView.class);
    DirectFieldAccessor viewAccessor = new DirectFieldAccessor(resolver);
    Class<?> viewClass = (Class<?>) viewAccessor.getPropertyValue("viewClass");
    assertThat(viewClass).isEqualTo(GroovyMarkupView.class);
  }

  @Test
  public void cacheKey() throws Exception {
    GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
    String cacheKey = (String) resolver.getCacheKey("test", Locale.US);
    assertThat(cacheKey).isNotNull();
    assertThat(cacheKey).isEqualTo("test_en_US");
  }

}

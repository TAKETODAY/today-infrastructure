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

package cn.taketoday.framework.web.reactive.result.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import cn.taketoday.context.support.GenericApplicationContext;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Brian Clozel
 */
class MustacheViewResolverTests {

  private final String prefix = "classpath:/" + getClass().getPackage().getName().replace(".", "/") + "/";

  private MustacheViewResolver resolver = new MustacheViewResolver();

  @BeforeEach
  void init() {
    GenericApplicationContext applicationContext = new GenericApplicationContext();
    applicationContext.refresh();
    this.resolver.setApplicationContext(applicationContext);
    this.resolver.setPrefix(this.prefix);
    this.resolver.setSuffix(".html");
  }

  @Test
  void resolveNonExistent() {
    assertThat(this.resolver.resolveViewName("bar", null).block(Duration.ofSeconds(30))).isNull();
  }

  @Test
  void resolveExisting() {
    assertThat(this.resolver.resolveViewName("template", null).block(Duration.ofSeconds(30))).isNotNull();
  }

}

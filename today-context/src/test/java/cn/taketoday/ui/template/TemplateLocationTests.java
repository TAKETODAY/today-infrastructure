/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.ui.template;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/4/27 16:50
 */
class TemplateLocationTests {

  private final PatternResourceLoader resourceLoader = PatternResourceLoader.fromResourceLoader(new DefaultResourceLoader());

  @Test
  void illegalArgument() {
    assertThatThrownBy(() -> new TemplateLocation(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Path is required");
  }

  @Test
  void exists() {
    assertThat(new TemplateLocation("classpath:not-found").exists(resourceLoader)).isFalse();
    assertThat(new TemplateLocation("classpath:do_not_delete_me.txt").exists(resourceLoader)).isTrue();

    assertThat(new TemplateLocation("classpath*:not-found").exists(resourceLoader)).isFalse();
    assertThat(new TemplateLocation("classpath*:cn/taketoday/lang/Version**").exists(resourceLoader)).isTrue();
  }

  @Test
  void toString_() {
    assertThat(new TemplateLocation("classpath:not-found").toString()).isEqualTo("classpath:not-found");
  }

}
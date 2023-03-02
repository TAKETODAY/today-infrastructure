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

package cn.taketoday.session;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.session.config.EnableWebSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/28 22:22
 */
class FileSessionPersisterTests {

  @Test
  void illegalArgument() {
    assertThatThrownBy(() ->
            new FileSessionPersister(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SessionRepository is required");

  }

  @Test
  void test() {
    var context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("server.session", Map.of("server.session.persistent", true))
    );
    context.register(Config.class);
    context.refresh();

    assertThat(context.containsBeanDefinition(PersistenceSessionRepository.class)).isTrue();
    context.close();
  }

  @Configuration
  @EnableWebSession
  static class Config {

  }

}
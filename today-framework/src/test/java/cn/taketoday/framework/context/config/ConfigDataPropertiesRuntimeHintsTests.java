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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.framework.context.config.ConfigDataProperties.Activate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:36
 */
class ConfigDataPropertiesRuntimeHintsTests {

  @Test
  void shouldRegisterHints() {
    RuntimeHints hints = new RuntimeHints();
    new ConfigDataPropertiesRuntimeHints().registerHints(hints, getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.reflection().onType(ConfigDataProperties.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onType(ConfigDataLocation.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onType(Activate.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onMethod(ConfigDataLocation.class, "valueOf")).accepts(hints);
  }

}
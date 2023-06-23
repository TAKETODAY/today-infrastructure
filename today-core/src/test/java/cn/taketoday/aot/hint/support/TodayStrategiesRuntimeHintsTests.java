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

package cn.taketoday.aot.hint.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.lang.DummyFactory;
import cn.taketoday.lang.MyDummyFactory1;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TodayStrategiesRuntimeHints}.
 *
 * @author Phillip Webb
 */
class TodayStrategiesRuntimeHintsTests {

  private RuntimeHints hints;

  @BeforeEach
  void setup() {
    this.hints = new RuntimeHints();
    TodayStrategies.forLocation("META-INF/config/aot.factories")
            .load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
                    .registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void resourceLocationHasHints() {
    assertThat(RuntimeHintsPredicates.resource().forResource(TodayStrategies.STRATEGIES_LOCATION)).accepts(this.hints);
  }

  @Test
  void factoryTypeHasHint() {
    assertThat(RuntimeHintsPredicates.reflection().onType(DummyFactory.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

  @Test
  void factoryImplementationHasHint() {
    assertThat(RuntimeHintsPredicates.reflection().onType(MyDummyFactory1.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

}

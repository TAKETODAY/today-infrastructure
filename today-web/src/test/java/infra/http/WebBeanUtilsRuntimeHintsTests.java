/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Web-related tests for {@link infra.beans.BeanUtilsRuntimeHints}.
 *
 * @author Sebastien Deleuze
 * @see infra.beans.BeanUtilsRuntimeHintsTests
 * @since 4.0
 */
class WebBeanUtilsRuntimeHintsTests {

  private final RuntimeHints hints = new RuntimeHints();

  @BeforeEach
  void setup() {
    TodayStrategies.forResourceLocation("META-INF/config/aot.factories")
            .load(RuntimeHintsRegistrar.class)
            .forEach(registrar -> registrar.registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void mediaTypeEditorHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(MediaTypeEditor.class)
            .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

}

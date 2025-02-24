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

package infra.format.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/22 19:57
 */
class FormattingConversionServiceRuntimeHintsTests {

  private RuntimeHints hints;

  @BeforeEach
  void setup() {
    this.hints = new RuntimeHints();
    TodayStrategies.forResourceLocation("META-INF/config/aot.factories")
            .load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
                    .registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void montearyAmountHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(javax.money.MonetaryAmount.class)).accepts(this.hints);
  }

}
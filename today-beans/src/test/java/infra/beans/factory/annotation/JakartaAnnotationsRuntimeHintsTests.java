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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.beans.factory.aot.AotServices;
import infra.util.ClassUtils;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 20:09
 */
class JakartaAnnotationsRuntimeHintsTests {

  private final RuntimeHints hints = new RuntimeHints();

  @BeforeEach
  void setup() {
    AotServices.factories().load(RuntimeHintsRegistrar.class)
            .forEach(registrar -> registrar.registerHints(this.hints,
                    ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void jakartaInjectAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Inject.class)).accepts(this.hints);
  }

  @Test
  void jakartaProviderAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Provider.class)).accepts(this.hints);
  }

  @Test
  void jakartaQualifierAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Qualifier.class)).accepts(this.hints);
  }

}
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

import java.util.function.Predicate;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.framework.test.context.FilteredClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/4 18:06
 */
class TemplateRuntimeHintsTests {

  private static final Predicate<RuntimeHints> TEST_PREDICATE = RuntimeHintsPredicates.resource()
          .forResource("templates/something/hello.html");

  @Test
  void templateRuntimeHintsIsRegistered() {
    Iterable<RuntimeHintsRegistrar> registrar = AotServices.factories().load(RuntimeHintsRegistrar.class);
    assertThat(registrar).anyMatch(TemplateRuntimeHints.class::isInstance);
  }

  @Test
  void contributeWhenTemplateLocationExists() {
    RuntimeHints runtimeHints = contribute(getClass().getClassLoader());
    assertThat(TEST_PREDICATE.test(runtimeHints)).isTrue();
  }

  @Test
  void contributeWhenTemplateLocationDoesNotExist() {
    FilteredClassLoader classLoader = new FilteredClassLoader(new ClassPathResource("templates"));
    RuntimeHints runtimeHints = contribute(classLoader);
    assertThat(TEST_PREDICATE.test(runtimeHints)).isFalse();
  }

  private RuntimeHints contribute(ClassLoader classLoader) {
    RuntimeHints runtimeHints = new RuntimeHints();
    new TemplateRuntimeHints().registerHints(runtimeHints, classLoader);
    return runtimeHints;
  }

}
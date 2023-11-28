/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for component scanning
 * {@link ControllerAdvice} and {@link RestControllerAdvice} beans.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class ComponentScannedControllerAdviceTests {

  @Test
  void scannedAdviceHasCustomName() {
    String basePackage = getClass().getPackageName() + ".scanned";
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(basePackage)) {
      assertThat(context.getBean("myControllerAdvice")).isNotNull();
      assertThat(context.getBean("myRestControllerAdvice")).isNotNull();
    }
  }

}

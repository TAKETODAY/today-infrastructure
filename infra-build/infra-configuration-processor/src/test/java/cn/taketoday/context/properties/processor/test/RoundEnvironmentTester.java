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

package cn.taketoday.context.properties.processor.test;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * A tester utility for {@link RoundEnvironment}.
 *
 * @author Stephane Nicoll
 */
public class RoundEnvironmentTester {

  private final RoundEnvironment roundEnvironment;

  RoundEnvironmentTester(RoundEnvironment roundEnvironment) {
    this.roundEnvironment = roundEnvironment;
  }

  /**
   * Return the root {@link TypeElement} for the specified {@code type}.
   *
   * @param type the type of the class
   * @return the {@link TypeElement}
   */
  public TypeElement getRootElement(Class<?> type) {
    return (TypeElement) this.roundEnvironment.getRootElements()
            .stream()
            .filter((element) -> element.toString().equals(type.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "No element found for " + type + " make sure it is included in the list of classes to compile"));
  }

}

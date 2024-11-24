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

package infra.test.context.junit.jupiter.defaultmethods;

import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.comics.Cat;

/**
 * Parameterized test class for integration tests that demonstrate support for
 * interface default methods and Java generics in JUnit Jupiter test classes when used
 * with the TestContext Framework and the {@link InfraExtension}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class CatInterfaceDefaultMethodsTests implements GenericComicCharactersInterfaceDefaultMethodsTests<Cat> {

  @Override
  public int getExpectedNumCharacters() {
    return 2;
  }

  @Override
  public String getExpectedName() {
    return "Catbert";
  }

}

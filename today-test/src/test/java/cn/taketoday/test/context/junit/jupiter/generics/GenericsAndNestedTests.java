/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context.junit.jupiter.generics;

import org.junit.jupiter.api.Nested;

import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.comics.Cat;
import cn.taketoday.test.context.junit.jupiter.comics.Dog;

/**
 * Integration tests that verify support for Java generics combined with {@code @Nested}
 * test classes when used with the Spring TestContext Framework and the
 * {@link ApplicationExtension}.
 *
 * <p>
 * To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class GenericsAndNestedTests {

  @Nested
  class CatTests extends GenericComicCharactersTests<Cat> {

    @Override
    int getExpectedNumCharacters() {
      return 2;
    }

    @Override
    String getExpectedName() {
      return "Catbert";
    }
  }

  @Nested
  class DogTests extends GenericComicCharactersTests<Dog> {

    @Override
    int getExpectedNumCharacters() {
      return 1;
    }

    @Override
    String getExpectedName() {
      return "Dogbert";
    }
  }

}

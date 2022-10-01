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

import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.junit.jupiter.comics.Cat;

/**
 * Parameterized test class for integration tests that demonstrate support for
 * Java generics in JUnit Jupiter test classes when used with the Spring TestContext
 * Framework and the {@link InfraExtension}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
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

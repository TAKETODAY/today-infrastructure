/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.junit.jupiter.generics;

import org.junit.jupiter.api.Nested;

import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.comics.Cat;
import infra.test.context.junit.jupiter.comics.Dog;

/**
 * Integration tests that verify support for Java generics combined with {@code @Nested}
 * test classes when used with the TestContext Framework and the
 * {@link InfraExtension}.
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

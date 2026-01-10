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

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.junit.jupiter.TestConfig;
import infra.test.context.junit.jupiter.comics.Character;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests that demonstrate support for
 * Java generics in JUnit Jupiter test classes when used with the Infra TestContext
 * Framework and the {@link InfraExtension}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(TestConfig.class)
abstract class GenericComicCharactersTests<T extends Character> {

  @Autowired
  T character;

  @Autowired
  List<T> characters;

  @Test
  void autowiredFields() {
    assertThat(this.character).as("Character should have been @Autowired by Spring").isNotNull();
    assertThat(this.character).as("character's name").extracting(Character::getName).isEqualTo(getExpectedName());
    assertThat(this.characters).as("Number of characters in context").hasSize(getExpectedNumCharacters());
  }

  @Test
  void autowiredParameterByTypeForSingleGenericBean(@Autowired T character) {
    assertThat(character).as("Character should have been @Autowired by Spring").isNotNull();
    assertThat(this.character).as("character's name").extracting(Character::getName).isEqualTo(getExpectedName());
  }

  abstract int getExpectedNumCharacters();

  abstract String getExpectedName();

}

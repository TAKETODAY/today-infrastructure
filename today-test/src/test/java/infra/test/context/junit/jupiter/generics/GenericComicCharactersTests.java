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

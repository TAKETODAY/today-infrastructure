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

package cn.taketoday.test.context.junit.jupiter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.junit.jupiter.comics.Cat;
import cn.taketoday.test.context.junit.jupiter.comics.Dog;
import cn.taketoday.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate that the TestContext Framework
 * can be used with JUnit Jupiter's {@link ParameterizedTest @ParameterizedTest}
 * support in conjunction with the {@link InfraExtension}.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the
 * JUnit Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @see InfraExtension
 * @see ParameterizedTest
 * @since 4.0
 */
@JUnitConfig(TestConfig.class)
class InfraExtensionParameterizedTests {

  @ParameterizedTest
  @ValueSource(strings = { "Dilbert", "Wally" })
  void people(String name, @Autowired List<Person> people) {
    assertThat(people.stream().map(Person::getName).filter(name::equals)).hasSize(1);
  }

  @ParameterizedTest
  @CsvSource("dogbert, Dogbert")
  void dogs(String beanName, String dogName, ApplicationContext context) {
    assertThat(context.getBean(beanName, Dog.class)).extracting(Dog::getName).isEqualTo(dogName);
  }

  @ParameterizedTest
  @CsvSource({ "garfield, Garfield", "catbert, Catbert" })
  void cats(String beanName, String catName, ApplicationContext context) {
    assertThat(context.getBean(beanName, Cat.class)).extracting(Cat::getName).isEqualTo(catName);
  }

}

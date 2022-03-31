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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;
import cn.taketoday.test.context.junit.jupiter.comics.Dog;
import cn.taketoday.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate support for autowiring individual
 * parameters in test class constructors using {@link Autowired @Autowired}
 * and {@link Value @Value} with the Spring TestContext Framework and JUnit Jupiter.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @see ApplicationExtension
 * @see SpringJUnitJupiterAutowiredConstructorInjectionTests
 * @since 5.0
 */
@JUnitConfig(TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class SpringJUnitJupiterConstructorInjectionTests {

  final ApplicationContext applicationContext;
  final Person dilbert;
  final Dog dog;
  final Integer enigma;
  final TestInfo testInfo;

  SpringJUnitJupiterConstructorInjectionTests(ApplicationContext applicationContext, @Autowired Person dilbert,
          @Autowired Dog dog, @Value("${enigma}") Integer enigma, TestInfo testInfo) {

    this.applicationContext = applicationContext;
    this.dilbert = dilbert;
    this.dog = dog;
    this.enigma = enigma;
    this.testInfo = testInfo;
  }

  @Test
  void applicationContextInjected() {
    assertThat(applicationContext).as("ApplicationContext should have been injected by Spring").isNotNull();
    assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(this.dilbert);
  }

  @Test
  void beansInjected() {
    assertThat(this.dilbert).as("Dilbert should have been @Autowired by Spring").isNotNull();
    assertThat(this.dilbert.getName()).as("Person's name").isEqualTo("Dilbert");

    assertThat(this.dog).as("Dogbert should have been @Autowired by Spring").isNotNull();
    assertThat(this.dog.getName()).as("Dog's name").isEqualTo("Dogbert");
  }

  @Test
  void propertyPlaceholderInjected() {
    assertThat(this.enigma).as("Enigma should have been injected via @Value by Spring").isNotNull();
    assertThat(this.enigma).as("enigma").isEqualTo(42);
  }

  @Test
  void testInfoInjected() {
    assertThat(this.testInfo).as("TestInfo should have been injected by JUnit").isNotNull();
  }

}

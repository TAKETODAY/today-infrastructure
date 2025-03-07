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

package infra.test.context.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.comics.Dog;
import infra.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate support for autowiring individual
 * parameters in test class constructors using {@link Autowired @Autowired}
 * and {@link Value @Value} with the TestContext Framework and JUnit Jupiter.
 *
 * @author Sam Brannen
 * @see InfraExtension
 * @see ApplicationJUnitJupiterAutowiredConstructorInjectionTests
 * @since 4.0
 */
@JUnitConfig(TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class JUnitJupiterConstructorInjectionTests {

  final ApplicationContext applicationContext;
  final Person dilbert;
  final Dog dog;
  final Integer enigma;
  final TestInfo testInfo;

  JUnitJupiterConstructorInjectionTests(ApplicationContext applicationContext, @Autowired Person dilbert,
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

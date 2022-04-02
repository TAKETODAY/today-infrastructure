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
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.comics.Cat;
import cn.taketoday.test.context.junit.jupiter.comics.Dog;
import cn.taketoday.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate that the Spring TestContext Framework can be used
 * with JUnit Jupiter by registering the {@link ApplicationExtension} via a static field.
 * Note, however, that this is not the recommended way to register the {@code ApplicationExtension}.
 *
 * <p>
 * To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @see SpringExtensionTests
 * @see ApplicationExtension
 * @see RegisterExtension
 * @since 4.0
 */
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
class RegisterExtensionSpringExtensionTests {

  @RegisterExtension
  static final ApplicationExtension springExtension = new ApplicationExtension();

  @Autowired
  Person dilbert;

  @Autowired
  List<Person> people;

  @Autowired
  Dog dog;

  @Autowired
  Cat cat;

  @Autowired
  List<Cat> cats;

  @Value("${enigma}")
  Integer enigma;

  @Test
  void applicationContextInjectedIntoMethod(ApplicationContext applicationContext) {
    assertThat(applicationContext).as("ApplicationContext should have been injected by Spring").isNotNull();
    assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(this.dilbert);
  }

  @Test
  void genericApplicationContextInjectedIntoMethod(GenericApplicationContext applicationContext) {
    assertThat(applicationContext).as("GenericApplicationContext should have been injected by Spring").isNotNull();
    assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(this.dilbert);
  }

  @Test
  void autowiredFields() {
    assertThat(this.dilbert).as("Dilbert should have been @Autowired by Spring").isNotNull();
    assertThat(this.dilbert.getName()).as("Person's name").isEqualTo("Dilbert");
    assertThat(this.people).as("Number of people in context").hasSize(2);

    assertThat(this.dog).as("Dogbert should have been @Autowired by Spring").isNotNull();
    assertThat(this.dog.getName()).as("Dog's name").isEqualTo("Dogbert");

    assertThat(this.cat).as("Catbert should have been @Autowired by Spring as the @Primary cat").isNotNull();
    assertThat(this.cat.getName()).as("Primary cat's name").isEqualTo("Catbert");
    assertThat(this.cats).as("Number of cats in context").hasSize(2);

    assertThat(this.enigma).as("Enigma should have been injected via @Value by Spring").isNotNull();
    assertThat(this.enigma).as("enigma").isEqualTo(42);
  }

  @Test
  void autowiredParameterByTypeForSingleBean(@Autowired Dog dog) {
    assertThat(dog).as("Dogbert should have been @Autowired by Spring").isNotNull();
    assertThat(dog.getName()).as("Dog's name").isEqualTo("Dogbert");
  }

  @Test
  void autowiredParameterByTypeForPrimaryBean(@Autowired Cat primaryCat) {
    assertThat(primaryCat).as("Primary cat should have been @Autowired by Spring").isNotNull();
    assertThat(primaryCat.getName()).as("Primary cat's name").isEqualTo("Catbert");
  }

  @Test
  void autowiredParameterWithExplicitQualifier(@Qualifier("wally") Person person) {
    assertThat(person).as("Wally should have been @Autowired by Spring").isNotNull();
    assertThat(person.getName()).as("Person's name").isEqualTo("Wally");
  }

  /**
   * NOTE: Test code must be compiled with "-g" (debug symbols) or "-parameters" in
   * order for the parameter name to be used as the qualifier; otherwise, use
   * {@code @Qualifier("wally")}.
   */
  @Test
  void autowiredParameterWithImplicitQualifierBasedOnParameterName(@Autowired Person wally) {
    assertThat(wally).as("Wally should have been @Autowired by Spring").isNotNull();
    assertThat(wally.getName()).as("Person's name").isEqualTo("Wally");
  }

  @Test
  void autowiredParameterAsJavaUtilOptional(@Autowired Optional<Dog> dog) {
    assertThat(dog).as("Optional dog should have been @Autowired by Spring").isNotNull();
    assertThat(dog.isPresent()).as("Value of Optional should be 'present'").isTrue();
    assertThat(dog.get().getName()).as("Dog's name").isEqualTo("Dogbert");
  }

  @Test
  void autowiredParameterThatDoesNotExistAsJavaUtilOptional(@Autowired Optional<Number> number) {
    assertThat(number).as("Optional number should have been @Autowired by Spring").isNotNull();
    assertThat(number).as("Value of Optional number should not be 'present'").isNotPresent();
  }

  @Test
  void autowiredParameterThatDoesNotExistButIsNotRequired(@Autowired(required = false) Number number) {
    assertThat(number).as("Non-required number should have been @Autowired as 'null' by Spring").isNull();
  }

  @Test
  void autowiredParameterOfList(@Autowired List<Person> peopleParam) {
    assertThat(peopleParam).as("list of people should have been @Autowired by Spring").isNotNull();
    assertThat(peopleParam).as("Number of people in context").hasSize(2);
  }

  @Test
  void valueParameterWithPrimitiveType(@Value("99") int num) {
    assertThat(num).isEqualTo(99);
  }

  @Test
  void valueParameterFromPropertyPlaceholder(@Value("${enigma}") Integer enigmaParam) {
    assertThat(enigmaParam).as("Enigma should have been injected via @Value by Spring").isNotNull();
    assertThat(enigmaParam).as("enigma").isEqualTo(42);
  }

  @Test
  void valueParameterFromDefaultValueForPropertyPlaceholder(@Value("${bogus:false}") Boolean defaultValue) {
    assertThat(defaultValue).as("Default value should have been injected via @Value by Spring").isNotNull();
    assertThat(defaultValue).as("default value").isFalse();
  }

  @Test
  void valueParameterFromSpelExpression(@Value("#{@dilbert.name}") String name) {
    assertThat(name).as(
            "Dilbert's name should have been injected via SpEL expression in @Value by Spring").isNotNull();
    assertThat(name).as("name from SpEL expression").isEqualTo("Dilbert");
  }

  @Test
  void valueParameterFromSpelExpressionWithNestedPropertyPlaceholder(@Value("#{'Hello ' + ${enigma}}") String hello) {
    assertThat(hello).as("hello should have been injected via SpEL expression in @Value by Spring").isNotNull();
    assertThat(hello).as("hello from SpEL expression").isEqualTo("Hello 42");
  }

  @Test
  void junitAndSpringMethodInjectionCombined(@Autowired Cat kittyCat, TestInfo testInfo, ApplicationContext context,
          TestReporter testReporter) {

    assertThat(testInfo).as("TestInfo should have been injected by JUnit").isNotNull();
    assertThat(testReporter).as("TestReporter should have been injected by JUnit").isNotNull();

    assertThat(context).as("ApplicationContext should have been injected by Spring").isNotNull();
    assertThat(kittyCat).as("Cat should have been @Autowired by Spring").isNotNull();
  }

}

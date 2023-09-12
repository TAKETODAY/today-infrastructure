/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.comics.Dog;
import cn.taketoday.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate support for {@link Autowired @Autowired}
 * test class constructors with the TestContext Framework and JUnit Jupiter.
 *
 * @author Sam Brannen
 * @see InfraExtension
 * @see JUnitJupiterConstructorInjectionTests
 * @since 4.0
 */
class ApplicationJUnitJupiterAutowiredConstructorInjectionTests {

  @Nested
  class InfraAutowiredTests extends BaseClass {

    @Autowired
    InfraAutowiredTests(ApplicationContext context, Person dilbert, Dog dog, @Value("${enigma}") Integer enigma) {
      super(context, dilbert, dog, enigma);
    }
  }

  @Nested
  class JakartaInjectTests extends BaseClass {

    @jakarta.inject.Inject
    JakartaInjectTests(ApplicationContext context, Person dilbert, Dog dog, @Value("${enigma}") Integer enigma) {
      super(context, dilbert, dog, enigma);
    }
  }

  @Nested
  class JavaxInjectTests extends BaseClass {

    @javax.inject.Inject
    JavaxInjectTests(ApplicationContext context, Person dilbert, Dog dog, @Value("${enigma}") Integer enigma) {
      super(context, dilbert, dog, enigma);
    }
  }

  @JUnitConfig(TestConfig.class)
  @TestPropertySource(properties = "enigma = 42")
  private static abstract class BaseClass {

    final ApplicationContext context;
    final Person dilbert;
    final Dog dog;
    final Integer enigma;

    BaseClass(ApplicationContext context, Person dilbert, Dog dog, Integer enigma) {
      this.context = context;
      this.dilbert = dilbert;
      this.dog = dog;
      this.enigma = enigma;
    }

    @Test
    void applicationContextInjected() {
      assertThat(context).as("ApplicationContext should have been injected").isNotNull();
      assertThat(context.getBean("dilbert", Person.class)).isEqualTo(this.dilbert);
    }

    @Test
    void beansInjected() {
      assertThat(this.dilbert).as("Dilbert should have been injected").isNotNull();
      assertThat(this.dilbert.getName()).as("Person's name").isEqualTo("Dilbert");

      assertThat(this.dog).as("Dogbert should have been injected").isNotNull();
      assertThat(this.dog.getName()).as("Dog's name").isEqualTo("Dogbert");
    }

    @Test
    void propertyPlaceholderInjected() {
      assertThat(this.enigma).as("Enigma should have been injected via @Value").isEqualTo(42);
    }

  }

}

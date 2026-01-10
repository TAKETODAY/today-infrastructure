/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.comics.Dog;
import infra.test.context.junit.jupiter.comics.Person;

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

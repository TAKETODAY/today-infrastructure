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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.test.context.junit.jupiter.comics.Cat;
import infra.test.context.junit.jupiter.comics.Dog;
import infra.test.context.junit.jupiter.comics.Person;
import infra.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate that the TestContext Framework
 * can be used with JUnit Jupiter's {@link ParameterizedTest @ParameterizedTest}
 * support in conjunction with the {@link InfraExtension}.
 *
 * @author Sam Brannen
 * @see InfraExtension
 * @see ParameterizedTest
 * @since 4.0
 */
@JUnitConfig(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
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

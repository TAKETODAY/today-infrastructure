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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which demonstrate the composability of annotations from
 * JUnit Jupiter and the TestContext Framework.
 *
 * <p>Note that {@link JUnitConfig @JUnitConfig} is meta-annotated
 * with JUnit Jupiter's {@link ExtendWith @ExtendWith} <b>and</b> Infra
 * {@link ContextConfiguration @ContextConfiguration}.
 *
 * @author Sam Brannen
 * @see InfraExtension
 * @see JUnitConfig
 * @see InfraExtensionTests
 * @since 4.0
 */
@JUnitConfig(TestConfig.class)
@DisplayName("@JUnitConfig Tests")
class ComposedInfraExtensionTests {

  @Autowired
  Person dilbert;

  @Autowired
  List<Person> people;

  @Test
  @DisplayName("ApplicationContext injected into method")
  void applicationContextInjected(ApplicationContext applicationContext) {
    assertThat(applicationContext).as("ApplicationContext should have been injected into method by Spring").isNotNull();
    assertThat(applicationContext.getBean("dilbert", Person.class)).isEqualTo(dilbert);
  }

  @Test
  @DisplayName("@Beans injected into fields")
  void infraBeansInjected() {
    assertThat(dilbert).as("Person should have been @Autowired by Spring").isNotNull();
    assertThat(dilbert.getName()).as("Person's name").isEqualTo("Dilbert");
    assertThat(people).as("Number of Person objects in context").hasSize(2);
  }

}

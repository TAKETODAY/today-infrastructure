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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.comics.Person;

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
  void springBeansInjected() {
    assertThat(dilbert).as("Person should have been @Autowired by Spring").isNotNull();
    assertThat(dilbert.getName()).as("Person's name").isEqualTo("Dilbert");
    assertThat(people).as("Number of Person objects in context").hasSize(2);
  }

}

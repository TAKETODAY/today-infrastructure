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
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.TestConstructor;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;
import cn.taketoday.test.context.junit.jupiter.comics.Dog;
import cn.taketoday.test.context.junit.jupiter.comics.Person;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.context.TestConstructor.AutowireMode.ALL;

/**
 * Integration tests which demonstrate support for automatically
 * {@link Autowired @Autowired} test class constructors in conjunction with the
 * {@link TestConstructor @TestConstructor} annotation
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.2
 * @see ApplicationExtension
 * @see SpringJUnitJupiterAutowiredConstructorInjectionTests
 * @see SpringJUnitJupiterConstructorInjectionTests
 */
@SpringJUnitConfig(TestConfig.class)
@TestPropertySource(properties = "enigma = 42")
@TestConstructor(autowireMode = ALL)
class TestConstructorAnnotationIntegrationTests {

	final ApplicationContext applicationContext;
	final Person dilbert;
	final Dog dog;
	final Integer enigma;


	TestConstructorAnnotationIntegrationTests(ApplicationContext applicationContext, Person dilbert, Dog dog,
			@Value("${enigma}") Integer enigma) {

		this.applicationContext = applicationContext;
		this.dilbert = dilbert;
		this.dog = dog;
		this.enigma = enigma;
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

}

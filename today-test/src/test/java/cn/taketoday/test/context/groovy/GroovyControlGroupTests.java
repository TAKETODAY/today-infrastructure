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

package cn.taketoday.test.context.groovy;

import org.junit.jupiter.api.Test;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.GenericGroovyApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration test to verify the expected functionality of
 * {@link GenericGroovyApplicationContext}, thereby validating the proper
 * syntax and configuration of {@code "context.groovy"} without using the
 * Spring TestContext Framework.
 *
 * <p>In other words, this test class serves merely as a <em>control group</em>
 * to ensure that there is nothing wrong with the Groovy script used by
 * other tests in this package.
 *
 * @author Sam Brannen
 * @since 4.1
 */
class GroovyControlGroupTests {

	@Test
	@SuppressWarnings("resource")
	void verifyScriptUsingGenericGroovyApplicationContext() {
		ApplicationContext ctx = new GenericGroovyApplicationContext(getClass(), "context.groovy");

		String foo = ctx.getBean("foo", String.class);
		assertThat(foo).isEqualTo("Foo");

		String bar = ctx.getBean("bar", String.class);
		assertThat(bar).isEqualTo("Bar");

		Pet pet = ctx.getBean(Pet.class);
		assertThat(pet).as("pet").isNotNull();
		assertThat(pet.getName()).isEqualTo("Dogbert");

		Employee employee = ctx.getBean(Employee.class);
		assertThat(employee).as("employee").isNotNull();
		assertThat(employee.getName()).isEqualTo("Dilbert");
		assertThat(employee.getCompany()).isEqualTo("???");
	}

}

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
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;

import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for loading an {@code ApplicationContext} from a
 * Groovy script with the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(ApplicationExtension.class)
@ContextConfiguration("context.groovy")
class GroovySpringContextTests implements BeanNameAware, InitializingBean {

	private Employee employee;

	@Autowired
	private Pet pet;

	@Autowired(required = false)
	protected Long nonrequiredLong;

	@Resource
	protected String foo;

	protected String bar;

	@Autowired
	private ApplicationContext applicationContext;

	private String beanName;

	private boolean beanInitialized = false;


	@Autowired
	protected void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Resource
	protected void setBar(String bar) {
		this.bar = bar;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void afterPropertiesSet() {
		this.beanInitialized = true;
	}


	@Test
	void verifyBeanNameSet() {
		assertThat(this.beanName.startsWith(getClass().getName())).as("The bean name of this test instance should have been set to the fully qualified class name " +
				"due to BeanNameAware semantics.").isTrue();
	}

	@Test
	void verifyBeanInitialized() {
		assertThat(this.beanInitialized).as("This test bean should have been initialized due to InitializingBean semantics.").isTrue();
	}

	@Test
	void verifyAnnotationAutowiredFields() {
		assertThat(this.nonrequiredLong).as("The nonrequiredLong property should NOT have been autowired.").isNull();
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Dogbert");
	}

	@Test
	void verifyAnnotationAutowiredMethods() {
		assertThat(this.employee).as("The employee setter method should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("Dilbert");
	}

	@Test
	void verifyResourceAnnotationWiredFields() {
		assertThat(this.foo).as("The foo field should have been wired via @Resource.").isEqualTo("Foo");
	}

	@Test
	void verifyResourceAnnotationWiredMethods() {
		assertThat(this.bar).as("The bar method should have been wired via @Resource.").isEqualTo("Bar");
	}

}

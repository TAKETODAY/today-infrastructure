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

package cn.taketoday.aop.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class ThisAndTargetSelectionOnlyPointcutsTests {

	private TestInterface testBean;

	private Counter thisAsClassCounter;
	private Counter thisAsInterfaceCounter;
	private Counter targetAsClassCounter;
	private Counter targetAsInterfaceCounter;
	private Counter thisAsClassAndTargetAsClassCounter;
	private Counter thisAsInterfaceAndTargetAsInterfaceCounter;
	private Counter thisAsInterfaceAndTargetAsClassCounter;


	@BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBean = (TestInterface) ctx.getBean("testBean");
		thisAsClassCounter = (Counter) ctx.getBean("thisAsClassCounter");
		thisAsInterfaceCounter = (Counter) ctx.getBean("thisAsInterfaceCounter");
		targetAsClassCounter = (Counter) ctx.getBean("targetAsClassCounter");
		targetAsInterfaceCounter = (Counter) ctx.getBean("targetAsInterfaceCounter");
		thisAsClassAndTargetAsClassCounter = (Counter) ctx.getBean("thisAsClassAndTargetAsClassCounter");
		thisAsInterfaceAndTargetAsInterfaceCounter = (Counter) ctx.getBean("thisAsInterfaceAndTargetAsInterfaceCounter");
		thisAsInterfaceAndTargetAsClassCounter = (Counter) ctx.getBean("thisAsInterfaceAndTargetAsClassCounter");

		thisAsClassCounter.reset();
		thisAsInterfaceCounter.reset();
		targetAsClassCounter.reset();
		targetAsInterfaceCounter.reset();
		thisAsClassAndTargetAsClassCounter.reset();
		thisAsInterfaceAndTargetAsInterfaceCounter.reset();
		thisAsInterfaceAndTargetAsClassCounter.reset();
	}


	@Test
	public void testThisAsClassDoesNotMatch() {
		testBean.doIt();
		assertThat(thisAsClassCounter.getCount()).isEqualTo(0);
	}

	@Test
	public void testThisAsInterfaceMatch() {
		testBean.doIt();
		assertThat(thisAsInterfaceCounter.getCount()).isEqualTo(1);
	}

	@Test
	public void testTargetAsClassDoesMatch() {
		testBean.doIt();
		assertThat(targetAsClassCounter.getCount()).isEqualTo(1);
	}

	@Test
	public void testTargetAsInterfaceMatch() {
		testBean.doIt();
		assertThat(targetAsInterfaceCounter.getCount()).isEqualTo(1);
	}

	@Test
	public void testThisAsClassAndTargetAsClassCounterNotMatch() {
		testBean.doIt();
		assertThat(thisAsClassAndTargetAsClassCounter.getCount()).isEqualTo(0);
	}

	@Test
	public void testThisAsInterfaceAndTargetAsInterfaceCounterMatch() {
		testBean.doIt();
		assertThat(thisAsInterfaceAndTargetAsInterfaceCounter.getCount()).isEqualTo(1);
	}

	@Test
	public void testThisAsInterfaceAndTargetAsClassCounterMatch() {
		testBean.doIt();
		assertThat(thisAsInterfaceAndTargetAsInterfaceCounter.getCount()).isEqualTo(1);
	}

}


interface TestInterface {
	public void doIt();
}


class TestImpl implements TestInterface {
	@Override
	public void doIt() {
	}
}

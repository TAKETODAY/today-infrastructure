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
public class DeclareParentsDelegateRefTests {

	protected NoMethodsBean noMethodsBean;

	protected Counter counter;


	@BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		noMethodsBean = (NoMethodsBean) ctx.getBean("noMethodsBean");
		counter = (Counter) ctx.getBean("counter");
		counter.reset();
	}


	@Test
	public void testIntroductionWasMade() {
		boolean condition = noMethodsBean instanceof ICounter;
		assertThat(condition).as("Introduction must have been made").isTrue();
	}

	@Test
	public void testIntroductionDelegation() {
		((ICounter)noMethodsBean).increment();
		assertThat(counter.getCount()).as("Delegate's counter should be updated").isEqualTo(1);
	}

}


interface NoMethodsBean {
}


class NoMethodsBeanImpl implements NoMethodsBean {
}


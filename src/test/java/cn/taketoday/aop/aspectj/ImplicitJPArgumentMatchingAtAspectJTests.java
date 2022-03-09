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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

/**
 * Tests to check if the first implicit join point argument is correctly processed.
 * See SPR-3723 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class ImplicitJPArgumentMatchingAtAspectJTests {

	@Test
	@SuppressWarnings("resource")
	public void testAspect() {
		// nothing to really test; it is enough if we don't get error while creating the app context
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
	}

	@Aspect
	static class CounterAtAspectJAspect {
		@Around(value="execution(* cn.taketoday.beans.testfixture.beans.TestBean.*(..)) and this(bean) and args(argument)",
				argNames="bean,argument")
		public void increment(ProceedingJoinPoint pjp, TestBean bean, Object argument) throws Throwable {
			pjp.proceed();
		}
	}
}


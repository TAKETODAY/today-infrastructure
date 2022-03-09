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

/**
 * See SPR-1682.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class SharedPointcutWithArgsMismatchTests {

	private ToBeAdvised toBeAdvised;


	@BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		toBeAdvised = (ToBeAdvised) ctx.getBean("toBeAdvised");
	}


	@Test
	public void testMismatchedArgBinding() {
		this.toBeAdvised.foo("Hello");
	}

}


class ToBeAdvised {

	public void foo(String s) {
		System.out.println(s);
	}
}


class MyAspect {

	public void doBefore(int x) {
		System.out.println(x);
	}

	public void doBefore(String x) {
		System.out.println(x);
	}
}

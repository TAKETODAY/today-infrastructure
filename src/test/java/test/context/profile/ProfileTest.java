/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.context.profile;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import test.demo.config.User;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Today <br>
 * 
 *         2018-11-15 19:59
 */
public class ProfileTest {

	private long start;

	@Before
	public void start() {
		start = System.currentTimeMillis();
	}

	@After
	public void end() {
		System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
	}

	@Test
	public void test_Profile() {

		try (ApplicationContext applicationContext = new StandardApplicationContext("","test.demo.config")) {

			User user = applicationContext.getBean("user", User.class);
			System.out.println(user);
			assert "TEST".equals(user.getUserName());
		}
	}

	@Test
	public void test_Conditional() {

		try (ApplicationContext applicationContext = new StandardApplicationContext("","test.demo.config")) {
			User yhj = applicationContext.getBean("yhj", User.class);
			User user = applicationContext.getBean("user_", User.class);
			assert yhj != null;
			System.out.println(user);
			System.err.println(Arrays.toString(applicationContext.getEnvironment().getActiveProfiles()));
			assert "Windows".equals(user.getUserName());
		}
	}

}

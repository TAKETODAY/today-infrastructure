/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.demo.domain;

import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;

import test.context.condition.WindowsCondition;

/**
 * @author Today <br>
 * 
 *         2018-09-06 15:30
 */
@Configuration
public class ConfigurationBean {

	@Prototype
	public User user() {
		return new User().setId(12);
	}

	@Singleton("user__")
	public User user__() {
		return new User().setId(12);
	}

	@Profile("test")
	@Prototype("user")
	public User testUser() {
		return new User().setUserName("TEST");
	}

	@Profile("prod")
	@Singleton("user")
	public User prodUser() {
		return new User().setUserName("PROD");
	}

	@Singleton("user_")
	@Conditional(WindowsCondition.class)
	public User windowsUser() {
		return new User().setUserName("Windows");
	}

}

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

package cn.taketoday.test.context.env;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.Environment;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Sam Brannen
 * @since 5.2
 */
@SpringJUnitConfig
abstract class AbstractExplicitPropertiesFileTests {

	@Autowired
	Environment env;


	@Test
	@DisplayName("verify properties are available in the Environment")
	void verifyPropertiesAreAvailableInEnvironment() {
		String userHomeKey = "user.home";
		assertThat(env.getProperty(userHomeKey)).isEqualTo(System.getProperty(userHomeKey));
		assertThat(env.getProperty("explicit")).isEqualTo("enigma");
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}

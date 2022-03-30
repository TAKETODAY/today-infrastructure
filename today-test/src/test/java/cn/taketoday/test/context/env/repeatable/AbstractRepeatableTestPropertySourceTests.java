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

package cn.taketoday.test.context.env.repeatable;

import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.Environment;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;

/**
 * Abstract base class for integration tests involving
 * {@link TestPropertySource @TestPropertySource} as a repeatable annotation.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@ExtendWith(ApplicationExtension.class)
@ContextConfiguration
abstract class AbstractRepeatableTestPropertySourceTests {

	@Autowired
	Environment env;


	protected void assertEnvironmentValue(String key, String expected) {
		assertThat(env.getProperty(key)).as("Value of key [" + key + "].").isEqualTo(expected);
	}


	@Configuration
	static class Config {
	}

}

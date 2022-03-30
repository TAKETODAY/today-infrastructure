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

package cn.taketoday.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.ApplicationJUnit4ClassRunner;
import cn.taketoday.test.context.junit4.aci.FooBarAliasInitializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in conjunction with annotation-driven
 * configuration in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(ApplicationJUnit4ClassRunner.class)
@ContextConfiguration(classes = { GlobalConfig.class, DevProfileConfig.class }, initializers = FooBarAliasInitializer.class)
public class SingleInitializerAnnotationConfigTests {

	@Autowired
	protected String foo;

	@Autowired(required = false)
	@Qualifier("bar")
	protected String bar;

	@Autowired
	protected String baz;


	@Test
	public void activeBeans() {
		assertThat(foo).isEqualTo("foo");
		assertThat(bar).isEqualTo("foo");
		assertThat(baz).isEqualTo("global config");
	}

}

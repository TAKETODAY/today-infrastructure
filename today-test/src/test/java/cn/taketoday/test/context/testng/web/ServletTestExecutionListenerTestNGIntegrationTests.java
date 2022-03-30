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

package cn.taketoday.test.context.testng.web;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.testng.AbstractTestNGSpringContextTests;
import cn.taketoday.test.context.web.ServletTestExecutionListener;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.web.context.request.RequestContextHolder;
import cn.taketoday.web.context.request.ServletRequestAttributes;
import org.testng.annotations.Test;

/**
 * TestNG-based integration tests for {@link ServletTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see cn.taketoday.test.context.web.ServletTestExecutionListenerJUnitIntegrationTests
 */
@ContextConfiguration
@WebAppConfiguration
public class ServletTestExecutionListenerTestNGIntegrationTests extends AbstractTestNGSpringContextTests {

	@Configuration
	static class Config {
		/* no beans required for this test */
	}


	@Autowired
	private MockHttpServletRequest servletRequest;


	/**
	 * Verifies bug fix for <a href="https://jira.spring.io/browse/SPR-11626">SPR-11626</a>.
	 *
	 * @see #ensureMocksAreReinjectedBetweenTests_2
	 */
	@Test
	public void ensureMocksAreReinjectedBetweenTests_1() {
		assertInjectedServletRequestEqualsRequestInRequestContextHolder();
	}

	/**
	 * Verifies bug fix for <a href="https://jira.spring.io/browse/SPR-11626">SPR-11626</a>.
	 *
	 * @see #ensureMocksAreReinjectedBetweenTests_1
	 */
	@Test
	public void ensureMocksAreReinjectedBetweenTests_2() {
		assertInjectedServletRequestEqualsRequestInRequestContextHolder();
	}

	private void assertInjectedServletRequestEqualsRequestInRequestContextHolder() {
		assertThat(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest())
			.as("Injected ServletRequest must be stored in the RequestContextHolder")
			.isEqualTo(servletRequest);
	}

}

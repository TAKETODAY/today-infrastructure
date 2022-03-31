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

package cn.taketoday.test.web.servlet.htmlunit;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HostRequestMatcher}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.0
 */
public class HostRequestMatcherTests extends AbstractWebRequestMatcherTests {

	@Test
	public void localhost() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost");
		assertMatches(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://company.example/jquery-1.11.0.min.js");
	}

	@Test
	public void multipleHosts() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost", "example.com");
		assertMatches(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertMatches(matcher, "https://example.com/jquery-1.11.0.min.js");
	}

	@Test
	public void specificPort() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:8080");
		assertMatches(matcher, "http://localhost:8080/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://localhost:9090/jquery-1.11.0.min.js");
	}

	@Test
	public void defaultHttpPort() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:80");
		assertMatches(matcher, "http://localhost:80/jquery-1.11.0.min.js");
		assertMatches(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "https://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://localhost:9090/jquery-1.11.0.min.js");
	}

	@Test
	public void defaultHttpsPort() throws Exception {
		WebRequestMatcher matcher = new HostRequestMatcher("localhost:443");
		assertMatches(matcher, "https://localhost:443/jquery-1.11.0.min.js");
		assertMatches(matcher, "https://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "http://localhost/jquery-1.11.0.min.js");
		assertDoesNotMatch(matcher, "https://localhost:9090/jquery-1.11.0.min.js");
	}

}

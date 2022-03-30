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

package cn.taketoday.test.web.client.match;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.test.web.client.match.XpathRequestMatchers;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link XpathRequestMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class XpathRequestMatchersTests {

	private static final String RESPONSE_CONTENT = "<foo><bar>111</bar><bar>true</bar></foo>";

	private MockClientHttpRequest request;


	@BeforeEach
	public void setUp() throws IOException {
		this.request = new MockClientHttpRequest();
		this.request.getBody().write(RESPONSE_CONTENT.getBytes());
	}


	@Test
	public void testNodeMatcher() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).node(Matchers.notNullValue()).match(this.request);
	}

	@Test
	public void testNodeMatcherNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).node(Matchers.nullValue()).match(this.request));
	}

	@Test
	public void testExists() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).exists().match(this.request);
	}

	@Test
	public void testExistsNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/Bar", null).exists().match(this.request));
	}

	@Test
	public void testDoesNotExist() throws Exception {
		new XpathRequestMatchers("/foo/Bar", null).doesNotExist().match(this.request);
	}

	@Test
	public void testDoesNotExistNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).doesNotExist().match(this.request));
	}

	@Test
	public void testNodeCount() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).nodeCount(2).match(this.request);
	}

	@Test
	public void testNodeCountNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).nodeCount(1).match(this.request));
	}

	@Test
	public void testString() throws Exception {
		new XpathRequestMatchers("/foo/bar[1]", null).string("111").match(this.request);
	}

	@Test
	public void testStringNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[1]", null).string("112").match(this.request));
	}

	@Test
	public void testNumber() throws Exception {
		new XpathRequestMatchers("/foo/bar[1]", null).number(111.0).match(this.request);
	}

	@Test
	public void testNumberNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[1]", null).number(111.1).match(this.request));
	}

	@Test
	public void testBoolean() throws Exception {
		new XpathRequestMatchers("/foo/bar[2]", null).booleanValue(true).match(this.request);
	}

	@Test
	public void testBooleanNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[2]", null).booleanValue(false).match(this.request));
	}

}

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

package cn.taketoday.web.handler.condition;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.bind.annotation.RequestMethod;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;

import java.util.Collections;

import cn.taketoday.web.handler.condition.RequestMethodsRequestCondition;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.web.bind.annotation.RequestMethod.DELETE;
import static cn.taketoday.web.bind.annotation.RequestMethod.GET;
import static cn.taketoday.web.bind.annotation.RequestMethod.HEAD;
import static cn.taketoday.web.bind.annotation.RequestMethod.OPTIONS;
import static cn.taketoday.web.bind.annotation.RequestMethod.POST;
import static cn.taketoday.web.bind.annotation.RequestMethod.PUT;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMethodsRequestConditionTests {

	@Test
	public void getMatchingCondition() {
		testMatch(new RequestMethodsRequestCondition(GET), GET);
		testMatch(new RequestMethodsRequestCondition(GET, POST), GET);
		testNoMatch(new RequestMethodsRequestCondition(GET), POST);
	}

	@Test
	public void getMatchingConditionWithHttpHead() {
		testMatch(new RequestMethodsRequestCondition(HEAD), HEAD);
		testMatch(new RequestMethodsRequestCondition(GET), GET);
		testNoMatch(new RequestMethodsRequestCondition(POST), HEAD);
	}

	@Test
	public void getMatchingConditionWithEmptyConditions() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();
		for (RequestMethod method : RequestMethod.values()) {
			if (method != OPTIONS) {
				HttpServletRequest request = new MockHttpServletRequest(method.name(), "");
				assertThat(condition.getMatchingCondition(request)).isNotNull();
			}
		}
		testNoMatch(condition, OPTIONS);
	}

	@Test
	public void getMatchingConditionWithCustomMethod() {
		HttpServletRequest request = new MockHttpServletRequest("PROPFIND", "");
		assertThat(new RequestMethodsRequestCondition().getMatchingCondition(request)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(GET, POST).getMatchingCondition(request)).isNull();
	}

	@Test
	public void getMatchingConditionWithCorsPreFlight() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "");
		request.addHeader("Origin", "https://example.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");

		assertThat(new RequestMethodsRequestCondition().getMatchingCondition(request)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(PUT).getMatchingCondition(request)).isNotNull();
		assertThat(new RequestMethodsRequestCondition(DELETE).getMatchingCondition(request)).isNull();
	}

	@Test // SPR-14410
	public void getMatchingConditionWithHttpOptionsInErrorDispatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/path");
		request.setDispatcherType(DispatcherType.ERROR);

		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();
		RequestMethodsRequestCondition result = condition.getMatchingCondition(request);

		assertThat(result).isNotNull();
		assertThat(result).isSameAs(condition);
	}

	@Test
	public void compareTo() {
		RequestMethodsRequestCondition c1 = new RequestMethodsRequestCondition(GET, HEAD);
		RequestMethodsRequestCondition c2 = new RequestMethodsRequestCondition(POST);
		RequestMethodsRequestCondition c3 = new RequestMethodsRequestCondition();

		MockHttpServletRequest request = new MockHttpServletRequest();

		int result = c1.compareTo(c2, request);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

		result = c2.compareTo(c1, request);
		assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();

		result = c2.compareTo(c3, request);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

		result = c1.compareTo(c1, request);
		assertThat(result).as("Invalid comparison result ").isEqualTo(0);
	}

	@Test
	public void combine() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(GET);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(POST);

		RequestMethodsRequestCondition result = condition1.combine(condition2);
		assertThat(result.getContent().size()).isEqualTo(2);
	}


	private void testMatch(RequestMethodsRequestCondition condition, RequestMethod method) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
		RequestMethodsRequestCondition actual = condition.getMatchingCondition(request);
		assertThat(actual).isNotNull();
		assertThat(actual.getContent()).isEqualTo(Collections.singleton(method));
	}

	private void testNoMatch(RequestMethodsRequestCondition condition, RequestMethod method) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "");
		assertThat(condition.getMatchingCondition(request)).isNull();
	}

}

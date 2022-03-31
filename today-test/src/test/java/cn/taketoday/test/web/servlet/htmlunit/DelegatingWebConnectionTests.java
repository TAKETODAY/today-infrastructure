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

import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.taketoday.core.testfixture.EnabledForTestGroups;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.htmlunit.DelegatingWebConnection;
import cn.taketoday.test.web.servlet.htmlunit.DelegatingWebConnection.DelegateWebConnection;
import cn.taketoday.test.web.servlet.htmlunit.MockMvcWebConnection;
import cn.taketoday.test.web.servlet.htmlunit.UrlRegexRequestMatcher;
import cn.taketoday.test.web.servlet.htmlunit.WebRequestMatcher;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;

import java.net.URL;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static cn.taketoday.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * Unit and integration tests for {@link DelegatingWebConnection}.
 *
 * @author Rob Winch
 * @since 4.0
 */
@ExtendWith(MockitoExtension.class)
public class DelegatingWebConnectionTests {

	private DelegatingWebConnection webConnection;

	private WebRequest request;

	private WebResponse expectedResponse;


	@Mock
	private WebRequestMatcher matcher1;

	@Mock
	private WebRequestMatcher matcher2;

	@Mock
	private WebConnection defaultConnection;

	@Mock
	private WebConnection connection1;

	@Mock
	private WebConnection connection2;


	@BeforeEach
	public void setup() throws Exception {
		request = new WebRequest(new URL("http://localhost/"));
		WebResponseData data = new WebResponseData("".getBytes("UTF-8"), 200, "", Collections.emptyList());
		expectedResponse = new WebResponse(data, request, 100L);
		webConnection = new DelegatingWebConnection(defaultConnection,
				new DelegateWebConnection(matcher1, connection1), new DelegateWebConnection(matcher2, connection2));
	}


	@Test
	public void getResponseDefault() throws Exception {
		given(defaultConnection.getResponse(request)).willReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1, connection2);
		verify(defaultConnection).getResponse(request);
	}

	@Test
	public void getResponseAllMatches() throws Exception {
		given(matcher1.matches(request)).willReturn(true);
		given(connection1.getResponse(request)).willReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verifyNoMoreInteractions(matcher2, connection2, defaultConnection);
		verify(connection1).getResponse(request);
	}

	@Test
	public void getResponseSecondMatches() throws Exception {
		given(matcher2.matches(request)).willReturn(true);
		given(connection2.getResponse(request)).willReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1, defaultConnection);
		verify(connection2).getResponse(request);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	public void verifyExampleInClassLevelJavadoc() throws Exception {
		WebClient webClient = new WebClient();

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup().build();
		MockMvcWebConnection mockConnection = new MockMvcWebConnection(mockMvc, webClient);

		WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
		WebConnection httpConnection = new HttpWebConnection(webClient);
		webClient.setWebConnection(
				new DelegatingWebConnection(mockConnection, new DelegateWebConnection(cdnMatcher, httpConnection)));

		Page page = webClient.getPage("https://code.jquery.com/jquery-1.11.0.min.js");
		assertThat(page.getWebResponse().getStatusCode()).isEqualTo(200);
		assertThat(page.getWebResponse().getContentAsString()).isNotEmpty();
	}


	@Controller
	static class TestController {
	}

}

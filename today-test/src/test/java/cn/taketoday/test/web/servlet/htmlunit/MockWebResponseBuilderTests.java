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

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.mock.web.MockHttpServletResponse;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.test.web.servlet.htmlunit.MockWebResponseBuilder;
import cn.taketoday.test.web.w.servlet.htmlunit.MockWebResponseBuilder;
import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


/**
 * Tests for {@link MockWebResponseBuilder}.
 *
 * @author Rob Winch
 * @since 4.2
 */
public class MockWebResponseBuilderTests {

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private WebRequest webRequest;

	private MockWebResponseBuilder responseBuilder;


	@BeforeEach
	public void setup() throws Exception {
		this.webRequest = new WebRequest(new URL("http://company.example:80/test/this/here"));
		this.responseBuilder = new MockWebResponseBuilder(System.currentTimeMillis(), this.webRequest, this.response);
	}


	// --- constructor

	@Test
	public void constructorWithNullWebRequest() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockWebResponseBuilder(0L, null, this.response));
	}

	@Test
	public void constructorWithNullResponse() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockWebResponseBuilder(0L, new WebRequest(new URL("http://company.example:80/test/this/here")), null));
	}


	// --- build

	@Test
	public void buildContent() throws Exception {
		this.response.getWriter().write("expected content");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentAsString()).isEqualTo("expected content");
	}

	@Test
	public void buildContentCharset() throws Exception {
		this.response.addHeader("Content-Type", "text/html; charset=UTF-8");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentCharset()).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	public void buildContentType() throws Exception {
		this.response.addHeader("Content-Type", "text/html; charset-UTF-8");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getContentType()).isEqualTo("text/html");
	}

	@Test
	public void buildResponseHeaders() throws Exception {
		this.response.addHeader("Content-Type", "text/html");
		this.response.addHeader("X-Test", "value");
		Cookie cookie = new Cookie("cookieA", "valueA");
		cookie.setDomain("domain");
		cookie.setPath("/path");
		cookie.setMaxAge(1800);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		this.response.addCookie(cookie);
		WebResponse webResponse = this.responseBuilder.build();

		List<NameValuePair> responseHeaders = webResponse.getResponseHeaders();
		assertThat(responseHeaders.size()).isEqualTo(3);
		NameValuePair header = responseHeaders.get(0);
		assertThat(header.getName()).isEqualTo("Content-Type");
		assertThat(header.getValue()).isEqualTo("text/html");
		header = responseHeaders.get(1);
		assertThat(header.getName()).isEqualTo("X-Test");
		assertThat(header.getValue()).isEqualTo("value");
		header = responseHeaders.get(2);
		assertThat(header.getName()).isEqualTo("Set-Cookie");
		assertThat(header.getValue())
				.startsWith("cookieA=valueA; Path=/path; Domain=domain; Max-Age=1800; Expires=")
				.endsWith("; Secure; HttpOnly");
	}

	// SPR-14169
	@Test
	public void buildResponseHeadersNullDomainDefaulted() throws Exception {
		Cookie cookie = new Cookie("cookieA", "valueA");
		this.response.addCookie(cookie);
		WebResponse webResponse = this.responseBuilder.build();

		List<NameValuePair> responseHeaders = webResponse.getResponseHeaders();
		assertThat(responseHeaders.size()).isEqualTo(1);
		NameValuePair header = responseHeaders.get(0);
		assertThat(header.getName()).isEqualTo("Set-Cookie");
		assertThat(header.getValue()).isEqualTo("cookieA=valueA");
	}

	@Test
	public void buildStatus() throws Exception {
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode()).isEqualTo(200);
		assertThat(webResponse.getStatusMessage()).isEqualTo("OK");
	}

	@Test
	public void buildStatusNotOk() throws Exception {
		this.response.setStatus(401);
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode()).isEqualTo(401);
		assertThat(webResponse.getStatusMessage()).isEqualTo("Unauthorized");
	}

	@Test
	public void buildStatusWithCustomMessage() throws Exception {
		this.response.sendError(401, "Custom");
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getStatusCode()).isEqualTo(401);
		assertThat(webResponse.getStatusMessage()).isEqualTo("Custom");
	}

	@Test
	public void buildWebRequest() throws Exception {
		WebResponse webResponse = this.responseBuilder.build();

		assertThat(webResponse.getWebRequest()).isEqualTo(this.webRequest);
	}

}

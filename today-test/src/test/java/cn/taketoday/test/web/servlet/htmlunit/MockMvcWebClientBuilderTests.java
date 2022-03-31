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

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;

import org.junit.jupiter.api.Test;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.bind.annotation.CookieValue;
import cn.taketoday.web.bind.annotation.DeleteMapping;
import cn.taketoday.web.bind.annotation.PostMapping;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.RequestParam;
import cn.taketoday.web.bind.annotation.RestController;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.net.URL;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Integration tests for {@link MockMvcWebClientBuilder}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@JUnitWebConfig
class MockMvcWebClientBuilderTests {

	private MockMvc mockMvc;

	MockMvcWebClientBuilderTests(WebApplicationContext wac) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}


	@Test
	void mockMvcSetupNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcWebClientBuilder.mockMvcSetup(null));
	}

	@Test
	void webAppContextSetupNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcWebClientBuilder.webAppContextSetup(null));
	}

	@Test
	void mockMvcSetupWithDefaultWebClientDelegate() throws Exception {
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertMockMvcUsed(client, "http://localhost/test");
	}

	@Test
	void mockMvcSetupWithCustomWebClientDelegate() throws Exception {
		WebClient otherClient = new WebClient();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).withDelegate(otherClient).build();

		assertMockMvcUsed(client, "http://localhost/test");
	}

	@Test // SPR-14066
	void cookieManagerShared() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("NA");
		client.getCookieManager().addCookie(new Cookie("localhost", "cookie", "cookieManagerShared"));
		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("cookieManagerShared");
	}

	@Test // SPR-14265
	void cookiesAreManaged() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new CookieController()).build();
		WebClient client = MockMvcWebClientBuilder.mockMvcSetup(this.mockMvc).build();

		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("NA");
		assertThat(postResponse(client, "http://localhost/?cookie=foo").getContentAsString()).isEqualTo("Set");
		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("foo");
		assertThat(deleteResponse(client, "http://localhost/").getContentAsString()).isEqualTo("Delete");
		assertThat(getResponse(client, "http://localhost/").getContentAsString()).isEqualTo("NA");
	}

	private void assertMockMvcUsed(WebClient client, String url) throws Exception {
		assertThat(getResponse(client, url).getContentAsString()).isEqualTo("mvc");
	}

	private WebResponse getResponse(WebClient client, String url) throws IOException {
		return createResponse(client, new WebRequest(new URL(url)));
	}

	private WebResponse postResponse(WebClient client, String url) throws IOException {
		return createResponse(client, new WebRequest(new URL(url), HttpMethod.POST));
	}

	private WebResponse deleteResponse(WebClient client, String url) throws IOException {
		return createResponse(client, new WebRequest(new URL(url), HttpMethod.DELETE));
	}

	private WebResponse createResponse(WebClient client, WebRequest request) throws IOException {
		return client.getWebConnection().getResponse(request);
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@RestController
		static class ContextPathController {

			@RequestMapping("/test")
			String contextPath(HttpServletRequest request) {
				return "mvc";
			}
		}
	}

	@RestController
	static class CookieController {

		static final String COOKIE_NAME = "cookie";

		@RequestMapping(path = "/", produces = "text/plain")
		String cookie(@CookieValue(name = COOKIE_NAME, defaultValue = "NA") String cookie) {
			return cookie;
		}

		@PostMapping(path = "/", produces = "text/plain")
		String setCookie(@RequestParam String cookie, HttpServletResponse response) {
			response.addCookie(new jakarta.servlet.http.Cookie(COOKIE_NAME, cookie));
			return "Set";
		}

		@DeleteMapping(path = "/", produces = "text/plain")
		String deleteCookie(HttpServletResponse response) {
			jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(COOKIE_NAME, "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			return "Delete";
		}
	}

}

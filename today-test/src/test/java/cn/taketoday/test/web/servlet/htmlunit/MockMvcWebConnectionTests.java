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

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;

import org.junit.jupiter.api.Test;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


/**
 * Integration tests for {@link MockMvcWebConnection}.
 *
 * @author Rob Winch
 * @since 4.0
 */
public class MockMvcWebConnectionTests {

	private final WebClient webClient = new WebClient();

	private final MockMvc mockMvc =
			MockMvcBuilders.standaloneSetup(new HelloController(), new ForwardController()).build();


	@Test
	public void contextPathNull() throws IOException {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, null));
		Page page = this.webClient.getPage("http://localhost/context/a");
		assertThat(page.getWebResponse().getStatusCode()).isEqualTo(200);
	}

	@Test
	public void contextPathExplicit() throws IOException {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, "/context"));
		Page page = this.webClient.getPage("http://localhost/context/a");
		assertThat(page.getWebResponse().getStatusCode()).isEqualTo(200);
	}

	@Test
	public void contextPathEmpty() throws IOException {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, ""));
		// Empty context path (root context) should not match to a URL with a context path
		assertThatExceptionOfType(FailingHttpStatusCodeException.class).isThrownBy(() ->
				this.webClient.getPage("http://localhost/context/a"))
			.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(404));
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient));
		// No context is the same providing an empty context path
		assertThatExceptionOfType(FailingHttpStatusCodeException.class).isThrownBy(() ->
				this.webClient.getPage("http://localhost/context/a"))
		.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(404));
	}

	@Test
	public void forward() throws IOException {
		this.webClient.setWebConnection(new MockMvcWebConnection(this.mockMvc, this.webClient, ""));
		Page page = this.webClient.getPage("http://localhost/forward");
		assertThat(page.getWebResponse().getContentAsString()).isEqualTo("hello");
	}

	@Test
	@SuppressWarnings("resource")
	public void contextPathDoesNotStartWithSlash() throws IOException {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnection(this.mockMvc, this.webClient, "context"));
	}

	@Test
	@SuppressWarnings("resource")
	public void contextPathEndsWithSlash() throws IOException {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new MockMvcWebConnection(this.mockMvc, this.webClient, "/context/"));
	}

}

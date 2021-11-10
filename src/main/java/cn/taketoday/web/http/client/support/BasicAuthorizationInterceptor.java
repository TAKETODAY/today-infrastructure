/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.http.client.support;

import cn.taketoday.web.http.HttpRequest;
import cn.taketoday.web.http.client.ClientHttpRequestExecution;
import cn.taketoday.web.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.web.http.client.ClientHttpResponse;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.Base64Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@link ClientHttpRequestInterceptor} to apply a BASIC authorization header.
 *
 * @author Phillip Webb
 * @since 4.0
 * @deprecated as of 5.1.1, in favor of {@link BasicAuthenticationInterceptor}
 * which reuses {@link cn.taketoday.web.http.HttpHeaders#setBasicAuth},
 * sharing its default charset ISO-8859-1 instead of UTF-8 as used here
 */
@Deprecated
public class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {

	private final String username;

	private final String password;


	/**
	 * Create a new interceptor which adds a BASIC authorization header
	 * for the given username and password.
	 * @param username the username to use
	 * @param password the password to use
	 */
	public BasicAuthorizationInterceptor(@Nullable String username, @Nullable String password) {
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		this.username = (username != null ? username : "");
		this.password = (password != null ? password : "");
	}


	@Override
	public ClientHttpResponse intercept(
			HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

		String token = Base64Utils.encodeToString(
				(this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));
		request.getHeaders().add("Authorization", "Basic " + token);
		return execution.execute(request, body);
	}

}

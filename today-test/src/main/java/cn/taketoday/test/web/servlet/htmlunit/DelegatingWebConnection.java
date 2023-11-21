/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.htmlunit.WebConnection;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.lang.Assert;

/**
 * Implementation of {@link WebConnection} that allows delegating to various
 * {@code WebConnection} implementations.
 *
 * <p>For example, if you host your JavaScript on the domain {@code code.jquery.com},
 * you might want to use the following.
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 *
 * MockMvc mockMvc = ...
 * MockMvcWebConnection mockConnection = new MockMvcWebConnection(mockMvc, webClient);
 *
 * WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
 * WebConnection httpConnection = new HttpWebConnection(webClient);
 * WebConnection webConnection = new DelegatingWebConnection(mockConnection, new DelegateWebConnection(cdnMatcher, httpConnection));
 *
 * webClient.setWebConnection(webConnection);
 *
 * WebClient webClient = new WebClient();
 * webClient.setWebConnection(webConnection);
 * </pre>
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.0
 */
public final class DelegatingWebConnection implements WebConnection {

  private final List<DelegateWebConnection> connections;

  private final WebConnection defaultConnection;

  public DelegatingWebConnection(WebConnection defaultConnection, List<DelegateWebConnection> connections) {
    Assert.notNull(defaultConnection, "Default WebConnection is required");
    Assert.notEmpty(connections, "Connections List must not be empty");
    this.connections = connections;
    this.defaultConnection = defaultConnection;
  }

  public DelegatingWebConnection(WebConnection defaultConnection, DelegateWebConnection... connections) {
    this(defaultConnection, Arrays.asList(connections));
  }

  @Override
  public WebResponse getResponse(WebRequest request) throws IOException {
    for (DelegateWebConnection connection : this.connections) {
      if (connection.getMatcher().matches(request)) {
        return connection.getDelegate().getResponse(request);
      }
    }
    return this.defaultConnection.getResponse(request);
  }

  @Override
  public void close() {
  }

  /**
   * The delegate web connection.
   */
  public static final class DelegateWebConnection {

    private final WebRequestMatcher matcher;

    private final WebConnection delegate;

    public DelegateWebConnection(WebRequestMatcher matcher, WebConnection delegate) {
      this.matcher = matcher;
      this.delegate = delegate;
    }

    private WebRequestMatcher getMatcher() {
      return this.matcher;
    }

    private WebConnection getDelegate() {
      return this.delegate;
    }
  }

}

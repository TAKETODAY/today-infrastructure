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

package cn.taketoday.mock.http.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.http.MockHttpOutputMessage;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class MockClientHttpRequest extends MockHttpOutputMessage implements ClientHttpRequest {

  private HttpMethod httpMethod;

  private URI uri;

  @Nullable
  private ClientHttpResponse clientHttpResponse;

  private boolean executed = false;

  /**
   * Default constructor.
   */
  public MockClientHttpRequest() {
    this.httpMethod = HttpMethod.GET;
    try {
      this.uri = new URI("/");
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Create an instance with the given HttpMethod and URI.
   */
  public MockClientHttpRequest(HttpMethod httpMethod, URI uri) {
    this.httpMethod = httpMethod;
    this.uri = uri;
  }

  public void setMethod(HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
  }

  @Override
  public HttpMethod getMethod() {
    return this.httpMethod;
  }

  @Override
  @Deprecated
  public String getMethodValue() {
    return this.httpMethod.name();
  }

  public void setURI(URI uri) {
    this.uri = uri;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  public void setResponse(ClientHttpResponse clientHttpResponse) {
    this.clientHttpResponse = clientHttpResponse;
  }

  public boolean isExecuted() {
    return this.executed;
  }

  /**
   * Set the {@link #isExecuted() executed} flag to {@code true} and return the
   * configured {@link #setResponse(ClientHttpResponse) response}.
   *
   * @see #executeInternal()
   */
  @Override
  public final ClientHttpResponse execute() throws IOException {
    this.executed = true;
    return executeInternal();
  }

  /**
   * The default implementation returns the configured
   * {@link #setResponse(ClientHttpResponse) response}.
   * <p>Override this method to execute the request and provide a response,
   * potentially different than the configured response.
   */
  protected ClientHttpResponse executeInternal() throws IOException {
    Assert.state(this.clientHttpResponse != null, "No ClientHttpResponse");
    return this.clientHttpResponse;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.httpMethod);
    sb.append(' ').append(this.uri);
    if (!getHeaders().isEmpty()) {
      sb.append(", headers: ").append(getHeaders());
    }
    if (sb.length() == 0) {
      sb.append("Not yet initialized");
    }
    return sb.toString();
  }

}

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

package cn.taketoday.framework.test.web.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.support.HttpRequestDecorator;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.test.web.client.ExpectedCount;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import cn.taketoday.test.web.client.RequestExpectationManager;
import cn.taketoday.test.web.client.RequestMatcher;
import cn.taketoday.test.web.client.ResponseActions;
import cn.taketoday.test.web.client.SimpleRequestExpectationManager;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RootUriTemplateHandler;
import cn.taketoday.web.util.UriTemplateHandler;

/**
 * {@link RequestExpectationManager} that strips the specified root URI from the request
 * before verification. Can be used to simply test declarations when all REST calls start
 * the same way. For example: <pre class="code">
 * RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://example.com").build();
 * MockRestServiceServer server = RootUriRequestExpectationManager.bindTo(restTemplate);
 * server.expect(requestTo("/hello")).andRespond(withSuccess());
 * restTemplate.getForEntity("/hello", String.class);
 * </pre>
 *
 * @author Phillip Webb
 * @see RootUriTemplateHandler
 * @see #bindTo(RestTemplate)
 * @see #forRestTemplate(RestTemplate, RequestExpectationManager)
 * @since 4.0
 */
public class RootUriRequestExpectationManager implements RequestExpectationManager {

  private final String rootUri;

  private final RequestExpectationManager expectationManager;

  public RootUriRequestExpectationManager(String rootUri, RequestExpectationManager expectationManager) {
    Assert.notNull(rootUri, "RootUri is required");
    Assert.notNull(expectationManager, "ExpectationManager is required");
    this.rootUri = rootUri;
    this.expectationManager = expectationManager;
  }

  @Override
  public ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
    return this.expectationManager.expectRequest(count, requestMatcher);
  }

  @Override
  public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
    String uri = request.getURI().toString();
    if (uri.startsWith(this.rootUri)) {
      request = replaceURI(request, uri.substring(this.rootUri.length()));
    }
    try {
      return this.expectationManager.validateRequest(request);
    }
    catch (AssertionError ex) {
      String message = ex.getMessage();
      String prefix = "Request URI expected:</";
      if (message != null && message.startsWith(prefix)) {
        throw new AssertionError(
                "Request URI expected:<" + this.rootUri + message.substring(prefix.length() - 1));
      }
      throw ex;
    }
  }

  private ClientHttpRequest replaceURI(ClientHttpRequest request, String replacementUri) {
    URI uri;
    try {
      uri = new URI(replacementUri);
      if (request instanceof MockClientHttpRequest) {
        ((MockClientHttpRequest) request).setURI(uri);
        return request;
      }
      return new ReplaceUriClientHttpRequest(uri, request);
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void verify() {
    this.expectationManager.verify();
  }

  @Override
  public void verify(Duration timeout) {
    this.expectationManager.verify(timeout);
  }

  @Override
  public void reset() {
    this.expectationManager.reset();
  }

  /**
   * Return a bound {@link MockRestServiceServer} for the given {@link RestTemplate},
   * configured with {@link RootUriRequestExpectationManager} when possible.
   *
   * @param restTemplate the source REST template
   * @return a configured {@link MockRestServiceServer}
   */
  public static MockRestServiceServer bindTo(RestTemplate restTemplate) {
    return bindTo(restTemplate, new SimpleRequestExpectationManager());
  }

  /**
   * Return a bound {@link MockRestServiceServer} for the given {@link RestTemplate},
   * configured with {@link RootUriRequestExpectationManager} when possible.
   *
   * @param restTemplate the source REST template
   * @param expectationManager the source {@link RequestExpectationManager}
   * @return a configured {@link MockRestServiceServer}
   */
  public static MockRestServiceServer bindTo(RestTemplate restTemplate,
          RequestExpectationManager expectationManager) {
    MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(restTemplate);
    return builder.build(forRestTemplate(restTemplate, expectationManager));
  }

  /**
   * Return {@link RequestExpectationManager} to be used for binding with the specified
   * {@link RestTemplate}. If the {@link RestTemplate} is using a
   * {@link RootUriTemplateHandler} then a {@link RootUriRequestExpectationManager} is
   * returned, otherwise the source manager is returned unchanged.
   *
   * @param restTemplate the source REST template
   * @param expectationManager the source {@link RequestExpectationManager}
   * @return a {@link RequestExpectationManager} to be bound to the template
   */
  public static RequestExpectationManager forRestTemplate(RestTemplate restTemplate,
          RequestExpectationManager expectationManager) {
    Assert.notNull(restTemplate, "RestTemplate is required");
    UriTemplateHandler templateHandler = restTemplate.getUriTemplateHandler();
    if (templateHandler instanceof RootUriTemplateHandler) {
      return new RootUriRequestExpectationManager(((RootUriTemplateHandler) templateHandler).getRootUri(),
              expectationManager);
    }
    return expectationManager;
  }

  /**
   * {@link ClientHttpRequest} wrapper to replace the request URI.
   */
  private static class ReplaceUriClientHttpRequest extends HttpRequestDecorator implements ClientHttpRequest {

    private final URI uri;

    ReplaceUriClientHttpRequest(URI uri, ClientHttpRequest request) {
      super(request);
      this.uri = uri;
    }

    @Override
    public URI getURI() {
      return this.uri;
    }

    @Override
    public OutputStream getBody() throws IOException {
      return getRequest().getBody();
    }

    @Override
    public ClientHttpResponse execute() throws IOException {
      return getRequest().execute();
    }

    @Override
    public ClientHttpRequest getRequest() {
      return (ClientHttpRequest) super.getRequest();
    }

  }

}

/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.client;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HttpComponentsClientHttpExchangeTests {

  private static final URI URI_VALUE = URI.create("https://example.test/orders");

  @Test
  @SuppressWarnings("deprecation") // RequestConfig is stored as an attribute on a plain HttpContext.
  void usesAnExplicitQueryRequestConfigInACustomContext() throws Exception {
    HttpClient client = mock();
    var config = RequestConfig.custom().setRedirectsEnabled(false).build();
    var context = new BasicHttpContext();
    var factory = new HttpComponentsClientHttpRequestFactory(client) {
      @Override
      protected void postProcessHttpRequest(ClassicHttpRequest request) {
        ((HttpUriRequestBase) request).setConfig(config);
      }
    };
    factory.setHttpContextFactory((method, uri) -> {
      assertThat(method).isEqualTo(HttpMethod.QUERY);
      assertThat(uri).isEqualTo(URI_VALUE);
      return context;
    });

    var request = (HttpComponentsClientHttpRequest) factory.createRequest(URI_VALUE, HttpMethod.QUERY);
    assertThat(request.getMethod()).isEqualTo(HttpMethod.QUERY);
    assertThat(request.getHttpContext()).isSameAs(context);
    assertThat(context.getAttribute(HttpClientContext.REQUEST_CONFIG)).isSameAs(config);
    factory.destroy();
  }

  @Test
  @SuppressWarnings("deprecation") // A plain HttpContext can carry a request config attribute.
  void preservesExistingContextConfigurationAndDoesNotInstallNullDefaults() {
    var factory = new HttpComponentsClientHttpRequestFactory(mock()) {
      @Override
      protected RequestConfig createRequestConfig(Object client) {
        return null;
      }
    };
    var config = RequestConfig.custom().setRedirectsEnabled(false).build();
    var context = HttpClientContext.create();
    context.setRequestConfig(config);
    factory.setHttpContextFactory((method, uri) -> context);
    var request = (HttpComponentsClientHttpRequest) factory.createRequest(URI_VALUE, HttpMethod.GET);
    assertThat(((HttpClientContext) request.getHttpContext()).getRequestConfig()).isSameAs(config);

    var plainContext = new BasicHttpContext();
    plainContext.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
    factory.setHttpContextFactory((method, uri) -> plainContext);
    assertThat(((HttpComponentsClientHttpRequest) factory.createRequest(URI_VALUE, HttpMethod.GET)).getHttpContext())
            .isSameAs(plainContext);
    assertThat(plainContext.getAttribute(HttpClientContext.REQUEST_CONFIG)).isSameAs(config);

    plainContext.removeAttribute(HttpClientContext.REQUEST_CONFIG);
    var withoutDefault = (HttpComponentsClientHttpRequest) factory.createRequest(URI_VALUE, HttpMethod.GET);
    assertThat(withoutDefault.getHttpContext().getAttribute(HttpClientContext.REQUEST_CONFIG)).isNull();
  }

  @Test
  void usesFactoryDefaultsForARequestWithoutItsOwnConfiguration() {
    var factory = new HttpComponentsClientHttpRequestFactory(mock()) {
      @Override
      protected ClassicHttpRequest createHttpUriRequest(HttpMethod method, URI uri) {
        return new BasicClassicHttpRequest(method.name(), uri);
      }
    };
    factory.setReadTimeout(250);

    var request = (HttpComponentsClientHttpRequest) factory.createRequest(URI_VALUE, HttpMethod.GET);

    assertThat(((HttpClientContext) request.getHttpContext()).getRequestConfig().getResponseTimeout().toMilliseconds())
            .isEqualTo(250);
  }

  @Test
  void retainsContentEncodingWhenTheNativeEntityIsStillEncoded() {
    var incoming = new BasicClassicHttpResponse(200);
    incoming.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
    incoming.setEntity(new StringEntity("body", ContentType.TEXT_PLAIN, "gzip", false));

    try (var response = new HttpComponentsClientHttpResponse(incoming)) {
      assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING)).isEqualTo("gzip");
    }
  }

  @Test
  void streamsAnAggregatedBodyAndSerializesCookiesAsOneHeader() throws Exception {
    var outgoing = new HttpPost(URI_VALUE);
    var context = new BasicHttpContext();
    HttpClient client = mock();
    var incoming = new BasicClassicHttpResponse(202, "Queued");
    given(client.executeOpen(null, outgoing, context)).willReturn(incoming);
    var request = new HttpComponentsClientHttpRequest(client, outgoing, context);
    request.getHeaders().add(HttpHeaders.COOKIE, "session=test");
    request.getHeaders().add(HttpHeaders.COOKIE, "locale=zh");
    request.getHeaders().set(HttpHeaders.TRANSFER_ENCODING, "chunked");
    request.setContentLength(2);
    var body = request.getBody();
    assertThat(request.getBody()).isSameAs(body);
    body.write(new byte[] { 5, 7 });
    assertThatIllegalStateException().isThrownBy(() -> request.setBody(output -> { })).withMessageContaining("not both");

    try (var response = request.async(Runnable::run).get(5, TimeUnit.SECONDS)) {
      assertThat(response.getRawStatusCode()).isEqualTo(202);
      assertThat(response.getStatusText()).isEqualTo("Queued");
      assertThat(response.getHeaders().isEmpty()).isTrue();
      assertThat(response.getBody().read()).isEqualTo(-1);
    }
    assertThat(outgoing.getHeaders(HttpHeaders.COOKIE)).singleElement()
            .satisfies(header -> assertThat(header.getValue()).isEqualTo("session=test; locale=zh"));
    assertThat(outgoing.containsHeader(HttpHeaders.TRANSFER_ENCODING)).isFalse();
    assertThat(outgoing.containsHeader(HttpHeaders.CONTENT_LENGTH)).isFalse();
    try (var entity = outgoing.getEntity()) {
      var bytes = new ByteArrayOutputStream();
      entity.writeTo(bytes);
      assertThat(bytes.toByteArray()).containsExactly(5, 7);
      assertThat(entity.getContentLength()).isEqualTo(2);
      assertThat(entity.isStreaming()).isFalse();
      assertThatThrownBy(entity::getContent).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  void exposesTheCauseWhenApacheCannotReadTheRequestUri() throws Exception {
    ClassicHttpRequest outgoing = mock();
    var failure = new URISyntaxException("invalid", "invalid URI");
    given(outgoing.getMethod()).willReturn("GET");
    given(outgoing.getUri()).willThrow(failure);
    var request = new HttpComponentsClientHttpRequest(mock(), outgoing, new BasicHttpContext());
    request.setBody(output -> { });

    assertThatIllegalStateException().isThrownBy(request::getBody).withMessageContaining("not both");
    assertThatIllegalStateException().isThrownBy(request::getURI).withCause(failure);
  }

  @Test
  void ignoresAnIoFailureWhenClosingTheNativeResponse() throws Exception {
    ClassicHttpResponse incoming = mock();
    doThrow(new IOException("close failed")).when(incoming).close();

    new HttpComponentsClientHttpResponse(incoming).close();

    verify(incoming).close();
  }

}

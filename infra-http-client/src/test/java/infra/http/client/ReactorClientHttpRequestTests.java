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

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import infra.http.HttpMethod;
import infra.http.support.ReactorResourceFactory;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ReactorClientHttpRequestTests {

  private final HttpClient transport = mock();

  private final HttpClient.RequestSender sender = mock();

  private final HttpClient.ResponseReceiver<?> receiver = mock();

  @Test
  void appliesDeferredTimeoutAndExecutorWhenExternalResourcesBecomeAvailable() throws Exception {
    ReactorResourceFactory resources = mock();
    ConnectionProvider provider = mock();
    LoopResources loops = mock();
    given(resources.getConnectionProvider()).willReturn(provider);
    given(resources.getLoopResources()).willReturn(loops);
    given(this.transport.runOn(loops)).willReturn(this.transport);
    given(this.transport.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 250)).willReturn(this.transport);
    var factory = new ReactorClientHttpRequestFactory(resources, client -> this.transport);
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setExecutor(null)).withMessage("Executor is required");
    factory.setConnectTimeout(250);
    factory.setExecutor(Runnable::run);
    verifyNoInteractions(this.transport);

    factory.start();
    assertThat(factory.isRunning()).isTrue();
    var uri = URI.create("/orders");
    assertThat(factory.createRequest(uri, HttpMethod.GET).getURI()).isEqualTo(uri);
    verify(this.transport).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 250);
    factory.stop();
    assertThat(factory.isRunning()).isFalse();
  }

  @Test
  void exposesRequestMetadataFromConvenienceConstructors() {
    var uri = URI.create("/orders");
    var defaultExecutor = new ReactorClientHttpRequest(this.transport, HttpMethod.POST, uri);
    var explicitExecutor = new ReactorClientHttpRequest(this.transport, HttpMethod.GET, uri, Runnable::run);

    assertThat(defaultExecutor.getURI()).isEqualTo(uri);
    assertThat(defaultExecutor.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(explicitExecutor.getURI()).isEqualTo(uri);
    assertThat(explicitExecutor.getMethod()).isEqualTo(HttpMethod.GET);
    verifyNoInteractions(this.transport);
  }

  @Test
  void sendsHeadersForARelativeUriAndReportsAMissingResponse() {
    var uri = URI.create("/orders");
    var request = new ReactorClientHttpRequest(this.transport, HttpMethod.GET, uri, Runnable::run, Duration.ofSeconds(5));
    given(this.transport.request(io.netty.handler.codec.http.HttpMethod.GET)).willReturn(this.sender);
    given(this.sender.uri("/orders")).willReturn(this.sender);
    HttpClientRequest outgoing = mock();
    var headers = new DefaultHttpHeaders();
    given(outgoing.requestHeaders()).willReturn(headers);
    given(this.sender.send(isA(BiFunction.class))).willAnswer(invocation -> {
      BiFunction<HttpClientRequest, NettyOutbound, Publisher<Void>> send = invocation.getArgument(0);
      Flux.from(send.apply(outgoing, mock())).blockLast(Duration.ofSeconds(5));
      return this.receiver;
    });
    given(this.receiver.responseConnection(any())).willReturn(Flux.empty());
    request.getHeaders().add("X-Values", List.of("one", "two"));

    assertThatIOException().isThrownBy(request::execute).withMessage("HTTP exchange resulted in no result");
    assertThat(headers.getAll("X-Values")).containsExactly("one", "two");
  }

  @Test
  void propagatesTheAsynchronousTransportFailure() {
    var uri = URI.create("/orders");
    var failure = new IOException("exchange failed");
    given(this.transport.request(io.netty.handler.codec.http.HttpMethod.GET)).willReturn(this.sender);
    given(this.sender.uri("/orders")).willReturn(this.sender);
    given(this.sender.send(isA(BiFunction.class))).willAnswer(invocation -> this.receiver);
    given(this.receiver.responseConnection(any())).willReturn(Flux.error(failure));
    var request = new ReactorClientHttpRequest(this.transport, HttpMethod.GET, uri, Runnable::run, Duration.ofSeconds(5));

    assertThatThrownBy(() -> request.async(Runnable::run).get(5, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class).hasCause(failure);
  }

  @Test
  void convertsWrappedIoFailuresAndKeepsOtherCauses() {
    var failure = new IOException("network error");
    var cause = new IllegalStateException("state error");
    var plain = new IllegalStateException("plain error");
    assertThat(ReactorClientHttpRequest.convertException(new IllegalStateException(failure))).isSameAs(failure);
    assertThat(ReactorClientHttpRequest.convertException(new IllegalStateException(new UncheckedIOException(failure))))
            .isSameAs(failure);
    assertThat(ReactorClientHttpRequest.convertException(new IllegalStateException("wrapped", cause)))
            .hasMessage("wrapped").hasCause(cause);
    assertThat(ReactorClientHttpRequest.convertException(plain)).hasCause(plain);
  }

  @Test
  void convertsResponseStreamSetupErrorsAndAllowsClosingTheResponse() {
    HttpClientResponse response = mock();
    Connection connection = mock();
    var failure = new IOException("failed to receive body");
    given(response.responseHeaders()).willReturn(new DefaultHttpHeaders());
    given(response.status()).willReturn(HttpResponseStatus.ACCEPTED);
    given(connection.inbound()).willThrow(new IllegalStateException(failure));
    var result = new ReactorClientHttpResponse(response, connection);

    assertThat(result.getStatusText()).isEqualTo("Accepted");
    assertThatIOException().isThrownBy(result::getBody).isSameAs(failure);
    result.close();
  }

}

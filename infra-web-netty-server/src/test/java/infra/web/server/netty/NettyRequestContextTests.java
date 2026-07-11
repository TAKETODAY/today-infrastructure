/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.server.netty;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;

import infra.context.ApplicationContext;
import infra.util.MultiValueMap;
import infra.web.DispatcherHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/19 16:56
 */
class NettyRequestContextTests {

  @Test
  void parseParameters() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();

    NettyRequestContext.parseParameters(parameters, "most-popular");
    assertThat(parameters).hasSize(1).containsKey("most-popular");

    parameters.clear();
    NettyRequestContext.parseParameters(parameters, "most-popular&name=value&name=");
    assertThat(parameters).hasSize(2).containsKeys("name", "most-popular")
            .containsValues(List.of(""), List.of("value", ""));

    parameters.clear();

    NettyRequestContext.parseParameters(parameters, "1=1&=2&n=v");
    assertThat(parameters).hasSize(3).containsKey("n").containsKey("1").containsKey("2");
  }

  @Test
  void parseParameters_withEmptyString_shouldNotAddParameters() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "");
    assertThat(parameters).isEmpty();
  }

  @Test
  void parseParameters_withSingleKeyValuePair_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key=value");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of("value"));
  }

  @Test
  void parseParameters_withMultipleKeyValuePairs_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key1=value1&key2=value2;key3=value3");
    assertThat(parameters).hasSize(3)
            .containsEntry("key1", List.of("value1"))
            .containsEntry("key2", List.of("value2"))
            .containsEntry("key3", List.of("value3"));
  }

  @Test
  void parseParameters_withEmptyValue_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key=");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of(""));
  }

  @Test
  void parseParameters_withKeyOnly_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of(""));
  }

  @Test
  void parseParameters_withMultipleEquals_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key=value=more");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of("value=more"));
  }

  @Test
  void parseParameters_withFragmentInUrl_shouldIgnoreFragmentPart() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key1=value1#key2=value2");
    assertThat(parameters).hasSize(1)
            .containsEntry("key1", List.of("value1"));
  }

  @Test
  void parseParameters_withSpecialCharactersInKeyAndValue_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    QueryStringEncoder encoder = new QueryStringEncoder("");
    encoder.addParam("key@123", "value$456");
    encoder.addParam("key#789", "value%0A");
    NettyRequestContext.parseParameters(parameters, encoder.toString().substring(1));
    assertThat(parameters).hasSize(2)
            .containsEntry("key@123", List.of("value$456"))
            .containsEntry("key#789", List.of("value%0A"));
  }

  @Test
  void parseParameters_withUTF8SpecialCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "name=张三&age=25&city=北京");

    assertThat(params)
            .hasSize(3)
            .containsEntry("name", List.of("张三"))
            .containsEntry("age", List.of("25"))
            .containsEntry("city", List.of("北京"));
  }

  @Test
  void parseParameters_withEncodedCharacters_shouldDecodeCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "title=Hello+World&q=Java%2BSpring");

    assertThat(params)
            .hasSize(2)
            .containsEntry("title", List.of("Hello World"))
            .containsEntry("q", List.of("Java+Spring"));
  }

  @Test
  void parseParameters_withMixedDelimiters_shouldParseAllParameters() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "a=1&b=2;c=3&d=4");

    assertThat(params)
            .hasSize(4)
            .containsEntry("a", List.of("1"))
            .containsEntry("b", List.of("2"))
            .containsEntry("c", List.of("3"))
            .containsEntry("d", List.of("4"));
  }

  @Test
  void parseParameters_withEmptyValues_shouldParseAsEmptyStrings() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "a=&b&c=");

    assertThat(params)
            .hasSize(3)
            .containsEntry("a", List.of(""))
            .containsEntry("b", List.of(""))
            .containsEntry("c", List.of(""));
  }

  @Test
  void parseParameters_withEqualSignInValue_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "equation=x=1&formula=a=b=c");

    assertThat(params)
            .hasSize(2)
            .containsEntry("equation", List.of("x=1"))
            .containsEntry("formula", List.of("a=b=c"));
  }

  @Test
  void parseParameters_withMoreThanMaxParams_shouldLimitParameters() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    StringBuilder query = new StringBuilder();
    for (int i = 0; i < 2000; i++) {
      query.append("p").append(i).append("=").append(i).append("&");
    }

    NettyRequestContext.parseParameters(params, query.toString());
    assertThat(params).hasSize(1024);
  }

  @Test
  void parseParameters_withFragmentIdentifier_shouldIgnoreFragment() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "a=1&b=2#fragment&c=3");

    assertThat(params)
            .hasSize(2)
            .containsEntry("a", List.of("1"))
            .containsEntry("b", List.of("2"))
            .doesNotContainKey("c");
  }

  @Test
  void parseParameters_withMalformedEncoding_shouldThrowIllegalArgumentException() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();

    assertThatThrownBy(() -> NettyRequestContext.parseParameters(params, "key=%2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unterminated escape sequence");
  }

  @Test
  void parseParameters_withPercentEncodedValues_shouldDecodeCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "name=%E5%BC%A0%E4%B8%89&city=%E5%8C%97%E4%BA%AC");

    assertThat(params)
            .hasSize(2)
            .containsEntry("name", List.of("张三"))
            .containsEntry("city", List.of("北京"));
  }

  @Test
  void parseParameters_withReservedCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "q=Java%3A%24%26%2B%2C%2F%3F%23%5B%5D%40");

    assertThat(params)
            .hasSize(1)
            .containsEntry("q", List.of("Java:$&+,/?#[]@"));
  }

  @Test
  void parseParameters_withMalformedPercentEncoding_shouldThrowException() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();

    assertThatThrownBy(() -> NettyRequestContext.parseParameters(params, "invalid=%2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unterminated escape sequence");

    assertThatThrownBy(() -> NettyRequestContext.parseParameters(params, "invalid=%2G"))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseParameters_withSpaceEncoding_shouldHandlePlusAndPercent20() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "q1=hello+world&q2=hello%20world");

    assertThat(params)
            .hasSize(2)
            .containsEntry("q1", List.of("hello world"))
            .containsEntry("q2", List.of("hello world"));
  }

  @Test
  void parseParameters_withControlCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "data=%00%01%02%03%04%05");

    assertThat(params)
            .hasSize(1)
            .containsEntry("data", List.of("\u0000\u0001\u0002\u0003\u0004\u0005"));
  }

  @Test
  void parseParameters_withMultipleEncodedDelimiters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=value%26one&key2=value%3Btwo");

    assertThat(params)
            .hasSize(2)
            .containsEntry("key1", List.of("value&one"))
            .containsEntry("key2", List.of("value;two"));
  }

  @Test
  void parseParameters_withNonAsciiCharactersInKeys_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "%E5%90%8D%E5%AD%97=zhang&%E5%B9%B4%E9%BE%84=20");

    assertThat(params)
            .hasSize(2)
            .containsEntry("名字", List.of("zhang"))
            .containsEntry("年龄", List.of("20"));
  }

  @Test
  void parseParameters_withMultipleEmptyValues_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=&key2=&key3=");

    assertThat(params)
            .hasSize(3)
            .containsEntry("key1", List.of(""))
            .containsEntry("key2", List.of(""))
            .containsEntry("key3", List.of(""));
  }

  @Test
  void parseParameters_withMultipleKeysNoValues_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1&key2&key3");

    assertThat(params)
            .hasSize(3)
            .containsEntry("key1", List.of(""))
            .containsEntry("key2", List.of(""))
            .containsEntry("key3", List.of(""));
  }

  @Test
  void parseParameters_withComplexEncodedCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key=%E2%98%83%E2%9C%88%F0%9F%98%80");

    assertThat(params)
            .hasSize(1)
            .containsEntry("key", List.of("☃✈😀"));
  }

  @Test
  void parseParameters_withNullBytes_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=abc%00def&key2=ghi%00jkl");

    assertThat(params)
            .hasSize(2)
            .containsEntry("key1", List.of("abc\u0000def"))
            .containsEntry("key2", List.of("ghi\u0000jkl"));
  }

  @Test
  void parseParameters_withLongChainedParams_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    StringBuilder longValue = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longValue.append("a");
    }
    NettyRequestContext.parseParameters(params, "key=" + longValue);

    assertThat(params)
            .hasSize(1)
            .containsEntry("key", List.of(longValue.toString()));
  }

  @Test
  void parseParameters_withConsecutiveDelimiters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=value1&&&&key2=value2;;;;key3=value3");

    assertThat(params)
            .hasSize(3)
            .containsEntry("key1", List.of("value1"))
            .containsEntry("key2", List.of("value2"))
            .containsEntry("key3", List.of("value3"));
  }

  @Test
  void parseParameters_withSemicolonAndFlagTrue_shouldTreatSemicolonAsNormal() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=value;1&key2=value;2", true);

    assertThat(params)
            .hasSize(2)
            .containsEntry("key1", List.of("value;1"))
            .containsEntry("key2", List.of("value;2"));
  }

  @Test
  void parseParameters_withMultipleSemicolonsAndFlagTrue_shouldNotSplitOnSemicolons() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key=a;b;c;d", true);

    assertThat(params)
            .hasSize(1)
            .containsEntry("key", List.of("a;b;c;d"));
  }

  @Test
  void parseParameters_withSemicolonInKeyAndFlagTrue_shouldTreatAsNormal() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key;with;semicolons=value", true);

    assertThat(params)
            .hasSize(1)
            .containsEntry("key;with;semicolons", List.of("value"));

  }

  // -- getServerName tests --

  @Test
  void getServerNameFromHostHeaderWithoutPort() {
    var request = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "example.com");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerName()).isEqualTo("example.com");
  }

  @Test
  void getServerNameFromHostHeaderWithPort() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "example.com:8080");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerName()).isEqualTo("example.com");
  }

  @Test
  void getServerNameFromHostHeaderWithIpv6WithoutPort() {
    var request = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "[::1]");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerName()).isEqualTo("[::1]");
  }

  @Test
  void getServerNameFromHostHeaderWithIpv6WithPort() {
    var request = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "[::1]:8080");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerName()).isEqualTo("[::1]");
  }

  @Test
  void getServerNameFallbackToLocalAddress() {
    var request = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var channel = mock(Channel.class);
    var pipeline = mock(ChannelPipeline.class);
    when(channel.pipeline()).thenReturn(pipeline);
    when(channel.localAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 80));

    var ctx = new NettyRequestContextStub(request, channel);
    assertThat(ctx.getServerName()).isEqualTo("192.168.1.1");
  }

  @Test
  void getServerNameFallbackToLocalhost() {
    var request = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var channel = mock(Channel.class);
    var pipeline = mock(ChannelPipeline.class);
    when(channel.pipeline()).thenReturn(pipeline);
    when(channel.localAddress()).thenReturn(null);

    var ctx = new NettyRequestContextStub(request, channel);
    assertThat(ctx.getServerName()).isEqualTo("localhost");
  }

  // -- getServerPort tests --

  @Test
  void getServerPortFromLocalAddress() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "example.com:9999");

    var channel = mock(Channel.class);
    var pipeline = mock(ChannelPipeline.class);
    when(channel.pipeline()).thenReturn(pipeline);
    when(channel.localAddress()).thenReturn(new InetSocketAddress("0.0.0.0", 3000));

    var ctx = new NettyRequestContextStub(request, channel);
    assertThat(ctx.getServerPort()).isEqualTo(3000);
  }

  @Test
  void getServerPortFromHostHeaderWhenNoLocalAddress() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "example.com:8080");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerPort()).isEqualTo(8080);
  }

  @Test
  void getServerPortFromHostHeaderWithIpv6WhenNoLocalAddress() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "[::1]:9090");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerPort()).isEqualTo(9090);
  }

  @Test
  void getServerPortDefaultsTo80WhenNoLocalAddressAndNoPortInHostHeader() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "example.com");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerPort()).isEqualTo(80);
  }

  @Test
  void getServerPortDefaultsTo80WhenNoAddressAndNoHostHeader() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerPort()).isEqualTo(80);
  }

  @Test
  void getServerPortDefaultsTo443WhenSecureAndNoAddressAndNoHostHeader() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var ctx = new SecureNettyRequestContextStub(request, null);
    assertThat(ctx.getServerPort()).isEqualTo(443);
  }

  @Test
  void getServerPortWithMalformedPortInHostHeader_fallsBackToDefault() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    request.headers().set(io.netty.handler.codec.http.HttpHeaderNames.HOST, "example.com:abc");

    var ctx = new NettyRequestContextStub(request, null);
    assertThat(ctx.getServerPort()).isEqualTo(80);
  }

  // -- parsePortFromHostHeader tests --

  @Test
  void parsePortFromHostHeaderWithIpv4AndPort() {
    assertThat(NettyRequestContext.parsePortFromHostHeader("example.com:8080")).isEqualTo(8080);
  }

  @Test
  void parsePortFromHostHeaderWithIpv4WithoutPort() {
    assertThat(NettyRequestContext.parsePortFromHostHeader("example.com")).isEqualTo(-1);
  }

  @Test
  void parsePortFromHostHeaderWithIpv6AndPort() {
    assertThat(NettyRequestContext.parsePortFromHostHeader("[::1]:9090")).isEqualTo(9090);
  }

  @Test
  void parsePortFromHostHeaderWithIpv6WithoutPort() {
    assertThat(NettyRequestContext.parsePortFromHostHeader("[::1]")).isEqualTo(-1);
  }

  @Test
  void parsePortFromHostHeaderWithIpv6AndMalformedPort() {
    assertThat(NettyRequestContext.parsePortFromHostHeader("[::1]:abc")).isEqualTo(-1);
  }

  // -- onResponseCommitted tests --

  @Test
  void onResponseCommittedFiredOnFlush() throws IOException {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var ctx = new OnCommittedTrackingStub(request, null);
    assertThat(ctx.isCommitted()).isFalse();
    assertThat(ctx.onCommittedCallCount).isEqualTo(0);

    ctx.flush();

    assertThat(ctx.isCommitted()).isTrue();
    assertThat(ctx.onCommittedCallCount).isEqualTo(1);
  }

  @Test
  void onResponseCommittedFiredOnlyOnce() throws IOException {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var ctx = new OnCommittedTrackingStub(request, null);

    ctx.flush();
    ctx.flush();
    ctx.flush();

    assertThat(ctx.onCommittedCallCount).isEqualTo(1);
  }

  @Test
  void onResponseCommittedFiredOnSendRedirect() throws IOException {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var ctx = new OnCommittedTrackingStub(request, null);

    ctx.sendRedirect("/redirect");

    assertThat(ctx.isCommitted()).isTrue();
    assertThat(ctx.onCommittedCallCount).isEqualTo(1);
  }

  @Test
  void onResponseCommittedFiredOnOutputStreamWriteThenFlush() throws IOException {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var ctx = new OnCommittedTrackingStub(request, null);

    ctx.getOutputStream().write('X');
    assertThat(ctx.isCommitted()).isFalse();
    assertThat(ctx.onCommittedCallCount).isEqualTo(0);

    ctx.flush();
    assertThat(ctx.isCommitted()).isTrue();
    assertThat(ctx.onCommittedCallCount).isEqualTo(1);
  }

  @Test
  void onResponseCommittedNotCalledOnResetAfterFlushWithoutCommit() throws IOException {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");

    var ctx = new OnCommittedTrackingStub(request, null);

    ctx.getOutputStream();
    // flush not called, so response not committed
    ctx.reset();

    assertThat(ctx.isCommitted()).isFalse();
    assertThat(ctx.onCommittedCallCount).isEqualTo(0);
  }

  @Test
  void committedCallback_FiresWhenFlushed() throws IOException {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    var ctx = new NettyRequestContextStub(request, null);
    var committed = new boolean[1];

    ctx.registerCommittedCallback(() -> committed[0] = true);
    ctx.flush();

    assertThat(committed[0]).isTrue();
  }

  @Test
  void committingCallback_FiresWhenFlushed() throws IOException {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.GET, "/test");
    var ctx = new NettyRequestContextStub(request, null);
    var committed = new boolean[1];

    ctx.registerCommittingCallback(() -> committed[0] = true);
    ctx.flush();

    assertThat(committed[0]).isTrue();
  }

  // -- stub --

  private static Channel mockChannel() {
    return new EmbeddedChannel();
  }

  private static class NettyRequestContextStub extends NettyRequestContext {

    NettyRequestContextStub(io.netty.handler.codec.http.HttpRequest request, Channel channel) {
      this(request, channel, NettyRequestConfig.forBuilder(false));
    }

    NettyRequestContextStub(io.netty.handler.codec.http.HttpRequest request, Channel channel, NettyRequestConfig.Builder configBuilder) {
      super(mock(ApplicationContext.class),
              channel != null ? channel : mockChannel(),
              request,
              configBuilder
                      .sendErrorHandler((req, msg) -> { })
                      .multipartParser(mock(infra.web.multipart.MultipartParser.class))
                      .build(),
              mock(DispatcherHandler.class));
    }

    @Override
    public long getContentLength() {
      return 0;
    }

    @Override
    protected InputStream createInputStream() throws IOException {
      return InputStream.nullInputStream();
    }
  }

  private static class SecureNettyRequestContextStub extends NettyRequestContextStub {

    SecureNettyRequestContextStub(io.netty.handler.codec.http.HttpRequest request, Channel channel) {
      super(request, channel, NettyRequestConfig.forBuilder(true));
    }
  }

  private static class OnCommittedTrackingStub extends NettyRequestContextStub {

    int onCommittedCallCount;

    OnCommittedTrackingStub(io.netty.handler.codec.http.HttpRequest request, Channel channel) {
      super(request, channel);
      registerCommittedCallback(() -> onCommittedCallCount++);
    }
  }

}
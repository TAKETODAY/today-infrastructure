/*
 * Copyright 2002-present the original author or authors.
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

package infra.messaging.rsocket.annotation.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import infra.core.ReactiveAdapterRegistry;
import infra.core.codec.ByteArrayDecoder;
import infra.core.codec.ByteArrayEncoder;
import infra.core.codec.ByteBufferDecoder;
import infra.core.codec.ByteBufferEncoder;
import infra.core.codec.CharSequenceEncoder;
import infra.core.codec.StringDecoder;
import infra.core.io.buffer.NettyDataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.messaging.Message;
import infra.messaging.handler.CompositeMessageCondition;
import infra.messaging.handler.DestinationPatternsMessageCondition;
import infra.messaging.handler.HandlerMethod;
import infra.messaging.handler.annotation.MessageMapping;
import infra.messaging.rsocket.DefaultMetadataExtractor;
import infra.messaging.rsocket.RSocketStrategies;
import infra.messaging.rsocket.annotation.ConnectMapping;
import infra.messaging.support.MessageBuilder;
import infra.messaging.support.MessageHeaderAccessor;
import infra.util.AntPathMatcher;
import infra.util.ObjectUtils;
import infra.util.RouteMatcher;
import infra.util.SimpleRouteMatcher;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.rsocket.frame.FrameType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RSocketMessageHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class RSocketMessageHandlerTests {

  @Test
  void getRSocketStrategies() {
    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setDecoders(Collections.singletonList(new ByteArrayDecoder()));
    handler.setEncoders(Collections.singletonList(new ByteArrayEncoder()));
    handler.setRouteMatcher(new SimpleRouteMatcher(new AntPathMatcher()));
    handler.setMetadataExtractor(new DefaultMetadataExtractor());
    handler.setReactiveAdapterRegistry(new ReactiveAdapterRegistry());

    RSocketStrategies strategies = handler.getRSocketStrategies();
    assertThat(strategies).isNotNull();
    assertThat(strategies.encoders()).isEqualTo(handler.getEncoders());
    assertThat(strategies.decoders()).isEqualTo(handler.getDecoders());
    assertThat(strategies.routeMatcher()).isSameAs(handler.getRouteMatcher());
    assertThat(strategies.metadataExtractor()).isSameAs(handler.getMetadataExtractor());
    assertThat(strategies.reactiveAdapterRegistry()).isSameAs(handler.getReactiveAdapterRegistry());
  }

  @Test
  void setRSocketStrategies() {
    RSocketStrategies strategies = RSocketStrategies.builder()
            .encoder(new ByteArrayEncoder())
            .decoder(new ByteArrayDecoder())
            .routeMatcher(new SimpleRouteMatcher(new AntPathMatcher()))
            .metadataExtractor(new DefaultMetadataExtractor())
            .reactiveAdapterStrategy(new ReactiveAdapterRegistry())
            .build();

    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setRSocketStrategies(strategies);

    assertThat(handler.getEncoders()).isEqualTo(strategies.encoders());
    assertThat(handler.getDecoders()).isEqualTo(strategies.decoders());
    assertThat(handler.getRouteMatcher()).isSameAs(strategies.routeMatcher());
    assertThat(handler.getMetadataExtractor()).isSameAs(strategies.metadataExtractor());
    assertThat(handler.getReactiveAdapterRegistry()).isSameAs(strategies.reactiveAdapterRegistry());
  }

  @Test
  void getRSocketStrategiesReflectsCurrentState() {

    RSocketMessageHandler handler = new RSocketMessageHandler();

    // 1. Set properties
    handler.setDecoders(Collections.singletonList(new ByteArrayDecoder()));
    handler.setEncoders(Collections.singletonList(new ByteArrayEncoder()));
    handler.setRouteMatcher(new SimpleRouteMatcher(new AntPathMatcher()));
    handler.setMetadataExtractor(new DefaultMetadataExtractor());
    handler.setReactiveAdapterRegistry(new ReactiveAdapterRegistry());

    RSocketStrategies strategies = handler.getRSocketStrategies();
    assertThat(strategies.encoders()).isEqualTo(handler.getEncoders());
    assertThat(strategies.decoders()).isEqualTo(handler.getDecoders());
    assertThat(strategies.routeMatcher()).isSameAs(handler.getRouteMatcher());
    assertThat(strategies.metadataExtractor()).isSameAs(handler.getMetadataExtractor());
    assertThat(strategies.reactiveAdapterRegistry()).isSameAs(handler.getReactiveAdapterRegistry());

    // 2. Set properties again
    handler.setDecoders(Collections.singletonList(StringDecoder.allMimeTypes()));
    handler.setEncoders(Collections.singletonList(CharSequenceEncoder.allMimeTypes()));
    handler.setRouteMatcher(new SimpleRouteMatcher(new AntPathMatcher()));
    handler.setMetadataExtractor(new DefaultMetadataExtractor());
    handler.setReactiveAdapterRegistry(new ReactiveAdapterRegistry());
    handler.afterPropertiesSet();

    strategies = handler.getRSocketStrategies();
    assertThat(strategies.encoders()).isEqualTo(handler.getEncoders());
    assertThat(strategies.decoders()).isEqualTo(handler.getDecoders());
    assertThat(strategies.routeMatcher()).isSameAs(handler.getRouteMatcher());
    assertThat(strategies.metadataExtractor()).isSameAs(handler.getMetadataExtractor());
    assertThat(strategies.reactiveAdapterRegistry()).isSameAs(handler.getReactiveAdapterRegistry());
  }

  @Test
  void metadataExtractorWithExplicitlySetDecoders() {
    DefaultMetadataExtractor extractor = new DefaultMetadataExtractor(StringDecoder.allMimeTypes());

    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setDecoders(Arrays.asList(new ByteArrayDecoder(), new ByteBufferDecoder()));
    handler.setEncoders(Collections.singletonList(new ByteBufferEncoder()));
    handler.setMetadataExtractor(extractor);
    handler.afterPropertiesSet();

    assertThat(((DefaultMetadataExtractor) handler.getMetadataExtractor()).getDecoders()).hasSize(1);
  }

  @Test
  void mappings() {
    testMapping(new SimpleController(), "path");
    testMapping(new TypeLevelMappingController(), "base.path");
    testMapping(new HandleAllController());
  }

  private static void testMapping(Object controller, String... expectedPatterns) {
    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setDecoders(Collections.singletonList(StringDecoder.allMimeTypes()));
    handler.setEncoders(Collections.singletonList(CharSequenceEncoder.allMimeTypes()));
    handler.setHandlers(Collections.singletonList(controller));
    handler.afterPropertiesSet();

    Map<CompositeMessageCondition, HandlerMethod> map = handler.getHandlerMethods();
    assertThat(map).hasSize(1);

    CompositeMessageCondition condition = map.entrySet().iterator().next().getKey();
    RSocketFrameTypeMessageCondition c1 = condition.getCondition(RSocketFrameTypeMessageCondition.class);
    assertThat(c1.getFrameTypes()).contains(FrameType.SETUP, FrameType.METADATA_PUSH);

    DestinationPatternsMessageCondition c2 = condition.getCondition(DestinationPatternsMessageCondition.class);
    if (ObjectUtils.isEmpty(expectedPatterns)) {
      assertThat(c2.getPatterns()).isEmpty();
    }
    else {
      assertThat(c2.getPatterns()).contains(expectedPatterns);
    }
  }

  @Test
  void rejectConnectMappingMethodsThatCanReply() {

    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setHandlers(Collections.singletonList(new InvalidConnectMappingController()));
    assertThatThrownBy(handler::afterPropertiesSet)
            .hasMessage("Invalid @ConnectMapping method. " +
                    "Return type must be void or a void async type: " +
                    "public java.lang.String infra.messaging.rsocket.annotation.support." +
                    "RSocketMessageHandlerTests$InvalidConnectMappingController.connectString()");

    handler = new RSocketMessageHandler();
    handler.setHandlers(Collections.singletonList(new AnotherInvalidConnectMappingController()));
    assertThatThrownBy(handler::afterPropertiesSet)
            .hasMessage("Invalid @ConnectMapping method. " +
                    "Return type must be void or a void async type: " +
                    "public reactor.core.publisher.Mono<java.lang.String> " +
                    "infra.messaging.rsocket.annotation.support." +
                    "RSocketMessageHandlerTests$AnotherInvalidConnectMappingController.connectString()");
  }

  @Test
  void ignoreFireAndForgetToHandlerThatCanReply() {

    InteractionMismatchController controller = new InteractionMismatchController();

    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setHandlers(Collections.singletonList(controller));
    handler.afterPropertiesSet();

    MessageHeaderAccessor headers = new MessageHeaderAccessor();
    headers.setLeaveMutable(true);
    RouteMatcher.Route route = handler.getRouteMatcher().parseRoute("mono-string");
    headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, route);
    headers.setHeader(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER, FrameType.REQUEST_FNF);
    Message<?> message = MessageBuilder.createMessage(Mono.empty(), headers.getMessageHeaders());

    // Simply dropped and logged (error cannot propagate to client)
    StepVerifier.create(handler.handleMessage(message)).expectComplete().verify();
    assertThat(controller.invokeCount).isEqualTo(0);
  }

  @Test
  void rejectRequestResponseToStreamingHandler() {

    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setHandlers(Collections.singletonList(new InteractionMismatchController()));
    handler.afterPropertiesSet();

    MessageHeaderAccessor headers = new MessageHeaderAccessor();
    headers.setLeaveMutable(true);
    RouteMatcher.Route route = handler.getRouteMatcher().parseRoute("flux-string");
    headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, route);
    headers.setHeader(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER, FrameType.REQUEST_RESPONSE);
    Message<?> message = MessageBuilder.createMessage(Mono.empty(), headers.getMessageHeaders());

    StepVerifier.create(handler.handleMessage(message))
            .expectErrorMessage(
                    "Destination 'flux-string' does not support REQUEST_RESPONSE. " +
                            "Supported interaction(s): [REQUEST_STREAM]")
            .verify();
  }

  @Test
  void handleNoMatch() {

    testHandleNoMatch(FrameType.SETUP);
    testHandleNoMatch(FrameType.METADATA_PUSH);
    testHandleNoMatch(FrameType.REQUEST_FNF);

    assertThatThrownBy(() -> testHandleNoMatch(FrameType.REQUEST_RESPONSE))
            .hasMessage("No handler for destination 'path'");
  }

  private static void testHandleNoMatch(FrameType frameType) {
    testHandleNoMatch(frameType, "");
  }

  private static void testHandleNoMatch(FrameType frameType, Object payload) {
    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setDecoders(Collections.singletonList(StringDecoder.allMimeTypes()));
    handler.setEncoders(Collections.singletonList(CharSequenceEncoder.allMimeTypes()));
    handler.afterPropertiesSet();

    RouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher("."));
    RouteMatcher.Route route = matcher.parseRoute("path");

    MessageHeaderAccessor headers = new MessageHeaderAccessor();
    headers.setHeader(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER, frameType);
    Message<Object> message = MessageBuilder.createMessage(payload, headers.getMessageHeaders());

    handler.handleNoMatch(route, message);
  }

  @Test
  void handleNoMatchWithNettyBufferPayload() {

    testHandleNoMatchBuffer(FrameType.SETUP, true);
    testHandleNoMatchBuffer(FrameType.METADATA_PUSH, false);
    testHandleNoMatchBuffer(FrameType.REQUEST_FNF, false);

    assertThatThrownBy(() -> testHandleNoMatchBuffer(FrameType.REQUEST_RESPONSE, false))
            .hasMessage("No handler for destination 'path'");
  }

  private static void testHandleNoMatchBuffer(FrameType frameType, boolean expectReleased) {
    NettyDataBufferFactory factory = new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT);
    NettyDataBuffer buf = factory.allocateBuffer(5);
    buf.write("hello", StandardCharsets.UTF_8);

    assertThat(buf.getNativeBuffer().refCnt()).as(frameType + " refCnt").isOne();

    try {
      testHandleNoMatch(frameType, buf);
    }
    finally {
      if (expectReleased) {
        assertThat(buf.getNativeBuffer().refCnt()).as(frameType + " is released").isZero();
      }
      else {
        assertThat(buf.getNativeBuffer().refCnt()).as("is not released").isOne();
      }
    }
  }

  private static class SimpleController {

    @ConnectMapping("path")
    public void handle() {
    }
  }

  @MessageMapping("base")
  private static class TypeLevelMappingController {

    @ConnectMapping("path")
    public void handleWithPatterns() {
    }
  }

  private static class HandleAllController {

    @ConnectMapping
    public void handleAll() {
    }
  }

  private static class InvalidConnectMappingController {

    @ConnectMapping
    public String connectString() {
      return "";
    }
  }

  private static class AnotherInvalidConnectMappingController {

    @ConnectMapping
    public Mono<String> connectString() {
      return Mono.empty();
    }
  }

  private static class InteractionMismatchController {

    private int invokeCount;

    @MessageMapping("mono-string")
    public Mono<String> messageMonoString() {
      this.invokeCount++;
      return Mono.empty();
    }

    @MessageMapping("flux-string")
    public Flux<String> messageFluxString() {
      this.invokeCount++;
      return Flux.empty();
    }
  }

}

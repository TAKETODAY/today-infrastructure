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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.messaging.simp;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.lang.Assert;
import infra.messaging.Message;
import infra.messaging.MessageChannel;
import infra.messaging.MessageDeliveryException;
import infra.messaging.MessageHeaders;
import infra.messaging.MessagingException;
import infra.messaging.core.AbstractMessageSendingTemplate;
import infra.messaging.core.MessagePostProcessor;
import infra.messaging.simp.user.UserDestinationMessageHandler;
import infra.messaging.simp.user.UserDestinationResolver;
import infra.messaging.support.MessageBuilder;
import infra.messaging.support.MessageHeaderAccessor;
import infra.messaging.support.MessageHeaderInitializer;
import infra.messaging.support.NativeMessageHeaderAccessor;
import infra.util.StringUtils;

/**
 * An implementation of
 * {@link SimpMessageSendingOperations}.
 *
 * <p>Also provides methods for sending messages to a user. See
 * {@link UserDestinationResolver
 * UserDestinationResolver} for more on user destinations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SimpMessagingTemplate extends AbstractMessageSendingTemplate<String>
        implements SimpMessageSendingOperations {

  private final MessageChannel messageChannel;

  private String destinationPrefix = "/user/";

  private volatile long sendTimeout = -1;

  private @Nullable MessageHeaderInitializer headerInitializer;

  /**
   * Create a new {@link SimpMessagingTemplate} instance.
   *
   * @param messageChannel the message channel (never {@code null})
   */
  public SimpMessagingTemplate(MessageChannel messageChannel) {
    Assert.notNull(messageChannel, "MessageChannel is required");
    this.messageChannel = messageChannel;
  }

  /**
   * Return the configured message channel.
   */
  public MessageChannel getMessageChannel() {
    return this.messageChannel;
  }

  /**
   * Configure the prefix to use for destinations targeting a specific user.
   * <p>The default value is "/user/".
   *
   * @see UserDestinationMessageHandler
   */
  public void setUserDestinationPrefix(String prefix) {
    Assert.hasText(prefix, "User destination prefix must not be empty");
    this.destinationPrefix = (prefix.endsWith("/") ? prefix : prefix + "/");

  }

  /**
   * Return the configured user destination prefix.
   */
  public String getUserDestinationPrefix() {
    return this.destinationPrefix;
  }

  /**
   * Specify the timeout value to use for send operations (in milliseconds).
   */
  public void setSendTimeout(long sendTimeout) {
    this.sendTimeout = sendTimeout;
  }

  /**
   * Return the configured send timeout (in milliseconds).
   */
  public long getSendTimeout() {
    return this.sendTimeout;
  }

  /**
   * Configure a {@link MessageHeaderInitializer} to apply to the headers of all
   * messages created through the {@code SimpMessagingTemplate}.
   * <p>By default, this property is not set.
   */
  public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
    this.headerInitializer = headerInitializer;
  }

  /**
   * Return the configured header initializer.
   */
  public @Nullable MessageHeaderInitializer getHeaderInitializer() {
    return this.headerInitializer;
  }

  /**
   * If the headers of the given message already contain a
   * {@link SimpMessageHeaderAccessor#DESTINATION_HEADER
   * SimpMessageHeaderAccessor#DESTINATION_HEADER} then the message is sent without
   * further changes.
   * <p>If a destination header is not already present ,the message is sent
   * to the configured {@link AbstractMessageSendingTemplate#setDefaultDestination(Object)
   * defaultDestination} or an {@code IllegalStateException} is raised if that isn't
   * configured.
   *
   * @param message the message to send (never {@code null})
   */
  @Override
  public void send(Message<?> message) {
    Assert.notNull(message, "Message is required");
    String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
    if (destination != null) {
      sendInternal(message);
      return;
    }
    doSend(getRequiredDefaultDestination(), message);
  }

  @Override
  protected void doSend(String destination, Message<?> message) {
    Assert.notNull(destination, "Destination is required");

    SimpMessageHeaderAccessor simpAccessor =
            MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

    if (simpAccessor != null) {
      if (simpAccessor.isMutable()) {
        simpAccessor.setDestination(destination);
        simpAccessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
        // ImmutableMessageChannelInterceptor will make it immutable
        sendInternal(message);
        return;
      }
      else {
        // Try and keep the original accessor type
        simpAccessor = (SimpMessageHeaderAccessor) MessageHeaderAccessor.getMutableAccessor(message);
        initHeaders(simpAccessor);
      }
    }
    else {
      simpAccessor = SimpMessageHeaderAccessor.wrap(message);
      initHeaders(simpAccessor);
    }

    simpAccessor.setDestination(destination);
    simpAccessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
    message = MessageBuilder.createMessage(message.getPayload(), simpAccessor.getMessageHeaders());
    sendInternal(message);
  }

  private void sendInternal(Message<?> message) {
    String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
    Assert.notNull(destination, "Destination header required");

    long timeout = this.sendTimeout;
    boolean sent = (timeout >= 0 ? this.messageChannel.send(message, timeout) : this.messageChannel.send(message));

    if (!sent) {
      throw new MessageDeliveryException(message,
              "Failed to send message to destination '" + destination + "' within timeout: " + timeout);
    }
  }

  private void initHeaders(SimpMessageHeaderAccessor simpAccessor) {
    if (getHeaderInitializer() != null) {
      getHeaderInitializer().initHeaders(simpAccessor);
    }
  }

  @Override
  public void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {
    convertAndSendToUser(user, destination, payload, (MessagePostProcessor) null);
  }

  @Override
  public void convertAndSendToUser(String user, String destination, Object payload,
          @Nullable Map<String, Object> headers) throws MessagingException {

    convertAndSendToUser(user, destination, payload, headers, null);
  }

  @Override
  public void convertAndSendToUser(String user, String destination, Object payload,
          @Nullable MessagePostProcessor postProcessor) throws MessagingException {

    convertAndSendToUser(user, destination, payload, null, postProcessor);
  }

  @Override
  public void convertAndSendToUser(String user, String destination, Object payload,
          @Nullable Map<String, Object> headers, @Nullable MessagePostProcessor postProcessor)
          throws MessagingException {

    Assert.notNull(user, "User is required");
    String username = user;
    Assert.isTrue(!user.contains("%2F"), () -> "Invalid sequence \"%2F\" in user name: " + username);
    user = StringUtils.replace(user, "/", "%2F");
    destination = destination.startsWith("/") ? destination : "/" + destination;
    super.convertAndSend(this.destinationPrefix + user + destination, payload, headers, postProcessor);
  }

  /**
   * Creates a new map and puts the given headers under the key
   * {@link NativeMessageHeaderAccessor#NATIVE_HEADERS NATIVE_HEADERS}.
   * <p>Effectively treats the input header map as headers to be sent out to the
   * destination.
   * <p>However if the given headers already contain the key {@code NATIVE_HEADERS}
   * then the same headers instance is returned without changes.
   * <p>Also if the given headers were prepared and obtained with
   * {@link SimpMessageHeaderAccessor#getMessageHeaders()} then the same headers
   * instance is also returned without changes.
   */
  @Override
  protected Map<String, Object> processHeadersToSend(@Nullable Map<String, Object> headers) {
    if (headers == null) {
      SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
      initHeaders(headerAccessor);
      headerAccessor.setLeaveMutable(true);
      return headerAccessor.getMessageHeaders();
    }
    if (headers.containsKey(NativeMessageHeaderAccessor.NATIVE_HEADERS)) {
      return headers;
    }
    if (headers instanceof MessageHeaders messageHeaders) {
      SimpMessageHeaderAccessor accessor =
              MessageHeaderAccessor.getAccessor(messageHeaders, SimpMessageHeaderAccessor.class);
      if (accessor != null) {
        return headers;
      }
    }

    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    initHeaders(headerAccessor);
    headers.forEach((key, value) -> headerAccessor.setNativeHeader(key, (value != null ? value.toString() : null)));
    return headerAccessor.getMessageHeaders();
  }

}

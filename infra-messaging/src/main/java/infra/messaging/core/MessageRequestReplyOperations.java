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

package infra.messaging.core;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.messaging.Message;
import infra.messaging.MessagingException;
import infra.messaging.converter.MessageConverter;

/**
 * Operations for sending messages to and receiving the reply from a destination.
 *
 * @param <D> the type of destination
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see GenericMessagingTemplate
 * @since 5.0
 */
public interface MessageRequestReplyOperations<D> {

  /**
   * Send a request message and receive the reply from a default destination.
   *
   * @param requestMessage the message to send
   * @return the reply, possibly {@code null} if the message could not be received,
   * for example due to a timeout
   */
  @Nullable Message<?> sendAndReceive(Message<?> requestMessage) throws MessagingException;

  /**
   * Send a request message and receive the reply from the given destination.
   *
   * @param destination the target destination
   * @param requestMessage the message to send
   * @return the reply, possibly {@code null} if the message could not be received,
   * for example due to a timeout
   */
  @Nullable Message<?> sendAndReceive(D destination, Message<?> requestMessage) throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter}, send
   * it as a {@link Message} to a default destination, receive the reply and convert
   * its body of the specified target class.
   *
   * @param request payload for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   */
  <T> @Nullable T convertSendAndReceive(Object request, Class<T> targetClass) throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter}, send
   * it as a {@link Message} to the given destination, receive the reply and convert
   * its body of the specified target class.
   *
   * @param destination the target destination
   * @param request payload for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   */
  <T> @Nullable T convertSendAndReceive(D destination, Object request, Class<T> targetClass) throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter}, send
   * it as a {@link Message} with the given headers, to the specified destination,
   * receive the reply and convert its body of the specified target class.
   *
   * @param request payload for the request message to send
   * @param headers the headers for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   * @since 5.0
   */
  <T> @Nullable T convertSendAndReceive(Object request, @Nullable Map<String, Object> headers, Class<T> targetClass)
          throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter}, send
   * it as a {@link Message} with the given headers, to the specified destination,
   * receive the reply and convert its body of the specified target class.
   *
   * @param destination the target destination
   * @param request payload for the request message to send
   * @param headers the headers for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   */
  <T> @Nullable T convertSendAndReceive(
          D destination, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass)
          throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter},
   * apply the given post-processor and send the resulting {@link Message} to a
   * default destination, receive the reply and convert its body of the given
   * target class.
   *
   * @param request payload for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @param requestPostProcessor post-process to apply to the request message
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   */
  <T> @Nullable T convertSendAndReceive(
          Object request, Class<T> targetClass, @Nullable MessagePostProcessor requestPostProcessor)
          throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter},
   * apply the given post-processor and send the resulting {@link Message} to the
   * given destination, receive the reply and convert its body of the given
   * target class.
   *
   * @param destination the target destination
   * @param request payload for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @param requestPostProcessor post-process to apply to the request message
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   */
  <T> @Nullable T convertSendAndReceive(D destination, Object request, Class<T> targetClass,
          MessagePostProcessor requestPostProcessor) throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter},
   * wrap it as a message with the given headers, apply the given post-processor
   * and send the resulting {@link Message} to the specified destination, receive
   * the reply and convert its body of the given target class.
   *
   * @param request payload for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @param requestPostProcessor post-process to apply to the request message
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   * @since 5.0
   */
  <T> @Nullable T convertSendAndReceive(
          Object request, @Nullable Map<String, Object> headers, Class<T> targetClass,
          @Nullable MessagePostProcessor requestPostProcessor) throws MessagingException;

  /**
   * Convert the given request Object to serialized form, possibly using a
   * {@link MessageConverter},
   * wrap it as a message with the given headers, apply the given post-processor
   * and send the resulting {@link Message} to the specified destination, receive
   * the reply and convert its body of the given target class.
   *
   * @param destination the target destination
   * @param request payload for the request message to send
   * @param targetClass the target type to convert the payload of the reply to
   * @param requestPostProcessor post-process to apply to the request message
   * @return the payload of the reply message, possibly {@code null} if the message
   * could not be received, for example due to a timeout
   */
  <T> @Nullable T convertSendAndReceive(
          D destination, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass,
          @Nullable MessagePostProcessor requestPostProcessor) throws MessagingException;

}

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

package infra.messaging.simp.config;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import infra.lang.Assert;
import infra.messaging.MessageChannel;
import infra.messaging.SubscribableChannel;
import infra.messaging.simp.broker.AbstractBrokerMessageHandler;

/**
 * Base class for message broker registration classes.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractBrokerRegistration {

  private final SubscribableChannel clientInboundChannel;

  private final MessageChannel clientOutboundChannel;

  private final List<String> destinationPrefixes;

  /**
   * Create a new broker registration.
   *
   * @param clientInboundChannel the inbound channel
   * @param clientOutboundChannel the outbound channel
   * @param destinationPrefixes the destination prefixes
   */
  public AbstractBrokerRegistration(SubscribableChannel clientInboundChannel,
          MessageChannel clientOutboundChannel, String @Nullable [] destinationPrefixes) {

    Assert.notNull(clientInboundChannel, "'clientInboundChannel' is required");
    Assert.notNull(clientOutboundChannel, "'clientOutboundChannel' is required");

    this.clientInboundChannel = clientInboundChannel;
    this.clientOutboundChannel = clientOutboundChannel;

    this.destinationPrefixes = (destinationPrefixes != null ?
            Arrays.asList(destinationPrefixes) : Collections.emptyList());
  }

  protected SubscribableChannel getClientInboundChannel() {
    return this.clientInboundChannel;
  }

  protected MessageChannel getClientOutboundChannel() {
    return this.clientOutboundChannel;
  }

  protected Collection<String> getDestinationPrefixes() {
    return this.destinationPrefixes;
  }

  protected abstract AbstractBrokerMessageHandler getMessageHandler(SubscribableChannel brokerChannel);

}

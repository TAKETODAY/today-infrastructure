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

package infra.messaging.simp.annotation.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.SmartLifecycle;
import infra.context.expression.EmbeddedValueResolverAware;
import infra.core.StringValueResolver;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.conversion.ConversionService;
import infra.format.support.DefaultFormattingConversionService;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.messaging.Message;
import infra.messaging.MessageChannel;
import infra.messaging.SubscribableChannel;
import infra.messaging.converter.ByteArrayMessageConverter;
import infra.messaging.converter.CompositeMessageConverter;
import infra.messaging.converter.MessageConverter;
import infra.messaging.converter.StringMessageConverter;
import infra.messaging.core.AbstractMessageSendingTemplate;
import infra.messaging.handler.DestinationPatternsMessageCondition;
import infra.messaging.handler.HandlerMethod;
import infra.messaging.handler.annotation.MessageMapping;
import infra.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import infra.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import infra.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import infra.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import infra.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import infra.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import infra.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import infra.messaging.handler.invocation.AbstractMethodMessageHandler;
import infra.messaging.handler.invocation.CompletableFutureReturnValueHandler;
import infra.messaging.handler.invocation.HandlerMethodArgumentResolver;
import infra.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import infra.messaging.handler.invocation.HandlerMethodReturnValueHandlerComposite;
import infra.messaging.handler.invocation.ReactiveReturnValueHandler;
import infra.messaging.simp.SimpAttributesContextHolder;
import infra.messaging.simp.SimpLogging;
import infra.messaging.simp.SimpMessageHeaderAccessor;
import infra.messaging.simp.SimpMessageMappingInfo;
import infra.messaging.simp.SimpMessageSendingOperations;
import infra.messaging.simp.SimpMessageTypeMessageCondition;
import infra.messaging.simp.SimpMessagingTemplate;
import infra.messaging.simp.annotation.SubscribeMapping;
import infra.messaging.support.MessageHeaderAccessor;
import infra.messaging.support.MessageHeaderInitializer;
import infra.stereotype.Controller;
import infra.util.AntPathMatcher;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.PathMatcher;
import infra.validation.Validator;

/**
 * A handler for messages delegating to {@link MessageMapping @MessageMapping}
 * and {@link SubscribeMapping @SubscribeMapping} annotated methods.
 *
 * <p>Supports Ant-style path patterns with template variables.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 5.0
 */
public class SimpAnnotationMethodMessageHandler extends AbstractMethodMessageHandler<SimpMessageMappingInfo>
        implements EmbeddedValueResolverAware, SmartLifecycle {

  private static final boolean REACTOR_PRESENT = ClassUtils.isPresent(
          "reactor.core.publisher.Flux", SimpAnnotationMethodMessageHandler.class.getClassLoader());

  private final SubscribableChannel clientInboundChannel;

  private final SimpMessageSendingOperations clientMessagingTemplate;

  private final SimpMessageSendingOperations brokerTemplate;

  private MessageConverter messageConverter;

  private ConversionService conversionService = new DefaultFormattingConversionService();

  private PathMatcher pathMatcher = new AntPathMatcher();

  private boolean slashPathSeparator = true;

  private @Nullable Validator validator;

  private @Nullable StringValueResolver valueResolver;

  private @Nullable MessageHeaderInitializer headerInitializer;

  private @Nullable Predicate<String> headerFilter;

  private @Nullable Integer phase;

  private volatile boolean running;

  private final Object lifecycleMonitor = new Object();

  /**
   * Create an instance of SimpAnnotationMethodMessageHandler with the given
   * message channels and broker messaging template.
   *
   * @param clientInboundChannel the channel for receiving messages from clients (for example, WebSocket clients)
   * @param clientOutboundChannel the channel for messages to clients (for example, WebSocket clients)
   * @param brokerTemplate a messaging template to send application messages to the broker
   */
  public SimpAnnotationMethodMessageHandler(SubscribableChannel clientInboundChannel,
          MessageChannel clientOutboundChannel, SimpMessageSendingOperations brokerTemplate) {

    Assert.notNull(clientInboundChannel, "clientInboundChannel is required");
    Assert.notNull(clientOutboundChannel, "clientOutboundChannel is required");
    Assert.notNull(brokerTemplate, "brokerTemplate is required");

    this.clientInboundChannel = clientInboundChannel;
    this.clientMessagingTemplate = new SimpMessagingTemplate(clientOutboundChannel);
    this.brokerTemplate = brokerTemplate;

    Collection<MessageConverter> converters = new ArrayList<>();
    converters.add(new StringMessageConverter());
    converters.add(new ByteArrayMessageConverter());
    this.messageConverter = new CompositeMessageConverter(converters);
  }

  /**
   * {@inheritDoc}
   * <p>Destination prefixes are expected to be slash-separated Strings and
   * therefore a slash is automatically appended where missing to ensure a
   * proper prefix-based match (i.e. matching complete segments).
   * <p>Note however that the remaining portion of a destination after the
   * prefix may use a different separator (for example, commonly "." in messaging)
   * depending on the configured {@code PathMatcher}.
   */
  @Override
  public void setDestinationPrefixes(@Nullable Collection<String> prefixes) {
    super.setDestinationPrefixes(appendSlashes(prefixes));
  }

  private static @Nullable Collection<String> appendSlashes(@Nullable Collection<String> prefixes) {
    if (CollectionUtils.isEmpty(prefixes)) {
      return prefixes;
    }
    Collection<String> result = new ArrayList<>(prefixes.size());
    for (String prefix : prefixes) {
      if (!prefix.endsWith("/")) {
        prefix = prefix + "/";
      }
      result.add(prefix);
    }
    return result;
  }

  /**
   * Configure a {@link MessageConverter} to use to convert the payload of a message from
   * its serialized form with a specific MIME type to an Object matching the target method
   * parameter. The converter is also used when sending a message to the message broker.
   *
   * @see CompositeMessageConverter
   */
  public void setMessageConverter(MessageConverter converter) {
    this.messageConverter = converter;
    ((AbstractMessageSendingTemplate<?>) this.clientMessagingTemplate).setMessageConverter(converter);
  }

  /**
   * Return the configured {@link MessageConverter}.
   */
  public MessageConverter getMessageConverter() {
    return this.messageConverter;
  }

  /**
   * Configure a {@link ConversionService} to use when resolving method arguments,
   * for example message header values.
   * <p>By default, {@link DefaultFormattingConversionService} is used.
   */
  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Return the configured {@link ConversionService}.
   */
  public ConversionService getConversionService() {
    return this.conversionService;
  }

  /**
   * Set the PathMatcher implementation to use for matching destinations
   * against configured destination patterns.
   * <p>By default, {@link AntPathMatcher} is used.
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    Assert.notNull(pathMatcher, "PathMatcher is required");
    this.pathMatcher = pathMatcher;
    this.slashPathSeparator = this.pathMatcher.combine("a", "a").equals("a/a");
  }

  /**
   * Return the PathMatcher implementation to use for matching destinations.
   */
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  /**
   * Return the configured Validator instance.
   */
  public @Nullable Validator getValidator() {
    return this.validator;
  }

  /**
   * Set the Validator instance used for validating {@code @Payload} arguments.
   *
   * @see infra.validation.annotation.Validated
   * @see PayloadMethodArgumentResolver
   */
  public void setValidator(@Nullable Validator validator) {
    this.validator = validator;
  }

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.valueResolver = resolver;
  }

  /**
   * Configure a {@link MessageHeaderInitializer} to pass on to
   * {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}
   * that send messages from controller return values.
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
   * Add a filter to determine which headers from the input message should be
   * propagated to the output message. Applies to return value handling of
   * {@code @SendTo}, {@code @SendToUser}, and {@code @SubscribeMapping}
   * controller methods. The filter is applied to the "native headers" submap.
   * Multiple filters are combined with {@link Predicate#or(Predicate)}.
   * <p>By default, no headers are propagated if this is not set.
   *
   * @see SendToMethodReturnValueHandler#addHeaderFilter(Predicate)
   * @see SubscriptionMethodReturnValueHandler#addHeaderFilter(Predicate)
   * @since 5.0
   */
  public void addHeaderFilter(Predicate<String> filter) {
    Assert.notNull(filter, "'headerFilter' predicate is required");
    this.headerFilter = (this.headerFilter != null ? this.headerFilter.or(filter) : filter);
  }

  /**
   * Set the phase that this handler should run in.
   * <p>By default, this is {@link SmartLifecycle#DEFAULT_PHASE}, but with
   * {@code @EnableWebSocketMessageBroker} configuration it is set to 0.
   *
   * @since 5.0
   */
  public void setPhase(int phase) {
    this.phase = phase;
  }

  @Override
  public int getPhase() {
    return (this.phase != null ? this.phase : SmartLifecycle.super.getPhase());
  }

  @Override
  public final void start() {
    synchronized(this.lifecycleMonitor) {
      this.clientInboundChannel.subscribe(this);
      this.running = true;
    }
  }

  @Override
  public final void stop() {
    synchronized(this.lifecycleMonitor) {
      this.running = false;
      this.clientInboundChannel.unsubscribe(this);
    }
  }

  @Override
  public final void stop(Runnable callback) {
    synchronized(this.lifecycleMonitor) {
      stop();
      callback.run();
    }
  }

  @Override
  public final boolean isRunning() {
    return this.running;
  }

  @Override
  protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
    ApplicationContext context = getApplicationContext();
    ConfigurableBeanFactory beanFactory = (context instanceof ConfigurableApplicationContext cac ?
            cac.getBeanFactory() : null);

    List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

    // Annotation-based argument resolution
    resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
    resolvers.add(new HeadersMethodArgumentResolver());
    resolvers.add(new DestinationVariableMethodArgumentResolver(this.conversionService));

    // Type-based argument resolution
    resolvers.add(new PrincipalMethodArgumentResolver());
    resolvers.add(new MessageMethodArgumentResolver(this.messageConverter));

    resolvers.addAll(getCustomArgumentResolvers());
    resolvers.add(new PayloadMethodArgumentResolver(this.messageConverter, this.validator));

    return resolvers;
  }

  @Override
  protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
    List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();

    // Single-purpose return value types

    handlers.add(new CompletableFutureReturnValueHandler());
    if (REACTOR_PRESENT) {
      handlers.add(new ReactiveReturnValueHandler());
    }

    // Annotation-based return value types

    SendToMethodReturnValueHandler sendToHandler =
            new SendToMethodReturnValueHandler(this.brokerTemplate, true);
    sendToHandler.setHeaderInitializer(this.headerInitializer);
    if (this.headerFilter != null) {
      sendToHandler.addHeaderFilter(this.headerFilter);
    }
    handlers.add(sendToHandler);

    SubscriptionMethodReturnValueHandler subscriptionHandler =
            new SubscriptionMethodReturnValueHandler(this.clientMessagingTemplate);
    subscriptionHandler.setHeaderInitializer(this.headerInitializer);
    if (this.headerFilter != null) {
      subscriptionHandler.addHeaderFilter(this.headerFilter);
    }
    handlers.add(subscriptionHandler);

    // Custom return value types

    handlers.addAll(getCustomReturnValueHandlers());

    // Catch-all

    sendToHandler = new SendToMethodReturnValueHandler(this.brokerTemplate, false);
    sendToHandler.setHeaderInitializer(this.headerInitializer);
    if (this.headerFilter != null) {
      sendToHandler.addHeaderFilter(this.headerFilter);
    }
    handlers.add(sendToHandler);

    return handlers;
  }

  @Override
  protected Logger getReturnValueHandlerLogger() {
    return SimpLogging.forLog(HandlerMethodReturnValueHandlerComposite.defaultLogger);
  }

  @Override
  protected Logger getHandlerMethodLogger() {
    return SimpLogging.forLog(HandlerMethod.defaultLogger);
  }

  @Override
  protected boolean isHandler(Class<?> beanType) {
    return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
  }

  @Override
  protected @Nullable SimpMessageMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    MessageMapping messageAnn = AnnotatedElementUtils.findMergedAnnotation(method, MessageMapping.class);
    if (messageAnn != null) {
      MessageMapping typeAnn = AnnotatedElementUtils.findMergedAnnotation(handlerType, MessageMapping.class);
      // Only actually register it if there are destinations specified;
      // otherwise @MessageMapping is just being used as a (meta-annotation) marker.
      if (messageAnn.value().length > 0 || (typeAnn != null && typeAnn.value().length > 0)) {
        SimpMessageMappingInfo result = createMessageMappingCondition(messageAnn.value());
        if (typeAnn != null) {
          result = createMessageMappingCondition(typeAnn.value()).combine(result);
        }
        return result;
      }
    }

    SubscribeMapping subscribeAnn = AnnotatedElementUtils.findMergedAnnotation(method, SubscribeMapping.class);
    if (subscribeAnn != null) {
      MessageMapping typeAnn = AnnotatedElementUtils.findMergedAnnotation(handlerType, MessageMapping.class);
      // Only actually register it if there are destinations specified;
      // otherwise @SubscribeMapping is just being used as a (meta-annotation) marker.
      if (subscribeAnn.value().length > 0 || (typeAnn != null && typeAnn.value().length > 0)) {
        SimpMessageMappingInfo result = createSubscribeMappingCondition(subscribeAnn.value());
        if (typeAnn != null) {
          result = createMessageMappingCondition(typeAnn.value()).combine(result);
        }
        return result;
      }
    }

    return null;
  }

  private SimpMessageMappingInfo createMessageMappingCondition(String[] destinations) {
    String[] resolvedDestinations = resolveEmbeddedValuesInDestinations(destinations);
    return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.MESSAGE,
            new DestinationPatternsMessageCondition(resolvedDestinations, this.pathMatcher));
  }

  private SimpMessageMappingInfo createSubscribeMappingCondition(String[] destinations) {
    String[] resolvedDestinations = resolveEmbeddedValuesInDestinations(destinations);
    return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.SUBSCRIBE,
            new DestinationPatternsMessageCondition(resolvedDestinations, this.pathMatcher));
  }

  /**
   * Resolve placeholder values in the given array of destinations.
   *
   * @return a new array with updated destinations
   * @since 5.0
   */
  protected String[] resolveEmbeddedValuesInDestinations(String[] destinations) {
    if (this.valueResolver == null) {
      return destinations;
    }
    String[] result = new String[destinations.length];
    for (int i = 0; i < destinations.length; i++) {
      result[i] = Objects.requireNonNull(this.valueResolver.resolveStringValue(destinations[i]));
    }
    return result;
  }

  @Override
  protected Set<String> getDirectLookupDestinations(SimpMessageMappingInfo mapping) {
    Set<String> result = new LinkedHashSet<>();
    for (String pattern : mapping.getDestinationConditions().getPatterns()) {
      if (!this.pathMatcher.isPattern(pattern)) {
        result.add(pattern);
      }
    }
    return result;
  }

  @Override
  protected @Nullable String getDestination(Message<?> message) {
    return SimpMessageHeaderAccessor.getDestination(message.getHeaders());
  }

  @Override
  protected @Nullable String getLookupDestination(@Nullable String destination) {
    if (destination == null) {
      return null;
    }
    if (CollectionUtils.isEmpty(getDestinationPrefixes())) {
      return destination;
    }
    for (String prefix : getDestinationPrefixes()) {
      if (destination.startsWith(prefix)) {
        if (this.slashPathSeparator) {
          return destination.substring(prefix.length() - 1);
        }
        else {
          return destination.substring(prefix.length());
        }
      }
    }
    return null;
  }

  @Override
  protected @Nullable SimpMessageMappingInfo getMatchingMapping(SimpMessageMappingInfo mapping, Message<?> message) {
    return mapping.getMatchingCondition(message);

  }

  @Override
  protected Comparator<SimpMessageMappingInfo> getMappingComparator(final Message<?> message) {
    return (info1, info2) -> info1.compareTo(info2, message);
  }

  @Override
  protected void handleMatch(SimpMessageMappingInfo mapping, HandlerMethod handlerMethod,
          String lookupDestination, Message<?> message) {

    Set<String> patterns = mapping.getDestinationConditions().getPatterns();
    if (CollectionUtils.isNotEmpty(patterns)) {
      String pattern = patterns.iterator().next();
      Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(pattern, lookupDestination);
      if (CollectionUtils.isNotEmpty(vars)) {
        MessageHeaderAccessor mha = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
        Assert.state(mha != null && mha.isMutable(), "Mutable MessageHeaderAccessor required");
        mha.setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
      }
    }

    try {
      SimpAttributesContextHolder.setAttributesFromMessage(message);
      super.handleMatch(mapping, handlerMethod, lookupDestination, message);
    }
    finally {
      SimpAttributesContextHolder.resetAttributes();
    }
  }

  @Override
  protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(Class<?> beanType) {
    return new AnnotationExceptionHandlerMethodResolver(beanType);
  }

}

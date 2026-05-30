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

package infra.messaging.rsocket.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;

import infra.messaging.handler.annotation.DestinationVariable;
import infra.messaging.handler.annotation.Payload;
import infra.messaging.rsocket.RSocketRequester;
import infra.messaging.rsocket.RSocketStrategies;

/**
 * Annotation to declare an RSocket endpoint on an RSocket service interface.
 * Supported for use as an RSocket requester via
 * {@link RSocketServiceProxyFactory}, and as a responder by implementing the
 * interface to handle requests.
 *
 * <p>The endpoint route is determined through the annotation attribute,
 * and through the method arguments.
 *
 * <p>The annotation is supported at the type level to express a common route,
 * to be inherited by all methods.
 *
 * <p>Supported method arguments:
 * <table border="1">
 * <tr>
 * <th>Method Argument</th>
 * <th>Description</th>
 * <th>Resolver</th>
 * </tr>
 * <tr>
 * <td>{@link DestinationVariable @DestinationVariable}</td>
 * <td>Add a route variable to expand into the route</td>
 * <td>{@link DestinationVariableArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link Payload @Payload}</td>
 * <td>Set the input payload(s) for the request</td>
 * <td>{@link PayloadArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link Object}, if followed by {@link infra.util.MimeType}</td>
 * <td>Add a metadata value</td>
 * <td>{@link MetadataArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link infra.util.MimeType}</td>
 * <td>Set the MIME type for the metadata value in the preceding argument</td>
 * <td>{@link MetadataArgumentResolver}</td>
 * </tr>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(RSocketExchangeReflectiveProcessor.class)
public @interface RSocketExchange {

  /**
   * Destination-based mapping expressed by this annotation. This is either
   * {@link infra.util.AntPathMatcher AntPathMatcher} or
   * {@link infra.web.util.pattern.PathPattern PathPattern}
   * based pattern, depending on which is configured via
   * {@link RSocketStrategies} in
   * {@link RSocketRequester}.
   */
  String value() default "";

}

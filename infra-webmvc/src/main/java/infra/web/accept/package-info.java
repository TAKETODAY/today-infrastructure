/**
 * This package contains classes used to determine the requested the media types in a request.
 *
 * <p>{@link infra.web.accept.ContentNegotiationStrategy} is the main
 * abstraction for determining requested {@linkplain infra.http.MediaType media types}
 * with implementations based on
 * {@linkplain infra.web.accept.PathExtensionContentNegotiationStrategy path extensions}, a
 * {@linkplain infra.web.accept.ParameterContentNegotiationStrategy a request parameter}, the
 * {@linkplain infra.web.accept.HeaderContentNegotiationStrategy 'Accept' header}, or a
 * {@linkplain infra.web.accept.FixedContentNegotiationStrategy default content type}.
 *
 * <p>{@link infra.web.accept.ContentNegotiationManager} is used to delegate to one
 * ore more of the above strategies in a specific order.
 */
@NullMarked
package infra.web.accept;

import org.jspecify.annotations.NullMarked;

/**
 * This package contains classes used to determine the requested the media types in a request.
 *
 * <p>{@link cn.taketoday.web.accept.ContentNegotiationStrategy} is the main
 * abstraction for determining requested {@linkplain cn.taketoday.http.MediaType media types}
 * with implementations based on
 * {@linkplain cn.taketoday.web.accept.PathExtensionContentNegotiationStrategy path extensions}, a
 * {@linkplain cn.taketoday.web.accept.ParameterContentNegotiationStrategy a request parameter}, the
 * {@linkplain cn.taketoday.web.accept.HeaderContentNegotiationStrategy 'Accept' header}, or a
 * {@linkplain cn.taketoday.web.accept.FixedContentNegotiationStrategy default content type}.
 *
 * <p>{@link cn.taketoday.web.accept.ContentNegotiationManager} is used to delegate to one
 * ore more of the above strategies in a specific order.
 */
@NonNullApi
@NonNullFields
package cn.taketoday.web.accept;

import cn.taketoday.lang.NonNullApi;
import cn.taketoday.lang.NonNullFields;

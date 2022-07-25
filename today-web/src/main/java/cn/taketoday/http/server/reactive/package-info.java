/**
 * Abstractions for reactive HTTP server support including a
 * {@link cn.taketoday.http.server.reactive.ServerHttpRequest} and
 * {@link cn.taketoday.http.server.reactive.ServerHttpResponse} along with an
 * {@link cn.taketoday.http.server.reactive.HttpHandler} for processing.
 *
 * <p>Also provides implementations adapting to different runtimes
 * including Servlet 3.1 containers, Netty + Reactor IO, and Undertow.
 */
@NonNullApi
@NonNullFields
package cn.taketoday.http.server.reactive;

import cn.taketoday.lang.NonNullApi;
import cn.taketoday.lang.NonNullFields;

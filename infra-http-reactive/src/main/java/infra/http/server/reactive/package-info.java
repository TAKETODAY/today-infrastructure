
/**
 * Abstractions for reactive HTTP server support including a
 * {@link infra.http.server.reactive.ServerHttpRequest} and
 * {@link infra.http.server.reactive.ServerHttpResponse} along with an
 * {@link infra.http.server.reactive.HttpHandler} for processing.
 *
 * <p>Also provides implementations adapting to different runtimes
 * including Netty + Reactor IO.
 */
@NullMarked
package infra.http.server.reactive;

import org.jspecify.annotations.NullMarked;

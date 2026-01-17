
/**
 * Abstractions for reactive HTTP server support including a
 * {@link infra.http.reactive.server.ServerHttpRequest} and
 * {@link infra.http.reactive.server.ServerHttpResponse} along with an
 * {@link infra.http.reactive.server.HttpHandler} for processing.
 *
 * <p>Also provides implementations adapting to different runtimes
 * including Netty + Reactor IO.
 */
@NullMarked
package infra.http.reactive.server;

import org.jspecify.annotations.NullMarked;

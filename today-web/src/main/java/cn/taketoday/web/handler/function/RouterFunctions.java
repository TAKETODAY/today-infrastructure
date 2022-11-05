/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.function;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * <strong>Central entry point to Infra functional web framework.</strong>
 * Exposes routing functionality, such as to {@linkplain #route() create} a
 * {@code RouterFunction} using a discoverable builder-style API, to
 * {@linkplain #route(RequestPredicate, HandlerFunction) create} a {@code RouterFunction}
 * given a {@code RequestPredicate} and {@code HandlerFunction}, and to do further
 * {@linkplain #nest(RequestPredicate, RouterFunction) subrouting} on an existing routing
 * function.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class RouterFunctions {

  private static final Logger log = LoggerFactory.getLogger(RouterFunctions.class);

  /**
   * Name of the request attribute that contains the {@link ServerRequest}.
   */
  public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

  /**
   * Name of the request attribute that contains the URI
   * templates map, mapping variable names to values.
   */
  public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
          RouterFunctions.class.getName() + ".uriTemplateVariables";

  /**
   * Name of the request attribute that contains the matching pattern, as a
   * {@link cn.taketoday.web.util.pattern.PathPattern}.
   */
  public static final String MATCHING_PATTERN_ATTRIBUTE =
          RouterFunctions.class.getName() + ".matchingPattern";

  /**
   * Offers a discoverable way to create router functions through a builder-style interface.
   *
   * @return a router function builder
   */
  public static Builder route() {
    return new RouterFunctionBuilder();
  }

  /**
   * Route to the given handler function if the given request predicate applies.
   * <p>For instance, the following example routes GET requests for "/user" to the
   * {@code listUsers} method in {@code userController}:
   * <pre class="code">
   * RouterFunction&lt;ServerResponse&gt; route =
   *     RouterFunctions.route(RequestPredicates.GET("/user"), userController::listUsers);
   * </pre>
   *
   * @param predicate the predicate to test
   * @param handlerFunction the handler function to route to if the predicate applies
   * @param <T> the type of response returned by the handler function
   * @return a router function that routes to {@code handlerFunction} if
   * {@code predicate} evaluates to {@code true}
   * @see RequestPredicates
   */
  public static <T extends ServerResponse> RouterFunction<T> route(
          RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

    return new DefaultRouterFunction<>(predicate, handlerFunction);
  }

  /**
   * Route to the given router function if the given request predicate applies. This method can be
   * used to create <strong>nested routes</strong>, where a group of routes share a common path
   * (prefix), header, or other request predicate.
   * <p>For instance, the following example first creates a composed route that resolves to
   * {@code listUsers} for a GET, and {@code createUser} for a POST. This composed route then gets
   * nested with a "/user" path predicate, so that GET requests for "/user" will list users,
   * and POST request for "/user" will create a new user.
   * <pre class="code">
   * RouterFunction&lt;ServerResponse&gt; userRoutes =
   *   RouterFunctions.route(RequestPredicates.method(HttpMethod.GET), this::listUsers)
   *     .andRoute(RequestPredicates.method(HttpMethod.POST), this::createUser);
   * RouterFunction&lt;ServerResponse&gt; nestedRoute =
   *   RouterFunctions.nest(RequestPredicates.path("/user"), userRoutes);
   * </pre>
   *
   * @param predicate the predicate to test
   * @param routerFunction the nested router function to delegate to if the predicate applies
   * @param <T> the type of response returned by the handler function
   * @return a router function that routes to {@code routerFunction} if
   * {@code predicate} evaluates to {@code true}
   * @see RequestPredicates
   */
  public static <T extends ServerResponse> RouterFunction<T> nest(
          RequestPredicate predicate, RouterFunction<T> routerFunction) {

    return new DefaultNestedRouterFunction<>(predicate, routerFunction);
  }

  /**
   * Route requests that match the given pattern to resources relative to the given root location.
   * For instance
   * <pre class="code">
   * Resource location = new FileSystemResource("public-resources/");
   * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
   * </pre>
   *
   * @param pattern the pattern to match
   * @param location the location directory relative to which resources should be resolved
   * @return a router function that routes to resources
   * @see #resourceLookupFunction(String, Resource)
   */
  public static RouterFunction<ServerResponse> resources(String pattern, Resource location) {
    return resources(resourceLookupFunction(pattern, location));
  }

  /**
   * Returns the resource lookup function used by {@link #resources(String, Resource)}.
   * The returned function can be {@linkplain Function#andThen(Function) composed} on, for
   * instance to return a default resource when the lookup function does not match:
   * <pre class="code">
   * Optional&lt;Resource&gt; defaultResource = Optional.of(new ClassPathResource("index.html"));
   * Function&lt;ServerRequest, Optional&lt;Resource&gt;&gt; lookupFunction =
   *   RouterFunctions.resourceLookupFunction("/resources/**", new FileSystemResource("public-resources/"))
   *     .andThen(resource -&gt; resource.or(() -&gt; defaultResource));
   * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources(lookupFunction);
   * </pre>
   *
   * @param pattern the pattern to match
   * @param location the location directory relative to which resources should be resolved
   * @return the default resource lookup function for the given parameters.
   */
  public static Function<ServerRequest, Optional<Resource>> resourceLookupFunction(String pattern, Resource location) {
    return new PathResourceLookupFunction(pattern, location);
  }

  /**
   * Route to resources using the provided lookup function. If the lookup function provides a
   * {@link Resource} for the given request, it will be it will be exposed using a
   * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
   *
   * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
   * @return a router function that routes to resources
   */
  public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
    return new ResourcesRouterFunction(lookupFunction);
  }

  /**
   * Changes the {@link PathPatternParser} on the given {@linkplain RouterFunction router function}. This method
   * can be used to change the {@code PathPatternParser} properties from the defaults, for instance to change
   * {@linkplain PathPatternParser#setCaseSensitive(boolean) case sensitivity}.
   *
   * @param routerFunction the router function to change the parser in
   * @param parser the parser to change to.
   * @param <T> the type of response returned by the handler function
   * @return the change router function
   */
  public static <T extends ServerResponse> RouterFunction<T> changeParser(
          RouterFunction<T> routerFunction, PathPatternParser parser) {

    Assert.notNull(routerFunction, "RouterFunction is required");
    Assert.notNull(parser, "Parser is required");

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(parser);
    routerFunction.accept(visitor);
    return routerFunction;
  }

  /**
   * Represents a discoverable builder for router functions.
   * Obtained via {@link RouterFunctions#route()}.
   */
  public interface Builder {

    /**
     * Adds a route to the given handler function that handles HTTP {@code GET} requests.
     *
     * @param handlerFunction the handler function to handle all {@code GET} requests
     * @return this builder
     */
    Builder GET(HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code GET} requests
     * that match the given pattern.
     *
     * @param pattern the pattern to match to
     * @param handlerFunction the handler function to handle all {@code GET} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code GET} requests
     * that match the given predicate.
     *
     * @param predicate predicate to match
     * @param handlerFunction the handler function to handle all {@code GET} requests that
     * match {@code predicate}
     * @return this builder
     * @see RequestPredicates
     */
    Builder GET(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code GET} requests
     * that match the given pattern and predicate.
     * <p>For instance, the following example routes GET requests for "/user" that accept JSON
     * to the {@code listUsers} method in {@code userController}:
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; route =
     *   RouterFunctions.route()
     *     .GET("/user", RequestPredicates.accept(MediaType.APPLICATION_JSON), userController::listUsers)
     *     .build();
     * </pre>
     *
     * @param pattern the pattern to match to
     * @param predicate additional predicate to match
     * @param handlerFunction the handler function to handle all {@code GET} requests that
     * match {@code pattern} and the predicate
     * @return this builder
     * @see RequestPredicates
     */
    Builder GET(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles HTTP {@code HEAD} requests.
     *
     * @param handlerFunction the handler function to handle all {@code HEAD} requests
     * @return this builder
     */
    Builder HEAD(HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
     * that match the given pattern.
     *
     * @param pattern the pattern to match to
     * @param handlerFunction the handler function to handle all {@code HEAD} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
     * that match the given predicate.
     *
     * @param predicate predicate to match
     * @param handlerFunction the handler function to handle all {@code HEAD} requests that
     * match {@code predicate}
     * @return this builder
     * @see RequestPredicates
     */
    Builder HEAD(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code HEAD} requests
     * that match the given pattern and predicate.
     *
     * @param pattern the pattern to match to
     * @param predicate additional predicate to match
     * @param handlerFunction the handler function to handle all {@code HEAD} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder HEAD(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles HTTP {@code POST} requests.
     *
     * @param handlerFunction the handler function to handle all {@code POST} requests
     * @return this builder
     */
    Builder POST(HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code POST} requests
     * that match the given pattern.
     *
     * @param pattern the pattern to match to
     * @param handlerFunction the handler function to handle all {@code POST} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code POST} requests
     * that match the given predicate.
     *
     * @param predicate predicate to match
     * @param handlerFunction the handler function to handle all {@code POST} requests that
     * match {@code predicate}
     * @return this builder
     * @see RequestPredicates
     */
    Builder POST(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code POST} requests
     * that match the given pattern and predicate.
     * <p>For instance, the following example routes POST requests for "/user" that contain JSON
     * to the {@code addUser} method in {@code userController}:
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; route =
     *   RouterFunctions.route()
     *     .POST("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::addUser)
     *     .build();
     * </pre>
     *
     * @param pattern the pattern to match to
     * @param predicate additional predicate to match
     * @param handlerFunction the handler function to handle all {@code POST} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder POST(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles HTTP {@code PUT} requests.
     *
     * @param handlerFunction the handler function to handle all {@code PUT} requests
     * @return this builder
     */
    Builder PUT(HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
     * that match the given pattern.
     *
     * @param pattern the pattern to match to
     * @param handlerFunction the handler function to handle all {@code PUT} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
     * that match the given predicate.
     *
     * @param predicate predicate to match
     * @param handlerFunction the handler function to handle all {@code PUT} requests that
     * match {@code predicate}
     * @return this builder
     * @see RequestPredicates
     */
    Builder PUT(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code PUT} requests
     * that match the given pattern and predicate.
     * <p>For instance, the following example routes PUT requests for "/user" that contain JSON
     * to the {@code editUser} method in {@code userController}:
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; route =
     *   RouterFunctions.route()
     *     .PUT("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
     *     .build();
     * </pre>
     *
     * @param pattern the pattern to match to
     * @param predicate additional predicate to match
     * @param handlerFunction the handler function to handle all {@code PUT} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder PUT(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles HTTP {@code PATCH} requests.
     *
     * @param handlerFunction the handler function to handle all {@code PATCH} requests
     * @return this builder
     */
    Builder PATCH(HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
     * that match the given pattern.
     *
     * @param pattern the pattern to match to
     * @param handlerFunction the handler function to handle all {@code PATCH} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
     * that match the given predicate.
     *
     * @param predicate predicate to match
     * @param handlerFunction the handler function to handle all {@code PATCH} requests that
     * match {@code predicate}
     * @return this builder
     * @see RequestPredicates
     */
    Builder PATCH(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code PATCH} requests
     * that match the given pattern and predicate.
     * <p>For instance, the following example routes PATCH requests for "/user" that contain JSON
     * to the {@code editUser} method in {@code userController}:
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; route =
     *   RouterFunctions.route()
     *     .PATCH("/user", RequestPredicates.contentType(MediaType.APPLICATION_JSON), userController::editUser)
     *     .build();
     * </pre>
     *
     * @param pattern the pattern to match to
     * @param predicate additional predicate to match
     * @param handlerFunction the handler function to handle all {@code PATCH} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder PATCH(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles HTTP {@code DELETE} requests.
     *
     * @param handlerFunction the handler function to handle all {@code DELETE} requests
     * @return this builder
     */
    Builder DELETE(HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
     * that match the given pattern.
     *
     * @param pattern the pattern to match to
     * @param handlerFunction the handler function to handle all {@code DELETE} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
     * that match the given predicate.
     *
     * @param predicate predicate to match
     * @param handlerFunction the handler function to handle all {@code DELETE} requests that
     * match {@code predicate}
     * @return this builder
     * @see RequestPredicates
     */
    Builder DELETE(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code DELETE} requests
     * that match the given pattern and predicate.
     *
     * @param pattern the pattern to match to
     * @param predicate additional predicate to match
     * @param handlerFunction the handler function to handle all {@code DELETE} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder DELETE(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles HTTP {@code OPTIONS} requests.
     *
     * @param handlerFunction the handler function to handle all {@code OPTIONS} requests
     * @return this builder
     */
    Builder OPTIONS(HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
     * that match the given pattern.
     *
     * @param pattern the pattern to match to
     * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
     * that match the given predicate.
     *
     * @param predicate predicate to match
     * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
     * match {@code predicate}
     * @return this builder
     * @see RequestPredicates
     */
    Builder OPTIONS(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all HTTP {@code OPTIONS} requests
     * that match the given pattern and predicate.
     *
     * @param pattern the pattern to match to
     * @param predicate additional predicate to match
     * @param handlerFunction the handler function to handle all {@code OPTIONS} requests that
     * match {@code pattern}
     * @return this builder
     */
    Builder OPTIONS(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds a route to the given handler function that handles all requests that match the
     * given predicate.
     *
     * @param predicate the request predicate to match
     * @param handlerFunction the handler function to handle all requests that match the predicate
     * @return this builder
     * @see RequestPredicates
     */
    Builder route(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

    /**
     * Adds the given route to this builder. Can be used to merge externally defined router
     * functions into this builder, or can be combined with
     * {@link RouterFunctions#route(RequestPredicate, HandlerFunction)}
     * to allow for more flexible predicate matching.
     * <p>For instance, the following example adds the router function returned from
     * {@code OrderController.routerFunction()}.
     * to the {@code changeUser} method in {@code userController}:
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; route =
     *   RouterFunctions.route()
     *     .GET("/users", userController::listUsers)
     *     .add(orderController.routerFunction());
     *     .build();
     * </pre>
     *
     * @param routerFunction the router function to be added
     * @return this builder
     * @see RequestPredicates
     */
    Builder add(RouterFunction<ServerResponse> routerFunction);

    /**
     * Route requests that match the given pattern to resources relative to the given root location.
     * For instance
     * <pre class="code">
     * Resource location = new FileSystemResource("public-resources/");
     * RouterFunction&lt;ServerResponse&gt; resources = RouterFunctions.resources("/resources/**", location);
     * </pre>
     *
     * @param pattern the pattern to match
     * @param location the location directory relative to which resources should be resolved
     * @return this builder
     */
    Builder resources(String pattern, Resource location);

    /**
     * Route to resources using the provided lookup function. If the lookup function provides a
     * {@link Resource} for the given request, it will be it will be exposed using a
     * {@link HandlerFunction} that handles GET, HEAD, and OPTIONS requests.
     *
     * @param lookupFunction the function to provide a {@link Resource} given the {@link ServerRequest}
     * @return this builder
     */
    Builder resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

    /**
     * Route to the supplied router function if the given request predicate applies. This method
     * can be used to create <strong>nested routes</strong>, where a group of routes share a
     * common path (prefix), header, or other request predicate.
     * <p>For instance, the following example creates a nested route with a "/user" path
     * predicate, so that GET requests for "/user" will list users,
     * and POST request for "/user" will create a new user.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; nestedRoute =
     *   RouterFunctions.route()
     *     .nest(RequestPredicates.path("/user"), () -&gt;
     *       RouterFunctions.route()
     *         .GET(this::listUsers)
     *         .POST(this::createUser)
     *         .build())
     *     .build();
     * </pre>
     *
     * @param predicate the predicate to test
     * @param routerFunctionSupplier supplier for the nested router function to delegate to if
     * the predicate applies
     * @return this builder
     * @see RequestPredicates
     */
    Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

    /**
     * Route to a built router function if the given request predicate applies.
     * This method can be used to create <strong>nested routes</strong>, where a group of routes
     * share a common path (prefix), header, or other request predicate.
     * <p>For instance, the following example creates a nested route with a "/user" path
     * predicate, so that GET requests for "/user" will list users,
     * and POST request for "/user" will create a new user.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; nestedRoute =
     *   RouterFunctions.route()
     *     .nest(RequestPredicates.path("/user"), builder -&gt;
     *       builder.GET(this::listUsers)
     *              .POST(this::createUser))
     *     .build();
     * </pre>
     *
     * @param predicate the predicate to test
     * @param builderConsumer consumer for a {@code Builder} that provides the nested router
     * function
     * @return this builder
     * @see RequestPredicates
     */
    Builder nest(RequestPredicate predicate, Consumer<Builder> builderConsumer);

    /**
     * Route to the supplied router function if the given path prefix pattern applies. This method
     * can be used to create <strong>nested routes</strong>, where a group of routes share a
     * common path prefix. Specifically, this method can be used to merge externally defined
     * router functions under a path prefix.
     * <p>For instance, the following example creates a nested route with a "/user" path
     * predicate that delegates to the router function defined in {@code userController},
     * and with a "/order" path that delegates to {@code orderController}.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; nestedRoute =
     *   RouterFunctions.route()
     *     .path("/user", userController::routerFunction)
     *     .path("/order", orderController::routerFunction)
     *     .build();
     * </pre>
     *
     * @param pattern the pattern to match to
     * @param routerFunctionSupplier supplier for the nested router function to delegate to if
     * the pattern matches
     * @return this builder
     */
    Builder path(String pattern, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

    /**
     * Route to a built router function if the given path prefix pattern applies.
     * This method can be used to create <strong>nested routes</strong>, where a group of routes
     * share a common path prefix.
     * <p>For instance, the following example creates a nested route with a "/user" path
     * predicate, so that GET requests for "/user" will list users,
     * and POST request for "/user" will create a new user.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; nestedRoute =
     *   RouterFunctions.route()
     *     .path("/user", builder -&gt;
     *       builder.GET(this::listUsers)
     *              .POST(this::createUser))
     *     .build();
     * </pre>
     *
     * @param pattern the pattern to match to
     * @param builderConsumer consumer for a {@code Builder} that provides the nested router
     * function
     * @return this builder
     */
    Builder path(String pattern, Consumer<Builder> builderConsumer);

    /**
     * Filters all routes created by this builder with the given filter function. Filter
     * functions are typically used to address cross-cutting concerns, such as logging,
     * security, etc.
     * <p>For instance, the following example creates a filter that returns a 401 Unauthorized
     * response if the request does not contain the necessary authentication headers.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; filteredRoute =
     *   RouterFunctions.route()
     *     .GET("/user", this::listUsers)
     *     .filter((request, next) -&gt; {
     *       // check for authentication headers
     *       if (isAuthenticated(request)) {
     *         return next.handle(request);
     *       }
     *       else {
     *         return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
     *       }
     *     })
     *     .build();
     * </pre>
     *
     * @param filterFunction the function to filter all routes built by this builder
     * @return this builder
     */
    Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction);

    /**
     * Filter the request object for all routes created by this builder with the given request
     * processing function. Filters are typically used to address cross-cutting concerns, such
     * as logging, security, etc.
     * <p>For instance, the following example creates a filter that logs the request before
     * the handler function executes.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; filteredRoute =
     *   RouterFunctions.route()
     *     .GET("/user", this::listUsers)
     *     .before(request -&gt; {
     *       log(request);
     *       return request;
     *     })
     *     .build();
     * </pre>
     *
     * @param requestProcessor a function that transforms the request
     * @return this builder
     */
    Builder before(Function<ServerRequest, ServerRequest> requestProcessor);

    /**
     * Filter the response object for all routes created by this builder with the given response
     * processing function. Filters are typically used to address cross-cutting concerns, such
     * as logging, security, etc.
     * <p>For instance, the following example creates a filter that logs the response after
     * the handler function executes.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; filteredRoute =
     *   RouterFunctions.route()
     *     .GET("/user", this::listUsers)
     *     .after((request, response) -&gt; {
     *       log(response);
     *       return response;
     *     })
     *     .build();
     * </pre>
     *
     * @param responseProcessor a function that transforms the response
     * @return this builder
     */
    Builder after(BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor);

    /**
     * Filters all exceptions that match the predicate by applying the given response provider
     * function.
     * <p>For instance, the following example creates a filter that returns a 500 response
     * status when an {@code IllegalStateException} occurs.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; filteredRoute =
     *   RouterFunctions.route()
     *     .GET("/user", this::listUsers)
     *     .onError(e -&gt; e instanceof IllegalStateException,
     *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
     *     .build();
     * </pre>
     *
     * @param predicate the type of exception to filter
     * @param responseProvider a function that creates a response
     * @return this builder
     */
    Builder onError(Predicate<Throwable> predicate,
            BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

    /**
     * Filters all exceptions of the given type by applying the given response provider
     * function.
     * <p>For instance, the following example creates a filter that returns a 500 response
     * status when an {@code IllegalStateException} occurs.
     * <pre class="code">
     * RouterFunction&lt;ServerResponse&gt; filteredRoute =
     *   RouterFunctions.route()
     *     .GET("/user", this::listUsers)
     *     .onError(IllegalStateException.class,
     *       (e, request) -&gt; ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
     *     .build();
     * </pre>
     *
     * @param exceptionType the type of exception to filter
     * @param responseProvider a function that creates a response
     * @return this builder
     */
    Builder onError(Class<? extends Throwable> exceptionType,
            BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

    /**
     * Add an attribute with the given name and value to the last route built with this builder.
     *
     * @param name the attribute name
     * @param value the attribute value
     * @return this builder
     */
    Builder withAttribute(String name, Object value);

    /**
     * Manipulate the attributes of the last route built with the given consumer.
     * <p>The map provided to the consumer is "live", so that the consumer can be used
     * to {@linkplain Map#put(Object, Object) overwrite} existing attributes,
     * {@linkplain Map#remove(Object) remove} attributes, or use any of the other
     * {@link Map} methods.
     *
     * @param attributesConsumer a function that consumes the attributes map
     * @return this builder
     */
    Builder withAttributes(Consumer<Map<String, Object>> attributesConsumer);

    /**
     * Builds the {@code RouterFunction}. All created routes are
     * {@linkplain RouterFunction#and(RouterFunction) composed} with one another, and filters
     * (if any) are applied to the result.
     *
     * @return the built router function
     */
    RouterFunction<ServerResponse> build();
  }

  /**
   * Receives notifications from the logical structure of router functions.
   */
  public interface Visitor {

    /**
     * Receive notification of the beginning of a nested router function.
     *
     * @param predicate the predicate that applies to the nested router functions
     * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
     */
    void startNested(RequestPredicate predicate);

    /**
     * Receive notification of the end of a nested router function.
     *
     * @param predicate the predicate that applies to the nested router functions
     * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
     */
    void endNested(RequestPredicate predicate);

    /**
     * Receive notification of a standard predicated route to a handler function.
     *
     * @param predicate the predicate that applies to the handler function
     * @param handlerFunction the handler function.
     * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
     */
    void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction);

    /**
     * Receive notification of a resource router function.
     *
     * @param lookupFunction the lookup function for the resources
     * @see RouterFunctions#resources(Function)
     */
    void resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

    /**
     * Receive notification of a router function with attributes. The
     * given attributes apply to the router notification that follows this one.
     *
     * @param attributes the attributes that apply to the following router
     */
    void attributes(Map<String, Object> attributes);

    /**
     * Receive notification of an unknown router function. This method is called for router
     * functions that were not created via the various {@link RouterFunctions} methods.
     *
     * @param routerFunction the router function
     */
    void unknown(RouterFunction<?> routerFunction);
  }

  abstract static class AbstractRouterFunction<T extends ServerResponse> implements RouterFunction<T> {

    @Override
    public String toString() {
      ToStringVisitor visitor = new ToStringVisitor();
      accept(visitor);
      return visitor.toString();
    }
  }

  /**
   * A composed routing function that first invokes one function, and then invokes the
   * another function (of the same response type {@code T}) if this route had
   * {@linkplain Optional#empty() no result}.
   *
   * @param <T> the server response type
   */
  static final class SameComposedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

    private final RouterFunction<T> first;
    private final RouterFunction<T> second;

    public SameComposedRouterFunction(RouterFunction<T> first, RouterFunction<T> second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public Optional<HandlerFunction<T>> route(ServerRequest request) {
      Optional<HandlerFunction<T>> firstRoute = this.first.route(request);
      if (firstRoute.isPresent()) {
        return firstRoute;
      }
      else {
        return this.second.route(request);
      }
    }

    @Override
    public void accept(Visitor visitor) {
      this.first.accept(visitor);
      this.second.accept(visitor);
    }
  }

  /**
   * A composed routing function that first invokes one function, and then invokes
   * another function (of a different response type) if this route had
   * {@linkplain Optional#empty() no result}.
   */
  static final class DifferentComposedRouterFunction extends AbstractRouterFunction<ServerResponse> {

    private final RouterFunction<?> first;
    private final RouterFunction<?> second;

    public DifferentComposedRouterFunction(RouterFunction<?> first, RouterFunction<?> second) {
      this.first = first;
      this.second = second;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
      Optional<? extends HandlerFunction<?>> firstRoute = this.first.route(request);
      if (firstRoute.isPresent()) {
        return (Optional<HandlerFunction<ServerResponse>>) firstRoute;
      }
      else {
        Optional<? extends HandlerFunction<?>> secondRoute = this.second.route(request);
        return (Optional<HandlerFunction<ServerResponse>>) secondRoute;
      }
    }

    @Override
    public void accept(Visitor visitor) {
      this.first.accept(visitor);
      this.second.accept(visitor);
    }
  }

  /**
   * Filter the specified {@linkplain HandlerFunction handler functions} with the given
   * {@linkplain HandlerFilterFunction filter function}.
   *
   * @param <T> the type of the {@linkplain HandlerFunction handler function} to filter
   * @param <S> the type of the response of the function
   */
  static final class FilteredRouterFunction<T extends ServerResponse, S extends ServerResponse>
          implements RouterFunction<S> {

    private final RouterFunction<T> routerFunction;
    private final HandlerFilterFunction<T, S> filterFunction;

    public FilteredRouterFunction(
            RouterFunction<T> routerFunction,
            HandlerFilterFunction<T, S> filterFunction) {
      this.routerFunction = routerFunction;
      this.filterFunction = filterFunction;
    }

    @Override
    public Optional<HandlerFunction<S>> route(ServerRequest request) {
      return this.routerFunction.route(request).map(this.filterFunction::apply);
    }

    @Override
    public void accept(Visitor visitor) {
      this.routerFunction.accept(visitor);
    }

    @Override
    public String toString() {
      return this.routerFunction.toString();
    }
  }

  private static final class DefaultRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

    private final RequestPredicate predicate;
    private final HandlerFunction<T> handlerFunction;

    public DefaultRouterFunction(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
      Assert.notNull(predicate, "Predicate is required");
      Assert.notNull(handlerFunction, "HandlerFunction is required");
      this.predicate = predicate;
      this.handlerFunction = handlerFunction;
    }

    @Override
    public Optional<HandlerFunction<T>> route(ServerRequest request) {
      if (predicate.test(request)) {
        if (log.isTraceEnabled()) {
          log.trace("Predicate \"{}\" matches against \"{}\"", predicate, request);
        }
        return Optional.of(handlerFunction);
      }
      else {
        return Optional.empty();
      }
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.route(predicate, handlerFunction);
    }

  }

  private static final class DefaultNestedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

    private final RequestPredicate predicate;
    private final RouterFunction<T> routerFunction;

    public DefaultNestedRouterFunction(RequestPredicate predicate, RouterFunction<T> routerFunction) {
      Assert.notNull(predicate, "Predicate is required");
      Assert.notNull(routerFunction, "RouterFunction is required");
      this.predicate = predicate;
      this.routerFunction = routerFunction;
    }

    @Override
    public Optional<HandlerFunction<T>> route(ServerRequest serverRequest) {
      return predicate.nest(serverRequest)
              .map(nestedRequest -> {
                        if (log.isTraceEnabled()) {
                          log.trace("Nested predicate \"{}\" matches against \"{}\"",
                                  predicate, serverRequest);
                        }
                        var result = routerFunction.route(nestedRequest);
                        if (result.isPresent() && nestedRequest != serverRequest) {
                          serverRequest.attributes().clear();
                          serverRequest.attributes().putAll(nestedRequest.attributes());
                        }
                        return result;
                      }
              )
              .orElse(Optional.empty());
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.startNested(predicate);
      routerFunction.accept(visitor);
      visitor.endNested(predicate);
    }

  }

  private static class ResourcesRouterFunction extends AbstractRouterFunction<ServerResponse> {

    private final Function<ServerRequest, Optional<Resource>> lookupFunction;

    public ResourcesRouterFunction(Function<ServerRequest, Optional<Resource>> lookupFunction) {
      Assert.notNull(lookupFunction, "Function is required");
      this.lookupFunction = lookupFunction;
    }

    @Override
    public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
      return lookupFunction.apply(request).map(ResourceHandlerFunction::new);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.resources(lookupFunction);
    }
  }

  static final class AttributesRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

    private final RouterFunction<T> delegate;
    private final Map<String, Object> attributes;

    public AttributesRouterFunction(RouterFunction<T> delegate, Map<String, Object> attributes) {
      this.delegate = delegate;
      this.attributes = initAttributes(attributes);
    }

    private static Map<String, Object> initAttributes(Map<String, Object> attributes) {
      if (attributes.isEmpty()) {
        return Collections.emptyMap();
      }
      else {
        return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
      }
    }

    @Override
    public Optional<HandlerFunction<T>> route(ServerRequest request) {
      return delegate.route(request);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.attributes(attributes);
      delegate.accept(visitor);
    }

    @Override
    public RouterFunction<T> withAttribute(String name, Object value) {
      Assert.hasLength(name, "Name must not be empty");
      Assert.notNull(value, "Value is required");

      Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
      attributes.put(name, value);
      return new AttributesRouterFunction<>(delegate, attributes);
    }

    @Override
    public RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
      Assert.notNull(attributesConsumer, "AttributesConsumer is required");

      Map<String, Object> attributes = new LinkedHashMap<>(this.attributes);
      attributesConsumer.accept(attributes);
      return new AttributesRouterFunction<>(delegate, attributes);
    }
  }

}

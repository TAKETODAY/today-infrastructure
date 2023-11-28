/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.reactive.function.client;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Mono;

/**
 * Static factory methods providing access to built-in implementations of
 * {@link ExchangeFilterFunction} for basic authentication, error handling, etc.
 *
 * @author Rob Winch
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ExchangeFilterFunctions {

  /**
   * Name of the request attribute with {@link Credentials} for {@link #basicAuthentication()}.
   */
  private static final String BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE =
      ExchangeFilterFunctions.class.getName() + ".basicAuthenticationCredentials";

  /**
   * Consume up to the specified number of bytes from the response body and
   * cancel if any more data arrives.
   * <p>Internally delegates to {@link DataBufferUtils#takeUntilByteCount}.
   *
   * @param maxByteCount the limit as number of bytes
   * @return the filter to limit the response size with
   */
  public static ExchangeFilterFunction limitResponseSize(long maxByteCount) {
    return (request, next) ->
        next.exchange(request).map(response ->
            response.mutate()
                .body(body -> DataBufferUtils.takeUntilByteCount(body, maxByteCount))
                .build());
  }

  /**
   * Return a filter that generates an error signal when the given
   * {@link HttpStatus} predicate matches.
   *
   * @param statusPredicate the predicate to check the HTTP status with
   * @param exceptionFunction the function that to create the exception
   * @return the filter to generate an error signal
   */
  public static ExchangeFilterFunction statusError(Predicate<HttpStatusCode> statusPredicate,
      Function<ClientResponse, ? extends Throwable> exceptionFunction) {

    Assert.notNull(statusPredicate, "Predicate is required");
    Assert.notNull(exceptionFunction, "Function is required");

    return ExchangeFilterFunction.ofResponseProcessor(
        response -> (statusPredicate.test(response.statusCode()) ?
                     Mono.error(exceptionFunction.apply(response)) : Mono.just(response)));
  }

  /**
   * Return a filter that applies HTTP Basic Authentication to the request
   * headers via {@link HttpHeaders#setBasicAuth(String)} and
   * {@link HttpHeaders#encodeBasicAuth(String, String, Charset)}.
   *
   * @param username the username
   * @param password the password
   * @return the filter to add authentication headers with
   * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
   * @see HttpHeaders#setBasicAuth(String)
   */
  public static ExchangeFilterFunction basicAuthentication(String username, String password) {
    String encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, null);
    return (request, next) ->
        next.exchange(ClientRequest.from(request)
            .headers(headers -> headers.setBasicAuth(encodedCredentials))
            .build());
  }

  /**
   * Variant of {@link #basicAuthentication(String, String)} that looks up
   * the {@link Credentials Credentials} in a
   * {@link #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE request attribute}.
   *
   * @return the filter to use
   * @see Credentials
   */
  public static ExchangeFilterFunction basicAuthentication() {
    return (request, next) -> {
      Object attr = request.attributes().get(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE);
      if (attr instanceof Credentials cred) {
        return next.exchange(ClientRequest.from(request)
            .headers(headers -> headers.setBasicAuth(cred.username, cred.password))
            .build());
      }
      else {
        return next.exchange(request);
      }
    };
  }

  /**
   * Stores username and password for HTTP basic authentication.
   */
  public record Credentials(String username, String password) {

    /**
     * Create a new {@code Credentials} instance with the given username and password.
     *
     * @param username the username
     * @param password the password
     */
    public Credentials {
      Assert.notNull(username, "'username' is required");
      Assert.notNull(password, "'password' is required");
    }

    /**
     * Return a {@literal Consumer} that stores the given username and password
     * as a request attribute of type {@code Credentials} that is in turn
     * used by {@link ExchangeFilterFunctions#basicAuthentication()}.
     *
     * @param username the username
     * @param password the password
     * @return a consumer that can be passed into
     * {@linkplain ClientRequest.Builder#attributes(Consumer)}
     * @see ClientRequest.Builder#attributes(Consumer)
     * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
     */
    public static Consumer<Map<String, Object>> basicAuthenticationCredentials(String username, String password) {
      Credentials credentials = new Credentials(username, password);
      return map -> map.put(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE, credentials);
    }

  }

}

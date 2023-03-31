/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.reactive.server;

import java.util.function.Consumer;

import reactor.core.publisher.Flux;

/**
 * {@code ExchangeResult} variant with the response body decoded as
 * {@code Flux<T>} but not yet consumed.
 *
 * @param <T> the type of elements in the response body
 * @author Rossen Stoyanchev
 * @see EntityExchangeResult
 * @since 4.0
 */
public class FluxExchangeResult<T> extends ExchangeResult {

  private final Flux<T> body;

  FluxExchangeResult(ExchangeResult result, Flux<T> body) {
    super(result);
    this.body = body;
  }

  /**
   * Return the response body as a {@code Flux<T>} of decoded elements.
   *
   * <p>The response body stream can then be consumed further with the
   * "reactor-test" {@code StepVerifier} and cancelled when enough elements have been
   * consumed from the (possibly infinite) stream:
   *
   * <pre>
   * FluxExchangeResult&lt;Person&gt; result = this.client.get()
   * 	.uri("/persons")
   * 	.accept(TEXT_EVENT_STREAM)
   * 	.exchange()
   * 	.expectStatus().isOk()
   * 	.expectHeader().contentType(TEXT_EVENT_STREAM)
   * 	.expectBody(Person.class)
   * 	.returnResult();
   *
   * StepVerifier.create(result.getResponseBody())
   * 	.expectNext(new Person("Jane"), new Person("Jason"))
   * 	.expectNextCount(4)
   * 	.expectNext(new Person("Jay"))
   * 	.thenCancel()
   * 	.verify();
   * </pre>
   */
  public Flux<T> getResponseBody() {
    return this.body;
  }

  /**
   * Invoke the given consumer within {@link #assertWithDiagnostics(Runnable)}
   * passing {@code "this"} instance to it. This method allows the following,
   * without leaving the {@code WebTestClient} chain of calls:
   * <pre class="code">
   * 	client.get()
   * 		.uri("/persons")
   * 		.accept(TEXT_EVENT_STREAM)
   * 		.exchange()
   * 		.expectStatus().isOk()
   * 	 	.returnResult()
   * 	 	.consumeWith(result -&gt; assertThat(...);
   * </pre>
   *
   * @param consumer the consumer for {@code "this"} instance
   */
  public void consumeWith(Consumer<FluxExchangeResult<T>> consumer) {
    assertWithDiagnostics(() -> consumer.accept(this));
  }

}

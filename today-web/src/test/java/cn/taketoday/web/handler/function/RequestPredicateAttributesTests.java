/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

class RequestPredicateAttributesTests {

  private DefaultServerRequest request;

  @BeforeEach
  public void createRequest() {

    ServletRequestContext context = new ServletRequestContext();
    context.setRequestURI("https://example.com/path");

    context.getAttributes().put("exchange", "bar");

    this.request = new DefaultServerRequest(context,
            Collections.singletonList(new StringHttpMessageConverter()));
  }

  @Test
  public void negateSucceed() {
    RequestPredicate predicate = new AddAttributePredicate(false, "predicate", "baz").negate();

    boolean result = predicate.test(this.request);
    assertThat(result).isTrue();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().get("predicate")).isEqualTo("baz");
  }

  @Test
  public void negateFail() {
    RequestPredicate predicate = new AddAttributePredicate(true, "predicate", "baz").negate();

    boolean result = predicate.test(this.request);
    assertThat(result).isFalse();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().containsKey("baz")).isFalse();
  }

  @Test
  public void andBothSucceed() {
    RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
    RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isTrue();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().get("left")).isEqualTo("baz");
    assertThat(this.request.attributes().get("right")).isEqualTo("qux");
  }

  @Test
  public void andLeftSucceed() {
    RequestPredicate left = new AddAttributePredicate(true, "left", "bar");
    RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isFalse();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().containsKey("left")).isFalse();
    assertThat(this.request.attributes().containsKey("right")).isFalse();
  }

  @Test
  public void andRightSucceed() {
    RequestPredicate left = new AddAttributePredicate(false, "left", "bar");
    RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isFalse();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().containsKey("left")).isFalse();
    assertThat(this.request.attributes().containsKey("right")).isFalse();
  }

  @Test
  public void andBothFail() {
    RequestPredicate left = new AddAttributePredicate(false, "left", "bar");
    RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.AndRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isFalse();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().containsKey("left")).isFalse();
    assertThat(this.request.attributes().containsKey("right")).isFalse();
  }

  @Test
  public void orBothSucceed() {
    RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
    RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isTrue();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().get("left")).isEqualTo("baz");
    assertThat(this.request.attributes().containsKey("right")).isFalse();
  }

  @Test
  public void orLeftSucceed() {
    RequestPredicate left = new AddAttributePredicate(true, "left", "baz");
    RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isTrue();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().get("left")).isEqualTo("baz");
    assertThat(this.request.attributes().containsKey("right")).isFalse();
  }

  @Test
  public void orRightSucceed() {
    RequestPredicate left = new AddAttributePredicate(false, "left", "baz");
    RequestPredicate right = new AddAttributePredicate(true, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isTrue();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().containsKey("left")).isFalse();
    assertThat(this.request.attributes().get("right")).isEqualTo("qux");
  }

  @Test
  public void orBothFail() {
    RequestPredicate left = new AddAttributePredicate(false, "left", "baz");
    RequestPredicate right = new AddAttributePredicate(false, "right", "qux");
    RequestPredicate predicate = new RequestPredicates.OrRequestPredicate(left, right);

    boolean result = predicate.test(this.request);
    assertThat(result).isFalse();

    assertThat(this.request.attributes().get("exchange")).isEqualTo("bar");
    assertThat(this.request.attributes().containsKey("baz")).isFalse();
    assertThat(this.request.attributes().containsKey("quux")).isFalse();
  }

  private static class AddAttributePredicate extends RequestPredicates.RequestModifyingPredicate {

    private final boolean result;

    private final String key;

    private final String value;

    public AddAttributePredicate(boolean result, String key, String value) {
      this.result = result;
      this.key = key;
      this.value = value;
    }

    @Override
    protected Result testInternal(ServerRequest request) {
      return Result.of(this.result, serverRequest -> serverRequest.attributes().put(this.key, this.value));
    }
  }

}

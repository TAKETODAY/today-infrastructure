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

package infra.web.util;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
public class UriTemplateTests {

  @Test
  void emptyPathDoesNotThrowException() {
    assertThatNoException().isThrownBy(() -> new UriTemplate(""));
  }

  @Test
  void nullPathThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new UriTemplate(null));
  }

  @Test
  void getVariableNames() {
    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    List<String> variableNames = template.getVariableNames();
    assertThat(variableNames).as("Invalid variable names").containsExactly("hotel", "booking");
  }

  @Test
  void getVariableNamesFromEmpty() {
    UriTemplate template = new UriTemplate("");
    List<String> variableNames = template.getVariableNames();
    assertThat(variableNames).isEmpty();
  }

  @Test
  void expandVarArgs() {
    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    URI result = template.expand("1", "42");
    assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotels/1/bookings/42"));
  }

  @Test
  void expandVarArgsFromEmpty() {
    UriTemplate template = new UriTemplate("");
    URI result = template.expand();
    assertThat(result).as("Invalid expanded template").isEqualTo(URI.create(""));

    result = template.expand("1", "42");
    assertThat(result).as("Invalid expanded template").isEqualTo(URI.create(""));
  }

  @Test
    // SPR-9712
  void expandVarArgsWithArrayValue() {
    UriTemplate template = new UriTemplate("/sum?numbers={numbers}");
    URI result = template.expand(new int[] { 1, 2, 3 });
    assertThat(result).isEqualTo(URI.create("/sum?numbers=1,2,3"));
  }

  @Test
  void expandVarArgsNotEnoughVariables() {
    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    assertThatIllegalArgumentException().isThrownBy(() -> template.expand("1"));
  }

  @Test
  void expandMap() {
    Map<String, String> uriVariables = Map.of("booking", "42", "hotel", "1");
    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    URI result = template.expand(uriVariables);
    assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotels/1/bookings/42"));
  }

  @Test
  void expandMapDuplicateVariables() {
    UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
    assertThat(template.getVariableNames()).containsExactly("c", "c", "c");
    URI result = template.expand(Map.of("c", "cheeseburger"));
    assertThat(result).isEqualTo(URI.create("/order/cheeseburger/cheeseburger/cheeseburger"));
  }

  @Test
  void expandMapNonString() {
    Map<String, Integer> uriVariables = Map.of("booking", 42, "hotel", 1);
    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    URI result = template.expand(uriVariables);
    assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotels/1/bookings/42"));
  }

  @Test
  void expandMapEncoded() {
    Map<String, String> uriVariables = Map.of("hotel", "Z\u00fcrich");
    UriTemplate template = new UriTemplate("/hotel list/{hotel}");
    URI result = template.expand(uriVariables);
    assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotel%20list/Z%C3%BCrich"));
  }

  @Test
  void expandMapUnboundVariables() {
    Map<String, String> uriVariables = Map.of("booking", "42", "bar", "1");
    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    assertThatIllegalArgumentException().isThrownBy(() -> template.expand(uriVariables));
  }

  @Test
  void expandEncoded() {
    UriTemplate template = new UriTemplate("/hotel list/{hotel}");
    URI result = template.expand("Z\u00fcrich");
    assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotel%20list/Z%C3%BCrich"));
  }

  @Test
  void matches() {
    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    assertThat(template.matches("/hotels/1/bookings/42")).as("UriTemplate does not match").isTrue();
    assertThat(template.matches("/hotels/bookings")).as("UriTemplate matches").isFalse();
    assertThat(template.matches("")).as("UriTemplate matches").isFalse();
    assertThat(template.matches(null)).as("UriTemplate matches").isFalse();
  }

  @Test
  void matchesAgainstEmpty() {
    UriTemplate template = new UriTemplate("");
    assertThat(template.matches("/hotels/1/bookings/42")).as("UriTemplate matches").isFalse();
    assertThat(template.matches("/hotels/bookings")).as("UriTemplate matches").isFalse();
    assertThat(template.matches("")).as("UriTemplate does not match").isTrue();
    assertThat(template.matches(null)).as("UriTemplate matches").isFalse();
  }

  @Test
  void matchesCustomRegex() {
    UriTemplate template = new UriTemplate("/hotels/{hotel:\\d+}");
    assertThat(template.matches("/hotels/42")).as("UriTemplate does not match").isTrue();
    assertThat(template.matches("/hotels/foo")).as("UriTemplate matches").isFalse();
  }

  @Test
  void match() {
    Map<String, String> expected = Map.of("booking", "42", "hotel", "1");

    UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
    Map<String, String> result = template.match("/hotels/1/bookings/42");
    assertThat(result).as("Invalid match").isEqualTo(expected);
  }

  @Test
  void matchAgainstEmpty() {
    UriTemplate template = new UriTemplate("");
    Map<String, String> result = template.match("/hotels/1/bookings/42");
    assertThat(result).as("Invalid match").isEmpty();
  }

  @Test
  void matchCustomRegex() {
    Map<String, String> expected = Map.of("booking", "42", "hotel", "1");

    UriTemplate template = new UriTemplate("/hotels/{hotel:\\d}/bookings/{booking:\\d+}");
    Map<String, String> result = template.match("/hotels/1/bookings/42");
    assertThat(result).as("Invalid match").isEqualTo(expected);
  }

  @Test
    // SPR-13627
  void matchCustomRegexWithNestedCurlyBraces() {
    UriTemplate template = new UriTemplate("/site.{domain:co.[a-z]{2}}");
    Map<String, String> result = template.match("/site.co.eu");
    assertThat(result).as("Invalid match").isEqualTo(Map.of("domain", "co.eu"));
  }

  @Test
  void matchDuplicate() {
    UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
    Map<String, String> result = template.match("/order/cheeseburger/cheeseburger/cheeseburger");
    assertThat(result).as("Invalid match").isEqualTo(Map.of("c", "cheeseburger"));
  }

  @Test
  void matchMultipleInOneSegment() {
    UriTemplate template = new UriTemplate("/{foo}-{bar}");
    Map<String, String> result = template.match("/12-34");
    Map<String, String> expected = Map.of("foo", "12", "bar", "34");
    assertThat(result).as("Invalid match").isEqualTo(expected);
  }

  @Test
    // SPR-16169
  void matchWithMultipleSegmentsAtTheEnd() {
    UriTemplate template = new UriTemplate("/account/{accountId}");
    assertThat(template.matches("/account/15/alias/5")).isFalse();
  }

  @Test
  void queryVariables() {
    UriTemplate template = new UriTemplate("/search?q={query}");
    assertThat(template.matches("/search?q=foo")).isTrue();
  }

  @Test
  void fragments() {
    UriTemplate template = new UriTemplate("/search#{fragment}");
    assertThat(template.matches("/search#foo")).isTrue();

    template = new UriTemplate("/search?query={query}#{fragment}");
    assertThat(template.matches("/search?query=foo#bar")).isTrue();
  }

  @Test
    // SPR-13705
  void matchesWithSlashAtTheEnd() {
    assertThat(new UriTemplate("/test/").matches("/test/")).isTrue();
  }

  @Test
  void expandWithDollar() {
    UriTemplate template = new UriTemplate("/{a}");
    URI uri = template.expand("$replacement");
    assertThat(uri).hasToString("/$replacement");
  }

  @Test
  void expandWithAtSign() {
    UriTemplate template = new UriTemplate("http://localhost/query={query}");
    URI uri = template.expand("foo@bar");
    assertThat(uri).hasToString("http://localhost/query=foo@bar");
  }

}

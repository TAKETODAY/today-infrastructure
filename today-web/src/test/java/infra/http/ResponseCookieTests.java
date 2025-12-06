/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ResponseCookie}.
 *
 * @author Rossen Stoyanchev
 */
class ResponseCookieTests {

  @Test
  void basic() {

    assertThat(ResponseCookie.from("id", null).build().toString()).isEqualTo("id=");
    assertThat(ResponseCookie.from("id", "1fWa").build().toString()).isEqualTo("id=1fWa");

    ResponseCookie cookie = ResponseCookie.from("id", "1fWa")
            .domain("abc").path("/path").maxAge(0).httpOnly(true).partitioned(true).secure(true).sameSite("None")
            .build();

    assertThat(cookie.toString()).isEqualTo("id=1fWa; Path=/path; Domain=abc; " +
            "Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; " +
            "Secure; HttpOnly; Partitioned; SameSite=None");
  }

  @Test
  void nameChecks() {

    Arrays.asList("id", "i.d.", "i-d", "+id", "i*d", "i$d", "#id")
            .forEach(name -> ResponseCookie.from(name, "value").build());

    Arrays.asList("\"id\"", "id\t", "i\td", "i d", "i;d", "{id}", "[id]", "\"", "id\u0091")
            .forEach(name -> assertThatThrownBy(() -> ResponseCookie.from(name, "value").build())
                    .hasMessageContaining("RFC2616 token"));
  }

  @Test
  void valueChecks() {

    Arrays.asList("1fWa", "", null, "1f=Wa", "1f-Wa", "1f/Wa", "1.f.W.a.")
            .forEach(value -> ResponseCookie.from("id", value).build());

    Arrays.asList("1f\tWa", "\t", "1f Wa", "1f;Wa", "\"1fWa", "1f\\Wa", "1f\"Wa", "\"", "1fWa\u0005", "1f\u0091Wa")
            .forEach(value -> assertThatThrownBy(() -> ResponseCookie.from("id", value).build())
                    .hasMessageContaining("RFC2616 cookie value"));
  }

  @Test
  void domainChecks() {

    Arrays.asList("abc", "abc.org", "abc-def.org", "abc3.org", ".abc.org")
            .forEach(domain -> ResponseCookie.from("n", "v").domain(domain).build());

    Arrays.asList("-abc.org", "abc.org.", "abc.org-")
            .forEach(domain -> assertThatThrownBy(() -> ResponseCookie.from("n", "v").domain(domain).build())
                    .hasMessageContaining("Invalid first/last char"));

    Arrays.asList("abc..org", "abc.-org", "abc-.org")
            .forEach(domain -> assertThatThrownBy(() -> ResponseCookie.from("n", "v").domain(domain).build())
                    .hasMessageContaining("invalid cookie domain char"));
  }

  @Test
  void domainWithEmptyDoubleQuotes() {

    Arrays.asList("\"\"", "\t\"\" ", " \" \t \"\t")
            .forEach(domain -> {
              ResponseCookie cookie = ResponseCookie.fromClientResponse("id", "1fWa").domain(domain).build();
              assertThat(cookie.getDomain()).isNull();
            });

  }

  @Test
  void equalsAndHashCode_withSameNamePathAndDomain_shouldBeEqual() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "1fWa").domain("abc").path("/path").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "1fWa").domain("abc").path("/path").build();

    assertThat(cookie1).isEqualTo(cookie2);
    assertThat(cookie1.hashCode()).isEqualTo(cookie2.hashCode());
  }

  @Test
  void equalsAndHashCode_withDifferentName_shouldNotBeEqual() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "1fWa").build();
    ResponseCookie cookie2 = ResponseCookie.from("id2", "1fWa").build();

    assertThat(cookie1).isNotEqualTo(cookie2);
  }

  @Test
  void equalsAndHashCode_withDifferentPath_shouldNotBeEqual() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "1fWa").path("/path1").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "1fWa").path("/path2").build();

    assertThat(cookie1).isNotEqualTo(cookie2);
  }

  @Test
  void equalsAndHashCode_withDifferentDomain_shouldNotBeEqual() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "1fWa").domain("abc.com").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "1fWa").domain("def.com").build();

    assertThat(cookie1).isNotEqualTo(cookie2);
  }

  @Test
  void equals_withNull_shouldNotBeEqual() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").build();
    assertThat(cookie).isNotEqualTo(null);
  }

  @Test
  void equals_withSameReference_shouldBeEqual() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").build();
    assertThat(cookie).isEqualTo(cookie);
  }

  @Test
  void equals_withDifferentClass_shouldNotBeEqual() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").build();
    assertThat(cookie).isNotEqualTo("different type");
  }

  @Test
  void equals_caseInsensitiveName_shouldBeEqual() {
    ResponseCookie cookie1 = ResponseCookie.from("ID", "1fWa").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "1fWa").build();

    assertThat(cookie1).isEqualTo(cookie2);
  }

  @Test
  void toString_withAllAttributes_shouldReturnFormattedString() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa")
            .domain("example.com")
            .path("/test")
            .maxAge(3600)
            .secure(true)
            .httpOnly(true)
            .partitioned(true)
            .sameSite("Strict")
            .build();

    String result = cookie.toString();
    assertThat(result).contains("id=1fWa");
    assertThat(result).contains("Domain=example.com");
    assertThat(result).contains("Path=/test");
    assertThat(result).contains("Max-Age=3600");
    assertThat(result).contains("Secure");
    assertThat(result).contains("HttpOnly");
    assertThat(result).contains("Partitioned");
    assertThat(result).contains("SameSite=Strict");
  }

  @Test
  void toString_withZeroMaxAge_shouldIncludeExpiresHeader() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").maxAge(0).build();

    String result = cookie.toString();
    assertThat(result).contains("Max-Age=0");
    assertThat(result).contains("Expires=Thu, 01 Jan 1970 00:00:00 GMT");
  }

  @Test
  void toString_withNegativeMaxAge_shouldNotIncludeMaxAgeAndExpires() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").maxAge(-1).build();

    String result = cookie.toString();
    assertThat(result).doesNotContain("Max-Age");
    assertThat(result).doesNotContain("Expires");
  }

  @Test
  void toString_withoutOptionalAttributes_shouldOnlyIncludeNameValue() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").build();

    String result = cookie.toString();
    assertThat(result).isEqualTo("id=1fWa");
  }

  @Test
  void getters_shouldReturnCorrectValues() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa")
            .domain("example.com")
            .path("/test")
            .maxAge(3600)
            .secure(true)
            .httpOnly(true)
            .partitioned(true)
            .sameSite("Lax")
            .build();

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
    assertThat(cookie.getDomain()).isEqualTo("example.com");
    assertThat(cookie.getPath()).isEqualTo("/test");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(3600));
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isPartitioned()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
  }

  @Test
  void getters_withDefaults_shouldReturnDefaultValues() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").build();

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isNull();
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(-1));
    assertThat(cookie.isSecure()).isFalse();
    assertThat(cookie.isHttpOnly()).isFalse();
    assertThat(cookie.isPartitioned()).isFalse();
    assertThat(cookie.getSameSite()).isNull();
  }

  @Test
  void fromClientResponse_withLenientDomain_shouldHandleEmptyQuotes() {
    ResponseCookie cookie = ResponseCookie.fromClientResponse("id", "1fWa").domain("\"\"").build();
    assertThat(cookie.getDomain()).isNull();
  }

  @Test
  void fromClientResponse_withNormalDomain_shouldPreserveDomain() {
    ResponseCookie cookie = ResponseCookie.fromClientResponse("id", "1fWa").domain("example.com").build();
    assertThat(cookie.getDomain()).isEqualTo("example.com");
  }

  @Test
  void mutate_shouldCreateBuilderWithSameValues() {
    ResponseCookie original = ResponseCookie.from("id", "1fWa")
            .domain("example.com")
            .path("/test")
            .maxAge(3600)
            .secure(true)
            .httpOnly(true)
            .partitioned(true)
            .sameSite("Strict")
            .build();

    ResponseCookie mutated = original.mutate().build();

    assertThat(mutated.getName()).isEqualTo(original.getName());
    assertThat(mutated.getValue()).isEqualTo(original.getValue());
    assertThat(mutated.getDomain()).isEqualTo(original.getDomain());
    assertThat(mutated.getPath()).isEqualTo(original.getPath());
    assertThat(mutated.getMaxAge()).isEqualTo(original.getMaxAge());
    assertThat(mutated.isSecure()).isEqualTo(original.isSecure());
    assertThat(mutated.isHttpOnly()).isEqualTo(original.isHttpOnly());
    assertThat(mutated.isPartitioned()).isEqualTo(original.isPartitioned());
    assertThat(mutated.getSameSite()).isEqualTo(original.getSameSite());
  }

  @Test
  void mutate_shouldAllowModifications() {
    ResponseCookie original = ResponseCookie.from("id", "1fWa").build();
    ResponseCookie modified = original.mutate().value("modified").maxAge(1800).build();

    assertThat(modified.getName()).isEqualTo(original.getName());
    assertThat(modified.getValue()).isEqualTo("modified");
    assertThat(modified.getMaxAge()).isEqualTo(Duration.ofSeconds(1800));
  }

  @Test
  void maxAge_withDuration_shouldSetCorrectly() {
    Duration duration = Duration.ofHours(1);
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").maxAge(duration).build();

    assertThat(cookie.getMaxAge()).isEqualTo(duration);
  }

  @Test
  void maxAge_withPositiveSeconds_shouldConvertToDuration() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").maxAge(3600L).build();

    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(3600));
  }

  @Test
  void maxAge_withNegativeSeconds_shouldConvertToNegativeOne() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").maxAge(-1L).build();

    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(-1));
  }

  @Test
  void maxAge_withZeroSeconds_shouldConvertToZero() {
    ResponseCookie cookie = ResponseCookie.from("id", "1fWa").maxAge(0L).build();

    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(0));
  }

  @Test
  void pathChecks_withInvalidChars_shouldThrowException() {
    assertThatThrownBy(() -> ResponseCookie.from("id", "value").path("/path\twith\tabs").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cookie path char");

    assertThatThrownBy(() -> ResponseCookie.from("id", "value").path("/path;semicolon").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cookie path char");
  }

  @Test
  void pathChecks_withValidChars_shouldSucceed() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "value").path("/valid-path_123").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "value").path("/path.with.dots").build();

    assertThat(cookie1.getPath()).isEqualTo("/valid-path_123");
    assertThat(cookie2.getPath()).isEqualTo("/path.with.dots");
  }

  @Test
  void forSimple_shouldCreateCookie() {
    ResponseCookie cookie = ResponseCookie.forSimple("id", "1fWa");

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isNull();
  }

  @Test
  void sameSite_attribute_shouldBeSetCorrectly() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "value").sameSite("Strict").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "value").sameSite("Lax").build();
    ResponseCookie cookie3 = ResponseCookie.from("id", "value").sameSite("None").build();
    ResponseCookie cookie4 = ResponseCookie.from("id", "value").sameSite(null).build();

    assertThat(cookie1.getSameSite()).isEqualTo("Strict");
    assertThat(cookie2.getSameSite()).isEqualTo("Lax");
    assertThat(cookie3.getSameSite()).isEqualTo("None");
    assertThat(cookie4.getSameSite()).isNull();
  }

  @Test
  void from_withHttpCookie_shouldCreateBuilderWithSameAttributes() {
    java.net.HttpCookie httpCookie = new java.net.HttpCookie("id", "1fWa");
    httpCookie.setDomain("example.com");
    httpCookie.setPath("/test");
    httpCookie.setMaxAge(3600);
    httpCookie.setSecure(true);
    httpCookie.setHttpOnly(true);

    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(httpCookie);
    ResponseCookie cookie = builder.build();

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
    assertThat(cookie.getDomain()).isEqualTo("example.com");
    assertThat(cookie.getPath()).isEqualTo("/test");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(3600));
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
  }

  @Test
  void domain_withLeadingDot_shouldBeAccepted() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").domain(".example.com").build();
    assertThat(cookie.getDomain()).isEqualTo(".example.com");
  }

  @Test
  void domain_withValidChars_shouldBeAccepted() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").domain("my-domain123.org").build();
    assertThat(cookie.getDomain()).isEqualTo("my-domain123.org");
  }

  @Test
  void path_withValidSpecialChars_shouldBeAccepted() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").path("/path_with-dots.and_underscores").build();
    assertThat(cookie.getPath()).isEqualTo("/path_with-dots.and_underscores");
  }

  @Test
  void path_withForwardSlashAndAlphanumeric_shouldBeAccepted() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").path("/api/v1/users/123").build();
    assertThat(cookie.getPath()).isEqualTo("/api/v1/users/123");
  }

  @Test
  void toString_withPositiveMaxAge_shouldIncludeExpiresWithFutureDate() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").maxAge(3600).build();
    String result = cookie.toString();

    assertThat(result).contains("Max-Age=3600");
    assertThat(result).contains("Expires=");
    assertThat(result).doesNotContain("Expires=Thu, 01 Jan 1970 00:00:00 GMT");
  }

  @Test
  void toString_withPartitionedAttribute_shouldIncludePartitionedFlag() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").partitioned(true).build();
    String result = cookie.toString();

    assertThat(result).contains("Partitioned");
  }

  @Test
  void toString_withoutPartitionedAttribute_shouldNotIncludePartitionedFlag() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").partitioned(false).build();
    String result = cookie.toString();

    assertThat(result).doesNotContain("Partitioned");
  }

  @Test
  void toString_withSecureAttribute_shouldIncludeSecureFlag() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").secure(true).build();
    String result = cookie.toString();

    assertThat(result).contains("Secure");
  }

  @Test
  void toString_withoutSecureAttribute_shouldNotIncludeSecureFlag() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").secure(false).build();
    String result = cookie.toString();

    assertThat(result).doesNotContain("Secure");
  }

  @Test
  void toString_withHttpOnlyAttribute_shouldIncludeHttpOnlyFlag() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").httpOnly(true).build();
    String result = cookie.toString();

    assertThat(result).contains("HttpOnly");
  }

  @Test
  void toString_withoutHttpOnlyAttribute_shouldNotIncludeHttpOnlyFlag() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").httpOnly(false).build();
    String result = cookie.toString();

    assertThat(result).doesNotContain("HttpOnly");
  }

  @Test
  void toString_withSameSiteAttribute_shouldIncludeSameSiteDirective() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").sameSite("Strict").build();
    String result = cookie.toString();

    assertThat(result).contains("SameSite=Strict");
  }

  @Test
  void toString_withoutSameSiteAttribute_shouldNotIncludeSameSiteDirective() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").sameSite(null).build();
    String result = cookie.toString();

    assertThat(result).doesNotContain("SameSite");
  }

  @Test
  void initDomain_withLenientModeAndEmptyDoubleQuotes_shouldReturnNull() {
    ResponseCookie cookie = ResponseCookie.fromClientResponse("id", "value").domain("\"\"").build();
    assertThat(cookie.getDomain()).isNull();
  }

  @Test
  void initDomain_withLenientModeAndWhitespaceAroundDoubleQuotes_shouldReturnNull() {
    ResponseCookie cookie = ResponseCookie.fromClientResponse("id", "value").domain(" \"\" ").build();
    assertThat(cookie.getDomain()).isNull();
  }

  @Test
  void validateCookieValue_withEmptyValue_shouldPassValidation() {
    ResponseCookie cookie = ResponseCookie.from("id", "").build();
    assertThat(cookie.getValue()).isEqualTo("");
  }

  @Test
  void validateCookieValue_withValidCharacters_shouldPassValidation() {
    ResponseCookie cookie = ResponseCookie.from("id", "ABC123-_.").build();
    assertThat(cookie.getValue()).isEqualTo("ABC123-_.");
  }

  @Test
  void validatePath_withNullPath_shouldPassValidation() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").path(null).build();
    assertThat(cookie.getPath()).isNull();
  }

  @Test
  void validateDomain_withNullDomain_shouldPassValidation() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").domain(null).build();
    assertThat(cookie.getDomain()).isNull();
  }

  @Test
  void validateDomain_withEmptyDomain_shouldPassValidation() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").domain("").build();
    assertThat(cookie.getDomain()).isEqualTo("");
  }

  @Test
  void forSimple_shouldCreateCookieUsingFromMethod() {
    ResponseCookie cookie = ResponseCookie.forSimple("id", "1fWa");

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isNull();
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(-1));
  }

  @Test
  void from_withNameOnly_shouldCreateBuilder() {
    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("id");
    ResponseCookie cookie = builder.value("1fWa").build();

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
  }

  @Test
  void from_withNameAndValue_shouldCreateBuilder() {
    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("id", "1fWa");
    ResponseCookie cookie = builder.build();

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
  }

  @Test
  void fromClientResponse_withNameAndValue_shouldCreateBuilder() {
    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.fromClientResponse("id", "1fWa");
    ResponseCookie cookie = builder.build();

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("1fWa");
  }

  @Test
  void value_method_shouldUpdateValue() {
    ResponseCookie original = ResponseCookie.from("id", "original").build();
    ResponseCookie updated = original.mutate().value("updated").build();

    assertThat(updated.getValue()).isEqualTo("updated");
    assertThat(original.getValue()).isEqualTo("original");
  }

  @Test
  void domain_method_shouldUpdateDomain() {
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().domain("example.com").build();

    assertThat(updated.getDomain()).isEqualTo("example.com");
    assertThat(original.getDomain()).isNull();
  }

  @Test
  void path_method_shouldUpdatePath() {
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().path("/newpath").build();

    assertThat(updated.getPath()).isEqualTo("/newpath");
    assertThat(original.getPath()).isNull();
  }

  @Test
  void secure_method_shouldUpdateSecureFlag() {
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().secure(true).build();

    assertThat(updated.isSecure()).isTrue();
    assertThat(original.isSecure()).isFalse();
  }

  @Test
  void httpOnly_method_shouldUpdateHttpOnlyFlag() {
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().httpOnly(true).build();

    assertThat(updated.isHttpOnly()).isTrue();
    assertThat(original.isHttpOnly()).isFalse();
  }

  @Test
  void partitioned_method_shouldUpdatePartitionedFlag() {
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().partitioned(true).build();

    assertThat(updated.isPartitioned()).isTrue();
    assertThat(original.isPartitioned()).isFalse();
  }

  @Test
  void sameSite_method_shouldUpdateSameSiteAttribute() {
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().sameSite("Lax").build();

    assertThat(updated.getSameSite()).isEqualTo("Lax");
    assertThat(original.getSameSite()).isNull();
  }

  @Test
  void maxAge_withDuration_method_shouldUpdateMaxAge() {
    Duration newMaxAge = Duration.ofMinutes(30);
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().maxAge(newMaxAge).build();

    assertThat(updated.getMaxAge()).isEqualTo(newMaxAge);
    assertThat(original.getMaxAge()).isEqualTo(Duration.ofSeconds(-1));
  }

  @Test
  void maxAge_withLong_method_shouldUpdateMaxAge() {
    ResponseCookie original = ResponseCookie.from("id", "value").build();
    ResponseCookie updated = original.mutate().maxAge(1800L).build();

    assertThat(updated.getMaxAge()).isEqualTo(Duration.ofSeconds(1800));
    assertThat(original.getMaxAge()).isEqualTo(Duration.ofSeconds(-1));
  }

  @Test
  void build_method_shouldCreateImmutableCookie() {
    ResponseCookie cookie = ResponseCookie.from("id", "value")
            .domain("example.com")
            .path("/test")
            .maxAge(3600)
            .secure(true)
            .httpOnly(true)
            .partitioned(true)
            .sameSite("Strict")
            .build();

    assertThat(cookie.getName()).isEqualTo("id");
    assertThat(cookie.getValue()).isEqualTo("value");
    assertThat(cookie.getDomain()).isEqualTo("example.com");
    assertThat(cookie.getPath()).isEqualTo("/test");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(3600));
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isPartitioned()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
  }

  @Test
  void equals_withSameInstance_shouldReturnTrue() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").build();
    assertThat(cookie).isEqualTo(cookie);
  }

  @Test
  void equals_withEquivalentCookies_shouldReturnTrue() {
    ResponseCookie cookie1 = ResponseCookie.from("ID", "value").domain("example.com").path("/test").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "value").domain("example.com").path("/test").build();

    assertThat(cookie1).isEqualTo(cookie2);
  }

  @Test
  void equals_withDifferentName_shouldReturnFalse() {
    ResponseCookie cookie1 = ResponseCookie.from("id1", "value").build();
    ResponseCookie cookie2 = ResponseCookie.from("id2", "value").build();

    assertThat(cookie1).isNotEqualTo(cookie2);
  }

  @Test
  void equals_withDifferentPath_shouldReturnFalse() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "value").path("/path1").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "value").path("/path2").build();

    assertThat(cookie1).isNotEqualTo(cookie2);
  }

  @Test
  void equals_withDifferentDomain_shouldReturnFalse() {
    ResponseCookie cookie1 = ResponseCookie.from("id", "value").domain("domain1.com").build();
    ResponseCookie cookie2 = ResponseCookie.from("id", "value").domain("domain2.com").build();

    assertThat(cookie1).isNotEqualTo(cookie2);
  }

  @Test
  void equals_withNull_shouldReturnFalse() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").build();
    assertThat(cookie).isNotEqualTo(null);
  }

  @Test
  void equals_withDifferentObjectType_shouldReturnFalse() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").build();
    assertThat(cookie).isNotEqualTo("string");
  }

  @Test
  void toString_withMinimalCookie_shouldOnlyShowNameValue() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").build();
    assertThat(cookie.toString()).isEqualTo("id=value");
  }

  @Test
  void toString_withMaxAgeZero_shouldIncludeExpiresEpoch() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").maxAge(0).build();
    assertThat(cookie.toString()).contains("Max-Age=0")
            .contains("Expires=Thu, 01 Jan 1970 00:00:00 GMT");
  }

  @Test
  void toString_withMaxAgeNegative_shouldNotIncludeMaxAgeOrExpires() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").maxAge(-1).build();
    String result = cookie.toString();
    assertThat(result).doesNotContain("Max-Age");
    assertThat(result).doesNotContain("Expires");
  }

  @Test
  void toString_withMaxAgePositive_shouldIncludeMaxAgeAndFutureExpires() {
    ResponseCookie cookie = ResponseCookie.from("id", "value").maxAge(3600).build();
    String result = cookie.toString();
    assertThat(result).contains("Max-Age=3600");
    assertThat(result).contains("Expires=");
    assertThat(result).doesNotContain("Expires=Thu, 01 Jan 1970 00:00:00 GMT");
  }

  @Test
  void validateCookieName_withValidNames_shouldPass() {
    assertThat(ResponseCookie.from("simple", "value").build().getName()).isEqualTo("simple");
    assertThat(ResponseCookie.from("with-dash", "value").build().getName()).isEqualTo("with-dash");
    assertThat(ResponseCookie.from("with.dot", "value").build().getName()).isEqualTo("with.dot");
    assertThat(ResponseCookie.from("with_underscore", "value").build().getName()).isEqualTo("with_underscore");
    assertThat(ResponseCookie.from("with*dollar", "value").build().getName()).isEqualTo("with*dollar");
    assertThat(ResponseCookie.from("with+plus", "value").build().getName()).isEqualTo("with+plus");
  }

  @Test
  void validateCookieName_withInvalidNames_shouldFail() {
    assertThatThrownBy(() -> ResponseCookie.from("with space", "value").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("with\ttab", "value").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("with\nnewline", "value").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("with=equals", "value").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("with,comma", "value").build())
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validateCookieValue_withValidValues_shouldPass() {
    assertThat(ResponseCookie.from("name", "simple").build().getValue()).isEqualTo("simple");
    assertThat(ResponseCookie.from("name", "").build().getValue()).isEqualTo("");
    assertThat(ResponseCookie.from("name", null).build().getValue()).isEmpty();
    assertThat(ResponseCookie.from("name", "with-dash").build().getValue()).isEqualTo("with-dash");
    assertThat(ResponseCookie.from("name", "with.dot").build().getValue()).isEqualTo("with.dot");
    assertThat(ResponseCookie.from("name", "with_underscore").build().getValue()).isEqualTo("with_underscore");
    assertThat(ResponseCookie.from("name", "with/slash").build().getValue()).isEqualTo("with/slash");
  }

  @Test
  void validateCookieValue_withInvalidValues_shouldFail() {
    assertThatThrownBy(() -> ResponseCookie.from("name", "with space").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "with\ttab").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "with\nnewline").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "with;semicolon").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "with\"quote").build())
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validateDomain_withValidDomains_shouldPass() {
    assertThat(ResponseCookie.from("name", "value").domain("example.com").build().getDomain()).isEqualTo("example.com");
    assertThat(ResponseCookie.from("name", "value").domain(".example.com").build().getDomain()).isEqualTo(".example.com");
    assertThat(ResponseCookie.from("name", "value").domain("sub.example.com").build().getDomain()).isEqualTo("sub.example.com");
    assertThat(ResponseCookie.from("name", "value").domain("example-domain.com").build().getDomain()).isEqualTo("example-domain.com");
  }

  @Test
  void validateDomain_withInvalidDomains_shouldFail() {
    assertThatThrownBy(() -> ResponseCookie.from("name", "value").domain("-example.com").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "value").domain("example.com-").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "value").domain("example..com").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "value").domain("example.-com").build())
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validatePath_withValidPaths_shouldPass() {
    assertThat(ResponseCookie.from("name", "value").path("/").build().getPath()).isEqualTo("/");
    assertThat(ResponseCookie.from("name", "value").path("/path").build().getPath()).isEqualTo("/path");
    assertThat(ResponseCookie.from("name", "value").path("/path/to/resource").build().getPath()).isEqualTo("/path/to/resource");
    assertThat(ResponseCookie.from("name", "value").path("/path-with-dashes").build().getPath()).isEqualTo("/path-with-dashes");
  }

  @Test
  void validatePath_withInvalidPaths_shouldFail() {
    assertThatThrownBy(() -> ResponseCookie.from("name", "value").path("/path\twith\ttabs").build())
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResponseCookie.from("name", "value").path("/path;with;semicolons").build())
            .isInstanceOf(IllegalArgumentException.class);
  }

}

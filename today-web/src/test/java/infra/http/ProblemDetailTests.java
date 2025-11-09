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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/3 12:37
 */
class ProblemDetailTests {

  @Test
  void equalsAndHashCode() {
    ProblemDetail pd1 = ProblemDetail.forRawStatusCode(500);
    ProblemDetail pd2 = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    ProblemDetail pd3 = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    ProblemDetail pd4 = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "some detail");

    assertThat(pd1).isEqualTo(pd2);
    assertThat(pd2).isEqualTo(pd1);
    assertThat(pd1.hashCode()).isEqualTo(pd2.hashCode());

    assertThat(pd3).isNotEqualTo(pd4);
    assertThat(pd4).isNotEqualTo(pd3);
    assertThat(pd3.hashCode()).isNotEqualTo(pd4.hashCode());

    assertThat(pd1).isNotEqualTo(pd3);
    assertThat(pd1).isNotEqualTo(pd4);
    assertThat(pd2).isNotEqualTo(pd3);
    assertThat(pd2).isNotEqualTo(pd4);
    assertThat(pd1.hashCode()).isNotEqualTo(pd3.hashCode());
    assertThat(pd1.hashCode()).isNotEqualTo(pd4.hashCode());
  }

  @Test
  void equalsAndHashCodeWithDeserialization() throws Exception {
    ProblemDetail originalDetail = ProblemDetail.forRawStatusCode(500);

    ObjectMapper mapper = new ObjectMapper();
    byte[] bytes = mapper.writeValueAsBytes(originalDetail);
    ProblemDetail deserializedDetail = mapper.readValue(bytes, ProblemDetail.class);

    assertThat(originalDetail).isEqualTo(deserializedDetail);
    assertThat(deserializedDetail).isEqualTo(originalDetail);
    assertThat(originalDetail.hashCode()).isEqualTo(deserializedDetail.hashCode());
  }

  @Test
  void constructor_withRawStatusCode_shouldSetStatus() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
    assertThat(problemDetail.getStatus()).isEqualTo(404);
  }

  @Test
  void constructor_withHttpStatusCode_shouldSetStatus() {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    assertThat(problemDetail.getStatus()).isEqualTo(404);
  }

  @Test
  void constructor_withStatusAndDetail_shouldSetBoth() {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid input");
    assertThat(problemDetail.getStatus()).isEqualTo(400);
    assertThat(problemDetail.getDetail()).isEqualTo("Invalid input");
  }

  @Test
  void setType_shouldSetType() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    URI type = URI.create("https://example.com/problems/out-of-credit");
    problemDetail.setType(type);
    assertThat(problemDetail.getType()).isEqualTo(type);
  }

  @Test
  void setTitle_shouldSetTitle() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    String title = "Internal Server Error";
    problemDetail.setTitle(title);
    assertThat(problemDetail.getTitle()).isEqualTo(title);
  }

  @Test
  void setStatus_withIntValue_shouldSetStatus() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(200);
    problemDetail.setStatus(500);
    assertThat(problemDetail.getStatus()).isEqualTo(500);
  }

  @Test
  void setStatus_withHttpStatus_shouldSetStatus() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(200);
    problemDetail.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(problemDetail.getStatus()).isEqualTo(500);
  }

  @Test
  void setDetail_shouldSetDetail() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    String detail = "Something went wrong";
    problemDetail.setDetail(detail);
    assertThat(problemDetail.getDetail()).isEqualTo(detail);
  }

  @Test
  void setInstance_shouldSetInstance() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    URI instance = URI.create("/account/12345/msgs/abc");
    problemDetail.setInstance(instance);
    assertThat(problemDetail.getInstance()).isEqualTo(instance);
  }

  @Test
  void setProperty_shouldAddProperty() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    problemDetail.setProperty("key1", "value1");
    problemDetail.setProperty("key2", 123);

    Map<String, Object> properties = problemDetail.getProperties();
    assertThat(properties).containsEntry("key1", "value1");
    assertThat(properties).containsEntry("key2", 123);
  }

  @Test
  void setProperties_shouldReplaceAllProperties() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    problemDetail.setProperty("oldKey", "oldValue");

    Map<String, Object> newProperties = new HashMap<>();
    newProperties.put("newKey1", "newValue1");
    newProperties.put("newKey2", 456);
    problemDetail.setProperties(newProperties);

    Map<String, Object> properties = problemDetail.getProperties();
    assertThat(properties).doesNotContainKey("oldKey");
    assertThat(properties).containsEntry("newKey1", "newValue1");
    assertThat(properties).containsEntry("newKey2", 456);
  }

  @Test
  void getTitle_whenNotSetButStatusKnown_shouldReturnReasonPhrase() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
    assertThat(problemDetail.getTitle()).isEqualTo("Not Found");
  }

  @Test
  void getTitle_whenNotSetAndStatusUnknown_shouldReturnNull() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(999);
    assertThat(problemDetail.getTitle()).isNull();
  }

  @Test
  void getTitle_whenSetTitle_shouldReturnSetTitle() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
    problemDetail.setTitle("Custom Title");
    assertThat(problemDetail.getTitle()).isEqualTo("Custom Title");
  }

  @Test
  void withType_shouldSetAndReturnSameInstance() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    URI type = URI.create("https://example.com/problems/out-of-credit");
    ProblemDetail result = problemDetail.withType(type);

    assertThat(result).isSameAs(problemDetail);
    assertThat(problemDetail.getType()).isEqualTo(type);
  }

  @Test
  void withTitle_shouldSetAndReturnSameInstance() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    ProblemDetail result = problemDetail.withTitle("Internal Server Error");

    assertThat(result).isSameAs(problemDetail);
    assertThat(problemDetail.getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void withStatus_HttpStatusCode_shouldSetAndReturnSameInstance() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(200);
    ProblemDetail result = problemDetail.withStatus(HttpStatus.INTERNAL_SERVER_ERROR);

    assertThat(result).isSameAs(problemDetail);
    assertThat(problemDetail.getStatus()).isEqualTo(500);
  }

  @Test
  void withRawStatusCode_shouldSetAndReturnSameInstance() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(200);
    ProblemDetail result = problemDetail.withRawStatusCode(500);

    assertThat(result).isSameAs(problemDetail);
    assertThat(problemDetail.getStatus()).isEqualTo(500);
  }

  @Test
  void withDetail_shouldSetAndReturnSameInstance() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    ProblemDetail result = problemDetail.withDetail("Something went wrong");

    assertThat(result).isSameAs(problemDetail);
    assertThat(problemDetail.getDetail()).isEqualTo("Something went wrong");
  }

  @Test
  void withInstance_shouldSetAndReturnSameInstance() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    URI instance = URI.create("/account/12345/msgs/abc");
    ProblemDetail result = problemDetail.withInstance(instance);

    assertThat(result).isSameAs(problemDetail);
    assertThat(problemDetail.getInstance()).isEqualTo(instance);
  }

  @Test
  void equalsAndHashCode_withSameValues_shouldBeEqual() {
    ProblemDetail pd1 = ProblemDetail.forRawStatusCode(500);
    ProblemDetail pd2 = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    ProblemDetail pd3 = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "some detail");
    ProblemDetail pd4 = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "some detail");

    assertThat(pd1).isEqualTo(pd2);
    assertThat(pd2).isEqualTo(pd1);
    assertThat(pd1.hashCode()).isEqualTo(pd2.hashCode());

    assertThat(pd3).isEqualTo(pd4);
    assertThat(pd4).isEqualTo(pd3);
    assertThat(pd3.hashCode()).isEqualTo(pd4.hashCode());
  }

  @Test
  void equalsAndHashCode_withDifferentValues_shouldNotBeEqual() {
    ProblemDetail pd1 = ProblemDetail.forRawStatusCode(500);
    ProblemDetail pd2 = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    ProblemDetail pd3 = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "some detail");
    ProblemDetail pd4 = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "other detail");

    assertThat(pd1).isNotEqualTo(pd2);
    assertThat(pd1).isNotEqualTo(pd3);
    assertThat(pd2).isNotEqualTo(pd3);
    assertThat(pd3).isNotEqualTo(pd4);

    assertThat(pd1.hashCode()).isNotEqualTo(pd2.hashCode());
    assertThat(pd1.hashCode()).isNotEqualTo(pd3.hashCode());
    assertThat(pd2.hashCode()).isNotEqualTo(pd3.hashCode());
    assertThat(pd3.hashCode()).isNotEqualTo(pd4.hashCode());
  }

  @Test
  void equals_withSelf_shouldReturnTrue() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    assertThat(problemDetail.equals(problemDetail)).isTrue();
  }

  @Test
  void equals_withNull_shouldReturnFalse() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    assertThat(problemDetail.equals(null)).isFalse();
  }

  @Test
  void equals_withDifferentClass_shouldReturnFalse() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    assertThat(problemDetail.equals("string")).isFalse();
  }

  @Test
  void equalsAndHashCode_withProperties_shouldConsiderProperties() {
    ProblemDetail pd1 = ProblemDetail.forRawStatusCode(500);
    pd1.setProperty("key", "value");

    ProblemDetail pd2 = ProblemDetail.forRawStatusCode(500);
    pd2.setProperty("key", "value");

    ProblemDetail pd3 = ProblemDetail.forRawStatusCode(500);
    pd3.setProperty("key", "different");

    assertThat(pd1).isEqualTo(pd2);
    assertThat(pd2).isEqualTo(pd1);
    assertThat(pd1.hashCode()).isEqualTo(pd2.hashCode());

    assertThat(pd1).isNotEqualTo(pd3);
    assertThat(pd1.hashCode()).isNotEqualTo(pd3.hashCode());
  }

  @Test
  void toString_shouldIncludeAllFields() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setDetail("Something went wrong");
    problemDetail.setInstance(URI.create("/error"));
    problemDetail.setProperty("trace", "stack trace");

    String toStringResult = problemDetail.toString();
    assertThat(toStringResult).contains("ProblemDetail");
    assertThat(toStringResult).contains("type='about:blank'");
    assertThat(toStringResult).contains("title='Internal Server Error'");
    assertThat(toStringResult).contains("status=500");
    assertThat(toStringResult).contains("detail='Something went wrong'");
    assertThat(toStringResult).contains("instance='/error'");
  }

  @Test
  void copyConstructor_shouldCopyAllFields() {
    ProblemDetail original = ProblemDetail.forRawStatusCode(404);
    original.setType(URI.create("https://example.com/not-found"));
    original.setTitle("Not Found");
    original.setDetail("Resource not found");
    original.setInstance(URI.create("/resource/123"));
    original.setProperty("timestamp", "2023-01-01T00:00:00Z");

    ProblemDetail copy = new ProblemDetail(original) { };

    assertThat(copy.getType()).isEqualTo(original.getType());
    assertThat(copy.getTitle()).isEqualTo(original.getTitle());
    assertThat(copy.getStatus()).isEqualTo(original.getStatus());
    assertThat(copy.getDetail()).isEqualTo(original.getDetail());
    assertThat(copy.getInstance()).isEqualTo(original.getInstance());
    assertThat(copy.getProperties()).containsExactlyEntriesOf(original.getProperties());
  }

  @Test
  void serialization_and_deserialization_shouldPreserveEquality() throws Exception {
    ProblemDetail originalDetail = ProblemDetail.forRawStatusCode(500);
    originalDetail.setTitle("Internal Server Error");
    originalDetail.setDetail("Something went wrong");
    originalDetail.setInstance(URI.create("/error"));
    originalDetail.setProperty("trace", "stack trace");

    ObjectMapper mapper = new ObjectMapper();
    byte[] bytes = mapper.writeValueAsBytes(originalDetail);
    ProblemDetail deserializedDetail = mapper.readValue(bytes, ProblemDetail.class);

    assertThat(originalDetail).isEqualTo(deserializedDetail);
    assertThat(deserializedDetail).isEqualTo(originalDetail);
    assertThat(originalDetail.hashCode()).isEqualTo(deserializedDetail.hashCode());
    assertThat(deserializedDetail.getTitle()).isEqualTo("Internal Server Error");
    assertThat(deserializedDetail.getDetail()).isEqualTo("Something went wrong");
    assertThat(deserializedDetail.getInstance()).isEqualTo(URI.create("/error"));
    assertThat(deserializedDetail.getProperties()).containsEntry("trace", "stack trace");
  }

  @ParameterizedTest
  @ValueSource(ints = { 100, 200, 201, 300, 400, 404, 500, 503 })
  void getTitle_withVariousStandardStatusCodes_shouldReturnCorrectReasonPhrase(int statusCode) {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(statusCode);
    HttpStatus status = HttpStatus.resolve(statusCode);
    if (status != null) {
      assertThat(problemDetail.getTitle()).isEqualTo(status.getReasonPhrase());
    }
    else {
      assertThat(problemDetail.getTitle()).isNull();
    }
  }

  @Test
  void getProperties_whenNoPropertiesSet_shouldReturnNull() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    assertThat(problemDetail.getProperties()).isNull();
  }

  @Test
  void setProperty_onNullProperties_shouldCreatePropertiesMap() {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(500);
    problemDetail.setProperty("key", "value");
    assertThat(problemDetail.getProperties()).isNotNull();
    assertThat(problemDetail.getProperties()).containsEntry("key", "value");
  }

}
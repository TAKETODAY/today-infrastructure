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

package cn.taketoday.framework.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.NullNode;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JsonObjectDeserializer}.
 *
 * @author Phillip Webb
 */
class JsonObjectDeserializerTests {

  private TestJsonObjectDeserializer<Object> testDeserializer = new TestJsonObjectDeserializer<>();

  @Test
  void deserializeObjectShouldReadJson() throws Exception {
    NameAndAgeJsonComponent.Deserializer deserializer = new NameAndAgeJsonComponent.Deserializer();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(NameAndAge.class, deserializer);
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(module);
    NameAndAge nameAndAge = mapper.readValue("{\"name\":\"spring\",\"age\":100}", NameAndAge.class);
    assertThat(nameAndAge.getName()).isEqualTo("spring");
    assertThat(nameAndAge.getAge()).isEqualTo(100);
  }

  @Test
  void nullSafeValueWhenValueIsNullShouldReturnNull() {
    String value = this.testDeserializer.testNullSafeValue(null, String.class);
    assertThat(value).isNull();
  }

  @Test
  void nullSafeValueWhenClassIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.testDeserializer.testNullSafeValue(Mockito.mock(JsonNode.class), null))
            .withMessageContaining("Type is required");
  }

  @Test
  void nullSafeValueWhenClassIsStringShouldReturnString() {
    JsonNode node = mock(JsonNode.class);
    given(node.textValue()).willReturn("abc");
    String value = this.testDeserializer.testNullSafeValue(node, String.class);
    assertThat(value).isEqualTo("abc");
  }

  @Test
  void nullSafeValueWhenClassIsBooleanShouldReturnBoolean() {
    JsonNode node = mock(JsonNode.class);
    given(node.booleanValue()).willReturn(true);
    Boolean value = this.testDeserializer.testNullSafeValue(node, Boolean.class);
    assertThat(value).isTrue();
  }

  @Test
  void nullSafeValueWhenClassIsLongShouldReturnLong() {
    JsonNode node = mock(JsonNode.class);
    given(node.longValue()).willReturn(10L);
    Long value = this.testDeserializer.testNullSafeValue(node, Long.class);
    assertThat(value).isEqualTo(10L);
  }

  @Test
  void nullSafeValueWhenClassIsIntegerShouldReturnInteger() {
    JsonNode node = mock(JsonNode.class);
    given(node.intValue()).willReturn(10);
    Integer value = this.testDeserializer.testNullSafeValue(node, Integer.class);
    assertThat(value).isEqualTo(10);
  }

  @Test
  void nullSafeValueWhenClassIsShortShouldReturnShort() {
    JsonNode node = mock(JsonNode.class);
    given(node.shortValue()).willReturn((short) 10);
    Short value = this.testDeserializer.testNullSafeValue(node, Short.class);
    assertThat(value).isEqualTo((short) 10);
  }

  @Test
  void nullSafeValueWhenClassIsDoubleShouldReturnDouble() {
    JsonNode node = mock(JsonNode.class);
    given(node.doubleValue()).willReturn(1.1D);
    Double value = this.testDeserializer.testNullSafeValue(node, Double.class);
    assertThat(value).isEqualTo(1.1D);
  }

  @Test
  void nullSafeValueWhenClassIsFloatShouldReturnFloat() {
    JsonNode node = mock(JsonNode.class);
    given(node.floatValue()).willReturn(1.1F);
    Float value = this.testDeserializer.testNullSafeValue(node, Float.class);
    assertThat(value).isEqualTo(1.1F);
  }

  @Test
  void nullSafeValueWhenClassIsBigDecimalShouldReturnBigDecimal() {
    JsonNode node = mock(JsonNode.class);
    given(node.decimalValue()).willReturn(BigDecimal.TEN);
    BigDecimal value = this.testDeserializer.testNullSafeValue(node, BigDecimal.class);
    assertThat(value).isEqualTo(BigDecimal.TEN);
  }

  @Test
  void nullSafeValueWhenClassIsBigIntegerShouldReturnBigInteger() {
    JsonNode node = mock(JsonNode.class);
    given(node.bigIntegerValue()).willReturn(BigInteger.TEN);
    BigInteger value = this.testDeserializer.testNullSafeValue(node, BigInteger.class);
    assertThat(value).isEqualTo(BigInteger.TEN);
  }

  @Test
  void nullSafeValueWhenClassIsUnknownShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.testDeserializer.testNullSafeValue(Mockito.mock(JsonNode.class), InputStream.class))
            .withMessageContaining("Unsupported value type java.io.InputStream");

  }

  @Test
  void getRequiredNodeWhenTreeIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.testDeserializer.testGetRequiredNode(null, "test"))
            .withMessageContaining("Tree is required");
  }

  @Test
  void getRequiredNodeWhenNodeIsNullShouldThrowException() {
    JsonNode tree = mock(JsonNode.class);
    given(tree.get("test")).willReturn(null);
    assertThatIllegalStateException().isThrownBy(() -> this.testDeserializer.testGetRequiredNode(tree, "test"))
            .withMessageContaining("Missing JSON field 'test'");
  }

  @Test
  void getRequiredNodeWhenNodeIsNullNodeShouldThrowException() {
    JsonNode tree = mock(JsonNode.class);
    given(tree.get("test")).willReturn(NullNode.instance);
    assertThatIllegalStateException().isThrownBy(() -> this.testDeserializer.testGetRequiredNode(tree, "test"))
            .withMessageContaining("Missing JSON field 'test'");
  }

  @Test
  void getRequiredNodeWhenNodeIsFoundShouldReturnNode() {
    JsonNode node = mock(JsonNode.class);
    given(node.get("test")).willReturn(node);
    assertThat(this.testDeserializer.testGetRequiredNode(node, "test")).isEqualTo(node);
  }

  static class TestJsonObjectDeserializer<T> extends JsonObjectDeserializer<T> {

    @Override
    protected T deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
            JsonNode tree) {
      return null;
    }

    <D> D testNullSafeValue(JsonNode jsonNode, Class<D> type) {
      return nullSafeValue(jsonNode, type);
    }

    JsonNode testGetRequiredNode(JsonNode tree, String fieldName) {
      return getRequiredNode(tree, fieldName);
    }

  }

}

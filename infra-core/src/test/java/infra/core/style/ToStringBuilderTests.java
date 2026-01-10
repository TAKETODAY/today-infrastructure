/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.core.style;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Harry Yang 2021/10/11 14:54
 */
class ToStringBuilderTests {

  private SomeObject s1, s2, s3;

  @BeforeEach
  void setUp() throws Exception {
    s1 = new SomeObject() {
      @Override
      public String toString() {
        return "A";
      }
    };
    s2 = new SomeObject() {
      @Override
      public String toString() {
        return "B";
      }
    };
    s3 = new SomeObject() {
      @Override
      public String toString() {
        return "C";
      }
    };
  }

  @Test
  void defaultStyleMap() {
    final Map<String, String> map = getMap();
    Object stringy = new Object() {
      @Override
      public String toString() {
        return new ToStringBuilder(this).append("familyFavoriteSport", map).toString();
      }
    };
    assertThat(stringy.toString()).isEqualTo(("[ToStringBuilderTests.4@" + ObjectUtils.getIdentityHexString(stringy) +
            " familyFavoriteSport = map['Keri' -> 'Softball', 'Scot' -> 'Fishing', 'Keith' -> 'Flag Football']]"));
  }

  private Map<String, String> getMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("Keri", "Softball");
    map.put("Scot", "Fishing");
    map.put("Keith", "Flag Football");
    return map;
  }

  @Test
  void defaultStyleArray() {
    SomeObject[] array = new SomeObject[] { s1, s2, s3 };
    String str = new ToStringBuilder(array).toString();
    assertThat(str).isEqualTo(("[@" + ObjectUtils.getIdentityHexString(array) +
            " array<ToStringBuilderTests.SomeObject>[A, B, C]]"));
  }

  @Test
  void primitiveArrays() {
    int[] integers = new int[] { 0, 1, 2, 3, 4 };
    String str = new ToStringBuilder(integers).toString();
    assertThat(str).isEqualTo(("[@" + ObjectUtils.getIdentityHexString(integers) + " array<Integer>[0, 1, 2, 3, 4]]"));
  }

  @Test
  void appendList() {
    List<SomeObject> list = new ArrayList<>();
    list.add(s1);
    list.add(s2);
    list.add(s3);
    String str = new ToStringBuilder(this).append("myLetters", list).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = list[A, B, C]]"));
  }

  @Test
  void appendSet() {
    Set<SomeObject> set = new LinkedHashSet<>();
    set.add(s1);
    set.add(s2);
    set.add(s3);
    String str = new ToStringBuilder(this).append("myLetters", set).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = set[A, B, C]]"));
  }

  @Test
  void appendClass() {
    String str = new ToStringBuilder(this).append("myClass", this.getClass()).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) +
            " myClass = ToStringBuilderTests]"));
  }

  @Test
  void appendMethod() throws Exception {
    String str = new ToStringBuilder(this)
            .append("myMethod", this.getClass().getDeclaredMethod("appendMethod")).toString();
    assertThat(str).isEqualTo(("[ToStringBuilderTests@" + ObjectUtils.getIdentityHexString(this) +
            " myMethod = appendMethod@ToStringBuilderTests]"));
  }

  @Test
  void constructorWithObjectCreatesBuilder() {
    Object obj = new Object();
    ToStringBuilder builder = new ToStringBuilder(obj);
    assertThat(builder).isNotNull();
    assertThat(builder.toString()).contains(obj.getClass().getSimpleName());
  }

  @Test
  void constructorWithObjectAndValueStylerCreatesBuilder() {
    Object obj = new Object();
    ValueStyler styler = new DefaultValueStyler();
    ToStringBuilder builder = new ToStringBuilder(obj, styler);
    assertThat(builder).isNotNull();
    assertThat(builder.toString()).contains(obj.getClass().getSimpleName());
  }

  @Test
  void constructorWithObjectAndToStringStylerCreatesBuilder() {
    Object obj = new Object();
    ToStringStyler styler = new DefaultToStringStyler(new DefaultValueStyler());
    ToStringBuilder builder = new ToStringBuilder(obj, styler);
    assertThat(builder).isNotNull();
    assertThat(builder.toString()).contains(obj.getClass().getSimpleName());
  }

  @Test
  void appendByteFieldValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("byteField", (byte) 42);
    String result = builder.toString();
    assertThat(result).contains("byteField = 42");
  }

  @Test
  void appendShortFieldValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("shortField", (short) 1000);
    String result = builder.toString();
    assertThat(result).contains("shortField = 1000");
  }

  @Test
  void appendIntFieldValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("intField", 12345);
    String result = builder.toString();
    assertThat(result).contains("intField = 12345");
  }

  @Test
  void appendLongFieldValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("longField", 123456789L);
    String result = builder.toString();
    assertThat(result).contains("longField = 123456789");
  }

  @Test
  void appendFloatFieldValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("floatField", 3.14f);
    String result = builder.toString();
    assertThat(result).contains("floatField = 3.14");
  }

  @Test
  void appendDoubleFieldValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("doubleField", 3.14159);
    String result = builder.toString();
    assertThat(result).contains("doubleField = 3.14159");
  }

  @Test
  void appendBooleanFieldValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("booleanField", true);
    String result = builder.toString();
    assertThat(result).contains("booleanField = true");
  }

  @Test
  void appendMultipleFields() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("field1", "value1").append("field2", "value2");
    String result = builder.toString();
    assertThat(result).contains("field1 = 'value1'");
    assertThat(result).contains("field2 = 'value2'");
  }

  @Test
  void appendObjectValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    Object obj = new Object() {
      @Override
      public String toString() {
        return "testObject";
      }
    };
    builder.append("objectField", obj);
    String result = builder.toString();
    assertThat(result).contains("objectField = testObject");
  }

  @Test
  void appendNullValue() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("nullField", null);
    String result = builder.toString();
    assertThat(result).contains("nullField = [null]");
  }

  @Test
  void appendValueWithoutFieldName() {
    ToStringBuilder builder = new ToStringBuilder(this);
    Object obj = new Object() {
      @Override
      public String toString() {
        return "standaloneValue";
      }
    };
    builder.append(obj);
    String result = builder.toString();
    assertThat(result).contains("standaloneValue");
  }

  @Test
  void forInstanceCreatesBuilder() {
    Object obj = new Object();
    ToStringBuilder builder = ToStringBuilder.forInstance(obj);
    assertThat(builder).isNotNull();
    assertThat(builder.toString()).contains(obj.getClass().getSimpleName());
  }

  @Test
  void constructorWithNullObjectThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ToStringBuilder(null))
            .withMessageContaining("The object to be styled is required");
  }

  public static class SomeObject {
  }

}

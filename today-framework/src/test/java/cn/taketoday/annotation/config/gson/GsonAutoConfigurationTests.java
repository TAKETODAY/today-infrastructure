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

package cn.taketoday.annotation.config.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 15:49
 */
class GsonAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class));

  @Test
  void gsonRegistration() {
    this.contextRunner.run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
    });
  }

  @Test
  void generateNonExecutableJsonTrue() {
    this.contextRunner.withPropertyValues("gson.generate-non-executable-json:true").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.toJson(new DataObject())).isNotEqualTo("{\"data\":1}");
      assertThat(gson.toJson(new DataObject())).endsWith("{\"data\":1}");
    });
  }

  @Test
  void generateNonExecutableJsonFalse() {
    this.contextRunner.withPropertyValues("gson.generate-non-executable-json:false").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
    });
  }

  @Test
  void excludeFieldsWithoutExposeAnnotationTrue() {
    this.contextRunner.withPropertyValues("gson.exclude-fields-without-expose-annotation:true")
            .run((context) -> {
              Gson gson = context.getBean(Gson.class);
              assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
            });
  }

  @Test
  void excludeFieldsWithoutExposeAnnotationFalse() {
    this.contextRunner.withPropertyValues("gson.exclude-fields-without-expose-annotation:false")
            .run((context) -> {
              Gson gson = context.getBean(Gson.class);
              assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
            });
  }

  @Test
  void serializeNullsTrue() {
    this.contextRunner.withPropertyValues("gson.serialize-nulls:true").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.serializeNulls()).isTrue();
    });
  }

  @Test
  void serializeNullsFalse() {
    this.contextRunner.withPropertyValues("gson.serialize-nulls:false").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.serializeNulls()).isFalse();
    });
  }

  @Test
  void enableComplexMapKeySerializationTrue() {
    this.contextRunner.withPropertyValues("gson.enable-complex-map-key-serialization:true")
            .run((context) -> {
              Gson gson = context.getBean(Gson.class);
              Map<DataObject, String> original = new LinkedHashMap<>();
              original.put(new DataObject(), "a");
              assertThat(gson.toJson(original)).isEqualTo("[[{\"data\":1},\"a\"]]");
            });
  }

  @Test
  void enableComplexMapKeySerializationFalse() {
    this.contextRunner.withPropertyValues("gson.enable-complex-map-key-serialization:false")
            .run((context) -> {
              Gson gson = context.getBean(Gson.class);
              Map<DataObject, String> original = new LinkedHashMap<>();
              original.put(new DataObject(), "a");
              assertThat(gson.toJson(original)).contains(DataObject.class.getName()).doesNotContain("\"data\":");
            });
  }

  @Test
  void notDisableInnerClassSerialization() {
    this.contextRunner.run((context) -> {
      Gson gson = context.getBean(Gson.class);
      WrapperObject wrapperObject = new WrapperObject();
      assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("{\"data\":\"nested\"}");
    });
  }

  @Test
  void disableInnerClassSerializationTrue() {
    this.contextRunner.withPropertyValues("gson.disable-inner-class-serialization:true").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      WrapperObject wrapperObject = new WrapperObject();
      assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("null");
    });
  }

  @Test
  void disableInnerClassSerializationFalse() {
    this.contextRunner.withPropertyValues("gson.disable-inner-class-serialization:false").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      WrapperObject wrapperObject = new WrapperObject();
      assertThat(gson.toJson(wrapperObject.new NestedObject())).isEqualTo("{\"data\":\"nested\"}");
    });
  }

  @Test
  void withLongSerializationPolicy() {
    this.contextRunner.withPropertyValues("gson.long-serialization-policy:" + LongSerializationPolicy.STRING)
            .run((context) -> {
              Gson gson = context.getBean(Gson.class);
              assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":\"1\"}");
            });
  }

  @Test
  void withFieldNamingPolicy() {
    FieldNamingPolicy fieldNamingPolicy = FieldNamingPolicy.UPPER_CAMEL_CASE;
    this.contextRunner.withPropertyValues("gson.field-naming-policy:" + fieldNamingPolicy).run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.fieldNamingStrategy()).isEqualTo(fieldNamingPolicy);
    });
  }

  @Test
  void additionalGsonBuilderCustomization() {
    this.contextRunner.withUserConfiguration(GsonBuilderCustomizerConfig.class).run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.toJson(new DataObject())).isEqualTo("{}");
    });
  }

  @Test
  void customGsonBuilder() {
    this.contextRunner.withUserConfiguration(GsonBuilderConfig.class).run((context) -> {
      Gson gson = context.getBean(Gson.class);
      JSONAssert.assertEquals("{\"data\":1,\"owner\":null}", gson.toJson(new DataObject()), true);
    });
  }

  @Test
  void withPrettyPrintingTrue() {
    this.contextRunner.withPropertyValues("gson.pretty-printing:true").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.toJson(new DataObject())).isEqualTo("{\n  \"data\": 1\n}");
    });
  }

  @Test
  void withPrettyPrintingFalse() {
    this.contextRunner.withPropertyValues("gson.pretty-printing:false").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
    });
  }

  @Test
  void withoutLenient() {
    this.contextRunner.run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson).hasFieldOrPropertyWithValue("lenient", false);
    });
  }

  @Test
  void withLenientTrue() {
    this.contextRunner.withPropertyValues("gson.lenient:true").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson).hasFieldOrPropertyWithValue("lenient", true);
    });
  }

  @Test
  void withLenientFalse() {
    this.contextRunner.withPropertyValues("gson.lenient:false").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson).hasFieldOrPropertyWithValue("lenient", false);
    });
  }

  @Test
  void withoutDisableHtmlEscaping() {
    this.contextRunner.run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.htmlSafe()).isTrue();
    });
  }

  @Test
  void withDisableHtmlEscapingTrue() {
    this.contextRunner.withPropertyValues("gson.disable-html-escaping:true").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.htmlSafe()).isFalse();
    });
  }

  @Test
  void withDisableHtmlEscapingFalse() {
    this.contextRunner.withPropertyValues("gson.disable-html-escaping:false").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      assertThat(gson.htmlSafe()).isTrue();
    });
  }

  @Test
  void customDateFormat() {
    this.contextRunner.withPropertyValues("gson.date-format:H").run((context) -> {
      Gson gson = context.getBean(Gson.class);
      ZonedDateTime dateTime = ZonedDateTime.of(1988, 6, 25, 20, 30, 0, 0, ZoneId.systemDefault());
      assertThat(gson.toJson(Date.from(dateTime.toInstant()))).isEqualTo("\"20\"");
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class GsonBuilderCustomizerConfig {

    @Bean
    GsonBuilderCustomizer customSerializationExclusionStrategy() {
      return (gsonBuilder) -> gsonBuilder.addSerializationExclusionStrategy(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
          return "data".equals(fieldAttributes.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
          return false;
        }
      });
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class GsonBuilderConfig {

    @Bean
    GsonBuilder customGsonBuilder() {
      return new GsonBuilder().serializeNulls();
    }

  }

  public class DataObject {

    @SuppressWarnings("unused")
    private Long data = 1L;

    @SuppressWarnings("unused")
    private final String owner = null;

    public void setData(Long data) {
      this.data = data;
    }

  }

  public class WrapperObject {

    @SuppressWarnings("unused")
    class NestedObject {

      private final String data = "nested";

    }

  }

}
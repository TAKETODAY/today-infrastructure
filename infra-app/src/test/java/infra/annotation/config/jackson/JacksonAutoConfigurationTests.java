/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.annotation.config.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import infra.annotation.config.jackson.JacksonAutoConfiguration.JacksonAutoConfigurationRuntimeHints;
import infra.annotation.config.jackson.JacksonAutoConfiguration.JacksonJsonMapperBuilderCustomizerConfiguration.StandardJsonMapperBuilderCustomizer;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.app.jackson.JacksonComponent;
import infra.app.jackson.JacksonMixin;
import infra.app.jackson.JacksonMixinModule;
import infra.app.jackson.JacksonMixinModuleEntries;
import infra.app.jackson.ObjectValueSerializer;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Primary;
import infra.context.annotation.config.AutoConfigurationPackage;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.annotation.Order;
import infra.http.ProblemDetail;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.util.StdDateFormat;
import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/29 21:28
 */
class JacksonAutoConfigurationTests {

  protected final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

  @EnumSource
  @ParameterizedTest
  void definesMapper(MapperType mapperType) {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(mapperType.mapperClass));
  }

  @EnumSource
  @ParameterizedTest
  void definesMapperBuilder(MapperType mapperType) {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(mapperType.builderClass));
  }

  @EnumSource
  @ParameterizedTest
  void mapperBacksOffWhenCustomMapperIsDefined(MapperType mapperType) {
    this.contextRunner.withBean("customMapper", mapperType.mapperClass).run((context) -> {
      assertThat(context).hasSingleBean(mapperType.mapperClass);
      assertThat(context).hasBean("customMapper");
    });
  }

  @EnumSource
  @ParameterizedTest
  void mapperDoesNotBackOffWhenObjectMapperIsDefined(MapperType mapperType) {
    this.contextRunner.withBean(tools.jackson.databind.ObjectMapper.class).run((context) -> {
      assertThat(context).hasSingleBean(mapperType.mapperClass);
      assertThat(context.getBeansOfType(tools.jackson.databind.ObjectMapper.class)).hasSize(MapperType.values().length + 1);
    });
  }

  @EnumSource
  @ParameterizedTest
  void mapperBuilderDoesNotBackOffWhenMapperIsDefined(MapperType mapperType) {
    this.contextRunner.withBean(mapperType.mapperClass)
            .run((context) -> assertThat(context).hasSingleBean(mapperType.builderClass));
  }

  @Test
  void standardJsonMapperBuilderCustomizerDoesNotBackOffWhenCustomizerIsDefined() {
    this.contextRunner.withBean(JsonMapperBuilderCustomizer.class, () -> mock(JsonMapperBuilderCustomizer.class))
            .run((context) -> assertThat(context).hasSingleBean(StandardJsonMapperBuilderCustomizer.class));
  }

  @Test
  void standardCborMapperBuilderCustomizerDoesNotBackOffWhenCustomizerIsDefined() {
    this.contextRunner.withBean(CborMapperBuilderCustomizer.class, () -> mock(CborMapperBuilderCustomizer.class))
            .run((context) -> assertThat(context).hasSingleBean(JacksonAutoConfiguration.CborConfiguration.StandardCborMapperBuilderCustomizer.class));
  }

  @Test
  void standardXmlMapperBuilderCustomizerDoesNotBackOffWhenCustomizerIsDefined() {
    this.contextRunner.withBean(XmlMapperBuilderCustomizer.class, () -> mock(XmlMapperBuilderCustomizer.class))
            .run((context) -> assertThat(context).hasSingleBean(JacksonAutoConfiguration.XmlConfiguration.StandardXmlMapperBuilderCustomizer.class));
  }

  @Test
  void doubleModuleRegistration() {
    this.contextRunner.withUserConfiguration(DoubleModulesConfig.class).run((context) -> {
      JsonMapper mapper = context.getBean(JsonMapper.class);
      assertThat(mapper.writeValueAsString(new Foo())).isEqualTo("{\"foo\":\"bar\"}");
    });
  }

  @Test
  void jsonMixinModuleShouldBeAutoConfiguredWithBasePackages() {
    this.contextRunner.withUserConfiguration(MixinConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(JacksonMixinModule.class).hasSingleBean(JacksonMixinModuleEntries.class);
      JacksonMixinModuleEntries moduleEntries = context.getBean(JacksonMixinModuleEntries.class);
      assertThat(moduleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
              .contains(entry(Person.class, EmptyMixin.class));
    });
  }

  @EnumSource
  @ParameterizedTest
  void noCustomDateFormat(MapperType mapperType) {
    this.contextRunner.run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(tools.jackson.databind.util.StdDateFormat.class);
      assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(StdDateFormat.class);
    });
  }

  @EnumSource
  @ParameterizedTest
  void customDateFormat(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.date-format:yyyyMMddHHmmss").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      DateFormat serializationDateFormat = mapper.serializationConfig().getDateFormat();
      assertThat(serializationDateFormat).isInstanceOf(SimpleDateFormat.class);
      assertThat(((SimpleDateFormat) serializationDateFormat).toPattern()).isEqualTo("yyyyMMddHHmmss");
      DateFormat deserializationDateFormat = mapper.serializationConfig().getDateFormat();
      assertThat(deserializationDateFormat).isInstanceOf(SimpleDateFormat.class);
      assertThat(((SimpleDateFormat) deserializationDateFormat).toPattern()).isEqualTo("yyyyMMddHHmmss");
    });
  }

  @EnumSource
  @ParameterizedTest
  void customDateFormatClass(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.date-format:" + MyDateFormat.class.getName())
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
              assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
            });
  }

  @EnumSource
  @ParameterizedTest
  void noCustomPropertyNamingStrategy(MapperType mapperType) {
    this.contextRunner.run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(mapper.serializationConfig().getPropertyNamingStrategy()).isNull();
    });
  }

  @EnumSource
  @ParameterizedTest
  void customPropertyNamingStrategyField(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.property-naming-strategy:SNAKE_CASE").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(mapper.serializationConfig().getPropertyNamingStrategy()).isInstanceOf(tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class);
    });
  }

  @EnumSource
  @ParameterizedTest
  void customPropertyNamingStrategyClass(MapperType mapperType) {
    this.contextRunner.withPropertyValues(
                    "jackson.property-naming-strategy:tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(mapper.serializationConfig().getPropertyNamingStrategy())
                      .isInstanceOf(tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class);
            });
  }

  @EnumSource
  @ParameterizedTest
  void enableSerializationFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.serialization.indent_output:true").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT.enabledByDefault()).isFalse();
      assertThat(
              mapper.serializationConfig().hasSerializationFeatures(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT.getMask()))
              .isTrue();
    });
  }

  @EnumSource
  @ParameterizedTest
  void disableSerializationFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.serialization.wrap_exceptions:false").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(tools.jackson.databind.SerializationFeature.WRAP_EXCEPTIONS.enabledByDefault()).isTrue();
      assertThat(mapper.isEnabled(tools.jackson.databind.SerializationFeature.WRAP_EXCEPTIONS)).isFalse();
    });
  }

  @EnumSource
  @ParameterizedTest
  void enableDeserializationFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.deserialization.use_big_decimal_for_floats:true")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.enabledByDefault()).isFalse();
              assertThat(mapper.deserializationConfig()
                      .hasDeserializationFeatures(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.getMask())).isTrue();
            });
  }

  @EnumSource
  @ParameterizedTest
  void disableDeserializationFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.deserialization.fail-on-null-for-primitives:false")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(tools.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES.enabledByDefault()).isTrue();
              assertThat(mapper.deserializationConfig()
                      .hasDeserializationFeatures(tools.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES.getMask()))
                      .isFalse();
            });
  }

  @EnumSource
  @ParameterizedTest
  void enableMapperFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.mapper.require_setters_for_getters:true")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(tools.jackson.databind.MapperFeature.REQUIRE_SETTERS_FOR_GETTERS.enabledByDefault()).isFalse();

              assertThat(mapper.serializationConfig().isEnabled(tools.jackson.databind.MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)).isTrue();
              assertThat(mapper.deserializationConfig().isEnabled(tools.jackson.databind.MapperFeature.REQUIRE_SETTERS_FOR_GETTERS))
                      .isTrue();
            });
  }

  @EnumSource
  @ParameterizedTest
  void disableMapperFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.mapper.infer-property-mutators:false").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(tools.jackson.databind.MapperFeature.INFER_PROPERTY_MUTATORS.enabledByDefault()).isTrue();
      assertThat(mapper.deserializationConfig().isEnabled(tools.jackson.databind.MapperFeature.INFER_PROPERTY_MUTATORS)).isFalse();
      assertThat(mapper.serializationConfig().isEnabled(tools.jackson.databind.MapperFeature.INFER_PROPERTY_MUTATORS)).isFalse();
    });
  }

  @Test
  void enableJsonReadFeature() {
    this.contextRunner.withPropertyValues("jackson.json.read.allow_single_quotes:true").run((context) -> {
      JsonMapper mapper = context.getBean(JsonMapper.class);
      assertThat(JsonReadFeature.ALLOW_SINGLE_QUOTES.enabledByDefault()).isFalse();
      assertThat(mapper.isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES)).isTrue();
    });
  }

  @Test
  void enableJsonWriteFeature() {
    this.contextRunner.withPropertyValues("jackson.json.write.write_numbers_as_strings:true")
            .run((context) -> {
              JsonMapper mapper = context.getBean(JsonMapper.class);
              JsonWriteFeature feature = JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS;
              assertThat(feature.enabledByDefault()).isFalse();
              assertThat(mapper.isEnabled(feature)).isTrue();
            });
  }

  @Test
  void disableJsonWriteFeature() {
    this.contextRunner.withPropertyValues("jackson.json.write.write_hex_upper_case:false").run((context) -> {
      JsonMapper mapper = context.getBean(JsonMapper.class);
      assertThat(JsonWriteFeature.WRITE_HEX_UPPER_CASE.enabledByDefault()).isTrue();
      assertThat(mapper.isEnabled(JsonWriteFeature.WRITE_HEX_UPPER_CASE)).isFalse();
    });
  }

  @EnumSource
  @ParameterizedTest
  void enableDatetimeFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.datatype.datetime.write-dates-as-timestamps:true")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              DateTimeFeature feature = DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;
              assertThat(feature.enabledByDefault()).isFalse();
              assertThat(mapper.isEnabled(feature)).isTrue();
            });
  }

  @EnumSource
  @ParameterizedTest
  void disableDatetimeFeature(MapperType mapperType) {
    this.contextRunner
            .withPropertyValues("jackson.datatype.datetime.adjust-dates-to-context-time-zone:false")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE.enabledByDefault()).isTrue();
              assertThat(mapper.isEnabled(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)).isFalse();
            });
  }

  @EnumSource
  @ParameterizedTest
  void enableEnumFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.datatype.enum.write-enums-to-lowercase=true")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(tools.jackson.databind.cfg.EnumFeature.WRITE_ENUMS_TO_LOWERCASE.enabledByDefault()).isFalse();
              assertThat(mapper.serializationConfig().isEnabled(tools.jackson.databind.cfg.EnumFeature.WRITE_ENUMS_TO_LOWERCASE)).isTrue();
            });
  }

  @EnumSource
  @ParameterizedTest
  void disableJsonNodeFeature(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.datatype.json-node.write-null-properties:false")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              assertThat(tools.jackson.databind.cfg.JsonNodeFeature.WRITE_NULL_PROPERTIES.enabledByDefault()).isTrue();
              assertThat(mapper.deserializationConfig().isEnabled(tools.jackson.databind.cfg.JsonNodeFeature.WRITE_NULL_PROPERTIES)).isFalse();
            });
  }

  @Test
  void defaultJsonFactoryIsRegisteredWithTheMapperBuilderWhenNoCustomFactoryExists() {
    this.contextRunner.run((context) -> {
      JsonMapper.Builder jsonMapperBuilder = context.getBean(JsonMapper.Builder.class);
      assertThat(jsonMapperBuilder.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)).isTrue();
    });
  }

  @Test
  void customJsonFactoryIsRegisteredWithTheMapperBuilder() {
    JsonFactory customJsonFactory = new JsonFactoryBuilder().configure(StreamReadFeature.AUTO_CLOSE_SOURCE, false)
            .build();
    this.contextRunner.withBean("customJsonFactory", JsonFactory.class, () -> customJsonFactory).run((context) -> {
      JsonMapper.Builder jsonMapperBuilder = context.getBean(JsonMapper.Builder.class);
      assertThat(jsonMapperBuilder.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE)).isFalse();
    });
  }

  @EnumSource
  @ParameterizedTest
  void moduleBeansAndWellKnownModulesAreRegisteredWithTheMapperBuilder(MapperType mapperType) {
    this.contextRunner.withUserConfiguration(ModuleConfig.class).run((context) -> {
      JsonMapper.Builder jsonMapperBuilder = context.getBean(JsonMapper.Builder.class);
      JsonMapper jsonMapper = jsonMapperBuilder.build();
      assertThat(context.getBean(CustomModule.class).getOwners()).contains(jsonMapperBuilder);
      assertThat(jsonMapper._serializationContext().findValueSerializer(Baz.class)).isNotNull();
    });
  }

  @Test
  void customModulesRegisteredByJsonMapperBuilderCustomizerShouldBeRetained() {
    this.contextRunner
            .withUserConfiguration(ModuleConfig.class, CustomModuleJsonMapperBuilderCustomizerConfig.class)
            .run((context) -> {
              JsonMapper jsonMapper = context.getBean(JsonMapper.class);
              assertThat(jsonMapper.registeredModules()).extracting(JacksonModule::getModuleName)
                      .contains("module-A", "module-B", CustomModule.class.getName());
            });
  }

  @Test
  void customModulesRegisteredByCborMapperBuilderCustomizerShouldBeRetained() {
    this.contextRunner
            .withUserConfiguration(ModuleConfig.class, CustomModuleCborMapperBuilderCustomizerConfig.class)
            .run((context) -> {
              CBORMapper mapper = context.getBean(CBORMapper.class);
              assertThat(mapper.registeredModules()).extracting(JacksonModule::getModuleName)
                      .contains("module-A", "module-B", CustomModule.class.getName());
            });
  }

  @Test
  void customModulesRegisteredByXmlMapperBuilderCustomizerShouldBeRetained() {
    this.contextRunner.withUserConfiguration(ModuleConfig.class, CustomModuleXmlMapperBuilderCustomizerConfig.class)
            .run((context) -> {
              XmlMapper mapper = context.getBean(XmlMapper.class);
              assertThat(mapper.registeredModules()).extracting(JacksonModule::getModuleName)
                      .contains("module-A", "module-B", CustomModule.class.getName());
            });
  }

  @EnumSource
  @ParameterizedTest
  void defaultPropertyInclusion(MapperType mapperType) {
    this.contextRunner.run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(mapper.serializationConfig().getDefaultPropertyInclusion().getContentInclusion())
              .isEqualTo(JsonInclude.Include.USE_DEFAULTS);
      assertThat(mapper.serializationConfig().getDefaultPropertyInclusion().getValueInclusion())
              .isEqualTo(JsonInclude.Include.USE_DEFAULTS);
    });
  }

  @EnumSource
  @ParameterizedTest
  void customPropertyInclusion(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.default-property-inclusion:non_null").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      assertThat(mapper.serializationConfig().getDefaultPropertyInclusion().getContentInclusion())
              .isEqualTo(JsonInclude.Include.NON_NULL);
      assertThat(mapper.serializationConfig().getDefaultPropertyInclusion().getValueInclusion())
              .isEqualTo(JsonInclude.Include.NON_NULL);
    });
  }

  @Test
  void customTimeZoneFormattingADateToJson() {
    this.contextRunner.withPropertyValues("jackson.time-zone:GMT+10", "jackson.date-format:z")
            .run((context) -> {
              JsonMapper mapper = context.getBean(JsonMapper.class);
              Date date = new Date(1436966242231L);
              assertThat(mapper.writeValueAsString(date)).isEqualTo("\"GMT+10:00\"");
            });
  }

  @Test
  void customTimeZoneFormattingADateToCbor() {
    this.contextRunner.withPropertyValues("jackson.time-zone:GMT+10", "jackson.date-format:z")
            .run((context) -> {
              CBORMapper mapper = context.getBean(CBORMapper.class);
              Date date = new Date(1436966242231L);
              assertThat(mapper.writeValueAsBytes(date))
                      .isEqualTo(new byte[] { 105, 71, 77, 84, 43, 49, 48, 58, 48, 48 });
            });
  }

  @Test
  void customTimeZoneFormattingADateToXml() {
    this.contextRunner.withPropertyValues("jackson.time-zone:GMT+10", "jackson.date-format:z")
            .run((context) -> {
              XmlMapper mapper = context.getBean(XmlMapper.class);
              Date date = new Date(1436966242231L);
              assertThat(mapper.writeValueAsString(date)).isEqualTo("<Date>GMT+10:00</Date>");
            });
  }

  @EnumSource
  @ParameterizedTest
  void enableDefaultLeniency(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.default-leniency:true").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = context.getBean(mapperType.mapperClass);
      byte[] source = mapper.writeValueAsBytes(Map.of("birthDate", "2010-12-30"));
      Person person = mapper.readValue(source, Person.class);
      assertThat(person.getBirthDate()).isNotNull();
    });
  }

  @EnumSource
  @ParameterizedTest
  void disableDefaultLeniency(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.default-leniency:false").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = context.getBean(mapperType.mapperClass);
      byte[] source = mapper.writeValueAsBytes(Map.of("birthDate", "2010-12-30"));
      assertThatExceptionOfType(InvalidFormatException.class)
              .isThrownBy(() -> mapper.readValue(source, Person.class))
              .withMessageContaining("expected format")
              .withMessageContaining("yyyyMMdd");
    });
  }

  @EnumSource
  @ParameterizedTest
  void constructorDetectorWithNoStrategyUseDefault(MapperType mapperType) {
    this.contextRunner.run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      tools.jackson.databind.cfg.ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
      assertThat(cd.singleArgMode()).isEqualTo(tools.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor.HEURISTIC);
      assertThat(cd.requireCtorAnnotation()).isFalse();
      assertThat(cd.allowJDKTypeConstructors()).isFalse();
    });
  }

  @EnumSource
  @ParameterizedTest
  void constructorDetectorWithDefaultStrategy(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.constructor-detector=default").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      tools.jackson.databind.cfg.ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
      assertThat(cd.singleArgMode()).isEqualTo(tools.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor.HEURISTIC);
      assertThat(cd.requireCtorAnnotation()).isFalse();
      assertThat(cd.allowJDKTypeConstructors()).isFalse();
    });
  }

  @EnumSource
  @ParameterizedTest
  void constructorDetectorWithUsePropertiesBasedStrategy(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.constructor-detector=use-properties-based")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
              tools.jackson.databind.cfg.ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
              assertThat(cd.singleArgMode()).isEqualTo(tools.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor.PROPERTIES);
              assertThat(cd.requireCtorAnnotation()).isFalse();
              assertThat(cd.allowJDKTypeConstructors()).isFalse();
            });
  }

  @EnumSource
  @ParameterizedTest
  void constructorDetectorWithUseDelegatingStrategy(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.constructor-detector=use-delegating").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      tools.jackson.databind.cfg.ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
      assertThat(cd.singleArgMode()).isEqualTo(tools.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor.DELEGATING);
      assertThat(cd.requireCtorAnnotation()).isFalse();
      assertThat(cd.allowJDKTypeConstructors()).isFalse();
    });
  }

  @EnumSource
  @ParameterizedTest
  void constructorDetectorWithExplicitOnlyStrategy(MapperType mapperType) {
    this.contextRunner.withPropertyValues("jackson.constructor-detector=explicit-only").run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = mapperType.getMapper(context);
      tools.jackson.databind.cfg.ConstructorDetector cd = mapper.deserializationConfig().getConstructorDetector();
      assertThat(cd.singleArgMode()).isEqualTo(tools.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor.REQUIRE_MODE);
      assertThat(cd.requireCtorAnnotation()).isFalse();
      assertThat(cd.allowJDKTypeConstructors()).isFalse();
    });
  }

  @Test
  void additionalJsonMapperBuilderCustomization() {
    this.contextRunner.withBean(JsonMapperBuilderCustomizer.class, () -> null)
            .withUserConfiguration(JsonMapperBuilderCustomConfig.class)
            .run((context) -> {
              JsonMapper mapper = context.getBean(JsonMapper.class);
              assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
              assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
            });
  }

  @Test
  void additionalCborMapperBuilderCustomization() {
    this.contextRunner.withBean(CborMapperBuilderCustomizer.class, () -> null)
            .withUserConfiguration(CborMapperBuilderCustomConfig.class)
            .run((context) -> {
              CBORMapper mapper = context.getBean(CBORMapper.class);
              assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
              assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
            });
  }

  @Test
  void additionalXmlMapperBuilderCustomization() {
    this.contextRunner.withBean(XmlMapperBuilderCustomizer.class, () -> null)
            .withUserConfiguration(XmlMapperBuilderCustomConfig.class)
            .run((context) -> {
              XmlMapper mapper = context.getBean(XmlMapper.class);
              assertThat(mapper.deserializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
              assertThat(mapper.serializationConfig().getDateFormat()).isInstanceOf(MyDateFormat.class);
            });
  }

  @EnumSource
  @ParameterizedTest
  void writeDurationAsTimestampsDefault(MapperType mapperType) {
    this.contextRunner.run((context) -> {
      tools.jackson.databind.ObjectMapper mapper = context.getBean(mapperType.mapperClass);
      Duration duration = Duration.ofHours(2);
      String written = mapper.readerFor(String.class).readValue(mapper.writeValueAsBytes(duration));
      assertThat(written).isEqualTo("PT2H");
    });
  }

  @EnumSource
  @ParameterizedTest
  @SuppressWarnings("unchecked")
  void writeWithVisibility(MapperType mapperType) {
    this.contextRunner
            .withPropertyValues("jackson.visibility.getter:none", "jackson.visibility.field:any")
            .run((context) -> {
              tools.jackson.databind.ObjectMapper mapper = context.getBean(mapperType.mapperClass);
              Map<String, ?> written = mapper.readValue(mapper.writeValueAsBytes(new VisibilityBean()), Map.class);
              assertThat(written).containsOnlyKeys("property1", "property2");
            });
  }

  @Test
  void jsonMapperBuilderIsNotSharedAcrossMultipleInjectionPoints() {
    this.contextRunner.withUserConfiguration(JsonMapperBuilderConsumerConfig.class).run((context) -> {
      JsonMapperBuilderConsumerConfig consumer = context.getBean(JsonMapperBuilderConsumerConfig.class);
      assertThat(consumer.builderOne).isNotNull();
      assertThat(consumer.builderTwo).isNotNull();
      assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
    });
  }

  @Test
  void cborMapperBuilderIsNotSharedAcrossMultipleInjectionPoints() {
    this.contextRunner.withUserConfiguration(CborMapperBuilderConsumerConfig.class).run((context) -> {
      CborMapperBuilderConsumerConfig consumer = context.getBean(CborMapperBuilderConsumerConfig.class);
      assertThat(consumer.builderOne).isNotNull();
      assertThat(consumer.builderTwo).isNotNull();
      assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
    });
  }

  @Test
  void xmlMapperBuilderIsNotSharedAcrossMultipleInjectionPoints() {
    this.contextRunner.withUserConfiguration(XmlMapperBuilderConsumerConfig.class).run((context) -> {
      XmlMapperBuilderConsumerConfig consumer = context.getBean(XmlMapperBuilderConsumerConfig.class);
      assertThat(consumer.builderOne).isNotNull();
      assertThat(consumer.builderTwo).isNotNull();
      assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
    });
  }

  @Test
  void jacksonComponentThatInjectsJsonMapperCausesBeanCurrentlyInCreationException() {
    this.contextRunner.withUserConfiguration(CircularDependencySerializerConfiguration.class).run((context) -> {
      assertThat(context).hasFailed();
      assertThat(context).getFailure().hasRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
    });
  }

  @Test
  void shouldRegisterPropertyNamingStrategyHints() {
    RuntimeHints hints = new RuntimeHints();
    new JacksonAutoConfigurationRuntimeHints().registerHints(hints, getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.reflection().onType(tools.jackson.databind.PropertyNamingStrategies.class)).accepts(hints);
  }

  @Test
  void shouldRegisterProblemDetailsMixinWithJsonMapper() {
    this.contextRunner.run((context) -> {
      JsonMapper mapper = context.getBean(JsonMapper.class);
      ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
      problemDetail.setProperty("spring", "boot");
      String json = mapper.writeValueAsString(problemDetail);
      assertThat(json).isEqualTo("{\"status\":404,\"title\":\"Not Found\",\"spring\":\"boot\"}");
    });
  }

  @Test
  void shouldRegisterProblemDetailsMixinWithXmlMapper() {
    this.contextRunner.run((context) -> {
      XmlMapper mapper = context.getBean(XmlMapper.class);
      ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
      problemDetail.setProperty("spring", "boot");
      String xml = mapper.writeValueAsString(problemDetail);
      assertThat(xml).isEqualTo(
              "<problem xmlns=\"urn:ietf:rfc:7807\"><status>404</status><title>Not Found</title><spring>boot</spring></problem>");
    });
  }

  static class MyDateFormat extends SimpleDateFormat {

    MyDateFormat() {
      super("yyyy-MM-dd HH:mm:ss");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MockObjectMapperConfig {

    @Bean
    @Primary
    tools.jackson.databind.ObjectMapper objectMapper() {
      return mock(tools.jackson.databind.ObjectMapper.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(BazSerializer.class)
  static class ModuleConfig {

    @Bean
    CustomModule jacksonModule() {
      return new CustomModule();
    }

  }

  @Configuration
  static class DoubleModulesConfig {

    @Bean
    JacksonModule jacksonModule() {
      tools.jackson.databind.module.SimpleModule module = new tools.jackson.databind.module.SimpleModule();
      module.addSerializer(Foo.class, new ValueSerializer<>() {

        @Override
        public void serialize(Foo value, tools.jackson.core.JsonGenerator jgen, SerializationContext context) {
          jgen.writeStartObject();
          jgen.writeStringProperty("foo", "bar");
          jgen.writeEndObject();
        }
      });
      return module;
    }

    @Bean
    @Primary
    JsonMapper jsonMapper() {
      return JsonMapper.builder().addModule(jacksonModule()).build();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JsonMapperBuilderCustomConfig {

    @Bean
    JsonMapperBuilderCustomizer customDateFormat() {
      return (builder) -> builder.defaultDateFormat(new MyDateFormat());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CborMapperBuilderCustomConfig {

    @Bean
    CborMapperBuilderCustomizer customDateFormat() {
      return (builder) -> builder.defaultDateFormat(new MyDateFormat());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class XmlMapperBuilderCustomConfig {

    @Bean
    XmlMapperBuilderCustomizer customDateFormat() {
      return (builder) -> builder.defaultDateFormat(new MyDateFormat());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomModuleJsonMapperBuilderCustomizerConfig {

    @Bean
    @Order(-1)
    JsonMapperBuilderCustomizer highPrecedenceCustomizer() {
      return (builder) -> builder.addModule(new tools.jackson.databind.module.SimpleModule("module-A"));
    }

    @Bean
    @Order(1)
    JsonMapperBuilderCustomizer lowPrecedenceCustomizer() {
      return (builder) -> builder.addModule(new tools.jackson.databind.module.SimpleModule("module-B"));
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomModuleCborMapperBuilderCustomizerConfig {

    @Bean
    @Order(-1)
    CborMapperBuilderCustomizer highPrecedenceCustomizer() {
      return (builder) -> builder.addModule(new tools.jackson.databind.module.SimpleModule("module-A"));
    }

    @Bean
    @Order(1)
    CborMapperBuilderCustomizer lowPrecedenceCustomizer() {
      return (builder) -> builder.addModule(new tools.jackson.databind.module.SimpleModule("module-B"));
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomModuleXmlMapperBuilderCustomizerConfig {

    @Bean
    @Order(-1)
    XmlMapperBuilderCustomizer highPrecedenceCustomizer() {
      return (builder) -> builder.addModule(new tools.jackson.databind.module.SimpleModule("module-A"));
    }

    @Bean
    @Order(1)
    XmlMapperBuilderCustomizer lowPrecedenceCustomizer() {
      return (builder) -> builder.addModule(new tools.jackson.databind.module.SimpleModule("module-B"));
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JsonMapperBuilderConsumerConfig {

    JsonMapper.@Nullable Builder builderOne;

    JsonMapper.@Nullable Builder builderTwo;

    @Bean
    String consumerOne(JsonMapper.Builder builder) {
      this.builderOne = builder;
      return "one";
    }

    @Bean
    String consumerTwo(JsonMapper.Builder builder) {
      this.builderTwo = builder;
      return "two";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CborMapperBuilderConsumerConfig {

    CBORMapper.@Nullable Builder builderOne;

    CBORMapper.@Nullable Builder builderTwo;

    @Bean
    String consumerOne(CBORMapper.Builder builder) {
      this.builderOne = builder;
      return "one";
    }

    @Bean
    String consumerTwo(CBORMapper.Builder builder) {
      this.builderTwo = builder;
      return "two";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class XmlMapperBuilderConsumerConfig {

    XmlMapper.@Nullable Builder builderOne;

    XmlMapper.@Nullable Builder builderTwo;

    @Bean
    String consumerOne(XmlMapper.Builder builder) {
      this.builderOne = builder;
      return "one";
    }

    @Bean
    String consumerTwo(XmlMapper.Builder builder) {
      this.builderTwo = builder;
      return "two";
    }

  }

  protected static final class Foo {

    private @Nullable String name;

    private Foo() {
    }

    static Foo create() {
      return new Foo();
    }

    public @Nullable String getName() {
      return this.name;
    }

    public void setName(@Nullable String name) {
      this.name = name;
    }

  }

  static class Bar {

    private @Nullable String propertyName;

    @Nullable String getPropertyName() {
      return this.propertyName;
    }

    void setPropertyName(@Nullable String propertyName) {
      this.propertyName = propertyName;
    }

  }

  @JacksonComponent
  static class BazSerializer extends ObjectValueSerializer<Baz> {

    @Override
    protected void serializeObject(Baz value, tools.jackson.core.JsonGenerator jgen, SerializationContext context) {
    }

  }

  static class Baz {

  }

  static class CustomModule extends SimpleModule {

    private final Set<Object> owners = new HashSet<>();

    @Override
    public void setupModule(SetupContext context) {
      this.owners.add(context.getOwner());
    }

    Set<Object> getOwners() {
      return this.owners;
    }

  }

  @SuppressWarnings("unused")
  static class VisibilityBean {

    private @Nullable String property1;

    public @Nullable String property2;

    @Nullable String getProperty3() {
      return null;
    }

  }

  static class Person {

    @JsonFormat(pattern = "yyyyMMdd")
    private @Nullable Date birthDate;

    @Nullable Date getBirthDate() {
      return this.birthDate;
    }

    void setBirthDate(@Nullable Date birthDate) {
      this.birthDate = birthDate;
    }

  }

  @JacksonMixin(type = Person.class)
  static class EmptyMixin {

  }

  @AutoConfigurationPackage
  static class MixinConfiguration {

  }

  @JacksonComponent
  static class CircularDependencySerializer extends ValueSerializer<String> {

    CircularDependencySerializer(JsonMapper jsonMapper) {

    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext context) {

    }

  }

  @Import(CircularDependencySerializer.class)
  @Configuration(proxyBeanMethods = false)
  static class CircularDependencySerializerConfiguration {

  }

  enum MapperType {

    CBOR(CBORMapper.class, CBORMapper.Builder.class),
    JSON(JsonMapper.class, JsonMapper.Builder.class),
    XML(XmlMapper.class, XmlMapper.Builder.class);

    private final Class<? extends tools.jackson.databind.ObjectMapper> mapperClass;

    private final Class<? extends MapperBuilder<?, ?>> builderClass;

    <M extends tools.jackson.databind.ObjectMapper, B extends MapperBuilder<M, B>> MapperType(Class<M> mapperClass,
            Class<B> builderClass) {
      this.mapperClass = mapperClass;
      this.builderClass = builderClass;
    }

    ObjectMapper getMapper(ApplicationContext context) {
      return context.getBean(this.mapperClass);
    }

  }

}

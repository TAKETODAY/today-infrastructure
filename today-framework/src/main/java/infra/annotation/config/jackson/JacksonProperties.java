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

package infra.annotation.config.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;

import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import infra.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Jackson.
 *
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Johannes Edmeier
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "jackson")
public class JacksonProperties {

  /**
   * Date format string or a fully-qualified date format class name. For instance,
   * 'yyyy-MM-dd HH:mm:ss'.
   */
  @Nullable
  private String dateFormat;

  /**
   * One of the constants on Jackson's PropertyNamingStrategies. Can also be a
   * fully-qualified class name of a PropertyNamingStrategy implementation.
   */
  @Nullable
  private String propertyNamingStrategy;

  /**
   * Jackson visibility thresholds that can be used to limit which methods (and fields)
   * are auto-detected.
   */
  private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility = new EnumMap<>(PropertyAccessor.class);

  /**
   * Jackson on/off features that affect the way Java objects are serialized.
   */
  private final Map<SerializationFeature, Boolean> serialization = new EnumMap<>(SerializationFeature.class);

  /**
   * Jackson on/off features that affect the way Java objects are deserialized.
   */
  private final Map<DeserializationFeature, Boolean> deserialization = new EnumMap<>(DeserializationFeature.class);

  /**
   * Jackson general purpose on/off features.
   */
  private final Map<MapperFeature, Boolean> mapper = new EnumMap<>(MapperFeature.class);

  /**
   * Jackson on/off features for parsers.
   */
  private final Map<JsonParser.Feature, Boolean> parser = new EnumMap<>(JsonParser.Feature.class);

  /**
   * Jackson on/off features for generators.
   */
  private final Map<JsonGenerator.Feature, Boolean> generator = new EnumMap<>(JsonGenerator.Feature.class);

  /**
   * Controls the inclusion of properties during serialization. Configured with one of
   * the values in Jackson's JsonInclude.Include enumeration.
   */
  private JsonInclude.@Nullable Include defaultPropertyInclusion;

  /**
   * Global default setting (if any) for leniency.
   */
  @Nullable
  private Boolean defaultLeniency;

  /**
   * Strategy to use to auto-detect constructor, and in particular behavior with
   * single-argument constructors.
   */
  @Nullable
  private ConstructorDetectorStrategy constructorDetector;

  /**
   * Time zone used when formatting dates. For instance, "America/Los_Angeles" or
   * "GMT+10".
   */
  @Nullable
  private TimeZone timeZone = null;

  /**
   * Locale used for formatting.
   */
  @Nullable
  private Locale locale;

  private final Datatype datatype = new Datatype();

  @Nullable
  public String getDateFormat() {
    return this.dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  @Nullable
  public String getPropertyNamingStrategy() {
    return this.propertyNamingStrategy;
  }

  public void setPropertyNamingStrategy(String propertyNamingStrategy) {
    this.propertyNamingStrategy = propertyNamingStrategy;
  }

  public Map<PropertyAccessor, JsonAutoDetect.Visibility> getVisibility() {
    return this.visibility;
  }

  public Map<SerializationFeature, Boolean> getSerialization() {
    return this.serialization;
  }

  public Map<DeserializationFeature, Boolean> getDeserialization() {
    return this.deserialization;
  }

  public Map<MapperFeature, Boolean> getMapper() {
    return this.mapper;
  }

  public Map<JsonParser.Feature, Boolean> getParser() {
    return this.parser;
  }

  public Map<JsonGenerator.Feature, Boolean> getGenerator() {
    return this.generator;
  }

  public JsonInclude.@Nullable Include getDefaultPropertyInclusion() {
    return this.defaultPropertyInclusion;
  }

  public void setDefaultPropertyInclusion(JsonInclude.Include defaultPropertyInclusion) {
    this.defaultPropertyInclusion = defaultPropertyInclusion;
  }

  @Nullable
  public Boolean getDefaultLeniency() {
    return this.defaultLeniency;
  }

  public void setDefaultLeniency(Boolean defaultLeniency) {
    this.defaultLeniency = defaultLeniency;
  }

  @Nullable
  public ConstructorDetectorStrategy getConstructorDetector() {
    return this.constructorDetector;
  }

  public void setConstructorDetector(ConstructorDetectorStrategy constructorDetector) {
    this.constructorDetector = constructorDetector;
  }

  @Nullable
  public TimeZone getTimeZone() {
    return this.timeZone;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  @Nullable
  public Locale getLocale() {
    return this.locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public Datatype getDatatype() {
    return this.datatype;
  }

  public enum ConstructorDetectorStrategy {

    /**
     * Use heuristics to see if "properties" mode is to be used.
     */
    DEFAULT,

    /**
     * Assume "properties" mode if not explicitly annotated otherwise.
     */
    USE_PROPERTIES_BASED,

    /**
     * Assume "delegating" mode if not explicitly annotated otherwise.
     */
    USE_DELEGATING,

    /**
     * Refuse to decide implicit mode and instead throw an InvalidDefinitionException
     * for ambiguous cases.
     */
    EXPLICIT_ONLY

  }

  public static class Datatype {

    /**
     * Jackson on/off features for enums.
     */
    private final Map<EnumFeature, Boolean> enumFeatures = new EnumMap<>(EnumFeature.class);

    /**
     * Jackson on/off features for JsonNodes.
     */
    private final Map<JsonNodeFeature, Boolean> jsonNode = new EnumMap<>(JsonNodeFeature.class);

    public Map<EnumFeature, Boolean> getEnum() {
      return this.enumFeatures;
    }

    public Map<JsonNodeFeature, Boolean> getJsonNode() {
      return this.jsonNode;
    }

  }

}

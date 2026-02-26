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

package infra.jackson.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.context.properties.bind.Name;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;

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
  public @Nullable String dateFormat;

  /**
   * One of the constants on Jackson's PropertyNamingStrategies. Can also be a
   * fully-qualified class name of a PropertyNamingStrategy implementation.
   */
  public @Nullable String propertyNamingStrategy;

  /**
   * Jackson visibility thresholds that can be used to limit which methods (and fields)
   * are auto-detected.
   */
  public final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility = new EnumMap<>(PropertyAccessor.class);

  /**
   * Jackson on/off features that affect the way Java objects are serialized.
   */
  public final Map<SerializationFeature, Boolean> serialization = new EnumMap<>(SerializationFeature.class);

  /**
   * Jackson on/off features that affect the way Java objects are deserialized.
   */
  public final Map<DeserializationFeature, Boolean> deserialization = new EnumMap<>(DeserializationFeature.class);

  /**
   * Jackson general purpose on/off features.
   */
  public final Map<MapperFeature, Boolean> mapper = new EnumMap<>(MapperFeature.class);

  /**
   * Controls the inclusion of properties during serialization. Configured with one of
   * the values in Jackson's JsonInclude.Include enumeration.
   */
  public JsonInclude.@Nullable Include defaultPropertyInclusion;

  /**
   * Global default setting (if any) for leniency.
   */
  public @Nullable Boolean defaultLeniency;

  /**
   * Strategy to use to auto-detect constructor, and in particular behavior with
   * single-argument constructors.
   */
  public @Nullable ConstructorDetectorStrategy constructorDetector;

  /**
   * Time zone used when formatting dates. For instance, "America/Los_Angeles" or
   * "GMT+10".
   */
  public @Nullable TimeZone timeZone;

  /**
   * Locale used for formatting.
   */
  public @Nullable Locale locale;

  /**
   * Whether to find and add modules to the auto-configured JsonMapper.Builder using
   * MapperBuilder.findAndAddModules(ClassLoader).
   */
  public boolean findAndAddModules = true;

  @NestedConfigurationProperty
  public final Datatype datatype = new Datatype();

  /**
   * Jackson on/off token reader features common to multiple formats.
   */
  public final Map<StreamReadFeature, Boolean> read = new EnumMap<>(StreamReadFeature.class);

  /**
   * Jackson on/off token writer features common to multiple formats.
   */
  public final Map<StreamWriteFeature, Boolean> write = new EnumMap<>(StreamWriteFeature.class);

  @NestedConfigurationProperty
  public final Json json = new Json();

  @NestedConfigurationProperty
  public final Factory factory = new Factory();

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
    @Name("enum")
    public final Map<EnumFeature, Boolean> enumFeatures = new EnumMap<>(EnumFeature.class);

    /**
     * Jackson on/off features for JsonNodes.
     */
    public final Map<JsonNodeFeature, Boolean> jsonNode = new EnumMap<>(JsonNodeFeature.class);

    /**
     * Jackson on/off features for DateTimes.
     */
    public final Map<DateTimeFeature, Boolean> datetime = new EnumMap<>(DateTimeFeature.class);

  }

  public static class Json {

    /**
     * Jackson on/off token reader features that are specific to JSON.
     */
    public final Map<JsonReadFeature, Boolean> read = new EnumMap<>(JsonReadFeature.class);

    /**
     * Jackson on/off token writer features that are specific to JSON.
     */
    public final Map<JsonWriteFeature, Boolean> write = new EnumMap<>(JsonWriteFeature.class);

  }

  public static class Factory {

    public final Constraints constraints = new Constraints();

    public static class Constraints {

      public final Read read = new Read();

      public final Write write = new Write();

      public static class Read {

        /**
         * Maximum nesting depth. The depth is a count of objects and arrays that
         * have not been closed.
         */
        public int maxNestingDepth = 500;

        /**
         * Maximum allowed document length. A value less than or equal to zero
         * indicates that any length is acceptable.
         */
        public long maxDocumentLength = -1L;

        /**
         * Maximum allowed token count. A value less than or equal to zero
         * indicates that any count is acceptable.
         */
        public long maxTokenCount = -1L;

        /**
         * Maximum number length.
         */
        public int maxNumberLength = 1_000;

        /**
         * Maximum string length.
         */
        public int maxStringLength = 100_000_000;

        /**
         * Maximum name length.
         */
        public int maxNameLength = 50_000;

      }

      public static class Write {

        /**
         * Maximum nesting depth. The depth is a count of objects and arrays that
         * have not been closed.
         */
        public int maxNestingDepth = 500;

      }

    }

  }

}

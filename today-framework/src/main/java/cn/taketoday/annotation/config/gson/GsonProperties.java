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

package cn.taketoday.annotation.config.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.LongSerializationPolicy;

import cn.taketoday.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure {@link Gson}.
 *
 * @author Ivan Golovko
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "gson")
public class GsonProperties {

  /**
   * Whether to generate non-executable JSON by prefixing the output with some special
   * text.
   */
  private Boolean generateNonExecutableJson;

  /**
   * Whether to exclude all fields from consideration for serialization or
   * deserialization that do not have the "Expose" annotation.
   */
  private Boolean excludeFieldsWithoutExposeAnnotation;

  /**
   * Whether to serialize null fields.
   */
  private Boolean serializeNulls;

  /**
   * Whether to enable serialization of complex map keys (i.e. non-primitives).
   */
  private Boolean enableComplexMapKeySerialization;

  /**
   * Whether to exclude inner classes during serialization.
   */
  private Boolean disableInnerClassSerialization;

  /**
   * Serialization policy for Long and long types.
   */
  private LongSerializationPolicy longSerializationPolicy;

  /**
   * Naming policy that should be applied to an object's field during serialization and
   * deserialization.
   */
  private FieldNamingPolicy fieldNamingPolicy;

  /**
   * Whether to output serialized JSON that fits in a page for pretty printing.
   */
  private Boolean prettyPrinting;

  /**
   * Whether to be lenient about parsing JSON that doesn't conform to RFC 4627.
   */
  private Boolean lenient;

  /**
   * Whether to disable the escaping of HTML characters such as {@code '<', '>'}, etc.
   */
  private Boolean disableHtmlEscaping;

  /**
   * Format to use when serializing Date objects.
   */
  private String dateFormat;

  public Boolean getGenerateNonExecutableJson() {
    return this.generateNonExecutableJson;
  }

  public void setGenerateNonExecutableJson(Boolean generateNonExecutableJson) {
    this.generateNonExecutableJson = generateNonExecutableJson;
  }

  public Boolean getExcludeFieldsWithoutExposeAnnotation() {
    return this.excludeFieldsWithoutExposeAnnotation;
  }

  public void setExcludeFieldsWithoutExposeAnnotation(Boolean excludeFieldsWithoutExposeAnnotation) {
    this.excludeFieldsWithoutExposeAnnotation = excludeFieldsWithoutExposeAnnotation;
  }

  public Boolean getSerializeNulls() {
    return this.serializeNulls;
  }

  public void setSerializeNulls(Boolean serializeNulls) {
    this.serializeNulls = serializeNulls;
  }

  public Boolean getEnableComplexMapKeySerialization() {
    return this.enableComplexMapKeySerialization;
  }

  public void setEnableComplexMapKeySerialization(Boolean enableComplexMapKeySerialization) {
    this.enableComplexMapKeySerialization = enableComplexMapKeySerialization;
  }

  public Boolean getDisableInnerClassSerialization() {
    return this.disableInnerClassSerialization;
  }

  public void setDisableInnerClassSerialization(Boolean disableInnerClassSerialization) {
    this.disableInnerClassSerialization = disableInnerClassSerialization;
  }

  public LongSerializationPolicy getLongSerializationPolicy() {
    return this.longSerializationPolicy;
  }

  public void setLongSerializationPolicy(LongSerializationPolicy longSerializationPolicy) {
    this.longSerializationPolicy = longSerializationPolicy;
  }

  public FieldNamingPolicy getFieldNamingPolicy() {
    return this.fieldNamingPolicy;
  }

  public void setFieldNamingPolicy(FieldNamingPolicy fieldNamingPolicy) {
    this.fieldNamingPolicy = fieldNamingPolicy;
  }

  public Boolean getPrettyPrinting() {
    return this.prettyPrinting;
  }

  public void setPrettyPrinting(Boolean prettyPrinting) {
    this.prettyPrinting = prettyPrinting;
  }

  public Boolean getLenient() {
    return this.lenient;
  }

  public void setLenient(Boolean lenient) {
    this.lenient = lenient;
  }

  public Boolean getDisableHtmlEscaping() {
    return this.disableHtmlEscaping;
  }

  public void setDisableHtmlEscaping(Boolean disableHtmlEscaping) {
    this.disableHtmlEscaping = disableHtmlEscaping;
  }

  public String getDateFormat() {
    return this.dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

}

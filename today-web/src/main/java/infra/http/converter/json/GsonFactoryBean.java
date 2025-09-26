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

package infra.http.converter.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jspecify.annotations.Nullable;

import java.text.SimpleDateFormat;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;

/**
 * A {@link FactoryBean} for creating a Google Gson 2.x {@link Gson} instance.
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @since 4.0
 */
public class GsonFactoryBean implements FactoryBean<Gson>, InitializingBean {

  private boolean base64EncodeByteArrays = false;

  private boolean serializeNulls = false;

  private boolean prettyPrinting = false;

  private boolean disableHtmlEscaping = false;

  @Nullable
  private String dateFormatPattern;

  @Nullable
  private Gson gson;

  /**
   * Whether to Base64-encode {@code byte[]} properties when reading and
   * writing JSON.
   * <p>When set to {@code true}, a custom {@link com.google.gson.TypeAdapter} will be
   * registered via {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}
   * which serializes a {@code byte[]} property to and from a Base64-encoded String
   * instead of a JSON array.
   *
   * @see GsonBuilderUtils#gsonBuilderWithBase64EncodedByteArrays()
   */
  public void setBase64EncodeByteArrays(boolean base64EncodeByteArrays) {
    this.base64EncodeByteArrays = base64EncodeByteArrays;
  }

  /**
   * Whether to use the {@link GsonBuilder#serializeNulls()} option when writing
   * JSON. This is a shortcut for setting up a {@code Gson} as follows:
   * <pre class="code">
   * new GsonBuilder().serializeNulls().create();
   * </pre>
   */
  public void setSerializeNulls(boolean serializeNulls) {
    this.serializeNulls = serializeNulls;
  }

  /**
   * Whether to use the {@link GsonBuilder#setPrettyPrinting()} when writing
   * JSON. This is a shortcut for setting up a {@code Gson} as follows:
   * <pre class="code">
   * new GsonBuilder().setPrettyPrinting().create();
   * </pre>
   */
  public void setPrettyPrinting(boolean prettyPrinting) {
    this.prettyPrinting = prettyPrinting;
  }

  /**
   * Whether to use the {@link GsonBuilder#disableHtmlEscaping()} when writing
   * JSON. Set to {@code true} to disable HTML escaping in JSON. This is a
   * shortcut for setting up a {@code Gson} as follows:
   * <pre class="code">
   * new GsonBuilder().disableHtmlEscaping().create();
   * </pre>
   */
  public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
    this.disableHtmlEscaping = disableHtmlEscaping;
  }

  /**
   * Define the date/time format with a {@link SimpleDateFormat}-style pattern.
   * This is a shortcut for setting up a {@code Gson} as follows:
   * <pre class="code">
   * new GsonBuilder().setDateFormat(dateFormatPattern).create();
   * </pre>
   */
  public void setDateFormatPattern(String dateFormatPattern) {
    this.dateFormatPattern = dateFormatPattern;
  }

  @Override
  public void afterPropertiesSet() {
    GsonBuilder builder = (this.base64EncodeByteArrays ?
                           GsonBuilderUtils.gsonBuilderWithBase64EncodedByteArrays() : new GsonBuilder());
    if (this.serializeNulls) {
      builder.serializeNulls();
    }
    if (this.prettyPrinting) {
      builder.setPrettyPrinting();
    }
    if (this.disableHtmlEscaping) {
      builder.disableHtmlEscaping();
    }
    if (this.dateFormatPattern != null) {
      builder.setDateFormat(this.dateFormatPattern);
    }
    this.gson = builder.create();
  }

  /**
   * Return the created Gson instance.
   */
  @Override
  @Nullable
  public Gson getObject() {
    return this.gson;
  }

  @Override
  public Class<?> getObjectType() {
    return Gson.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}

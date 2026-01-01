/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.converter;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility for building and configuring an immutable collection of {@link HttpMessageConverter}
 * instances for {@link #forClient() client} or {@link #forServer() server} usage. You can
 * ask to {@link Builder#registerDefaults() register default converters with classpath detection}
 * and {@link Builder#withJsonConverter(HttpMessageConverter) override specific converters} that were detected.
 * Custom converters can be independently added in front of default ones.
 * Finally, {@link Builder#configureMessageConverters(Consumer) default and custom converters can be configured}.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface HttpMessageConverters extends Iterable<HttpMessageConverter<?>> {

  /**
   * Return true if this instance does not contain any message converters.
   */
  boolean isEmpty();

  /**
   * Returns the list of HTTP message converters.
   *
   * @return the list of {@link HttpMessageConverter} instances
   */
  List<HttpMessageConverter<?>> asList();

  /**
   * Create a builder instance, tailored for HTTP client usage.
   * <p>The following HTTP message converters can be detected and registered if available, in order:
   * <ol>
   * <li>All custom message converters configured with the builder
   * <li>{@link ByteArrayHttpMessageConverter}
   * <li>{@link StringHttpMessageConverter} with the {@link java.nio.charset.StandardCharsets#ISO_8859_1} charset
   * <li>{@link ResourceHttpMessageConverter}, with resource streaming support disabled
   * <li>a Multipart converter, using all detected and custom converters for part conversion
   * <li>A Kotlin Serialization JSON converter
   * <li>A JSON converter
   * <li>A Smile converter
   * <li>A Kotlin Serialization CBOR converter
   * <li>A CBOR converter
   * <li>A YAML converter
   * <li>An XML converter
   * <li>A ProtoBuf converter
   * <li>ATOM and RSS converters
   * </ol>
   */
  static ClientBuilder forClient() {
    return new DefaultHttpMessageConverters.DefaultClientBuilder();
  }

  /**
   * Create a builder instance, tailored for HTTP server usage.
   * <p>The following HTTP message converters can be detected and registered if available, in order:
   * <ol>
   *     <li>All custom message converters configured with the builder
   *     <li>{@link ByteArrayHttpMessageConverter}
   *     <li>{@link StringHttpMessageConverter} with the {@link java.nio.charset.StandardCharsets#ISO_8859_1} charset
   *     <li>{@link ResourceHttpMessageConverter}
   *     <li>{@link ResourceRegionHttpMessageConverter}
   *     <li>A JSON converter
   *     <li>A Smile converter
   *     <li>A CBOR converter
   *     <li>A YAML converter
   *     <li>An XML converter
   *     <li>A ProtoBuf converter
   *     <li>ATOM and RSS converters
   *     <li>a Multipart converter, using all detected and custom converters for part conversion
   * </ol>
   */
  static ServerBuilder forServer() {
    return new DefaultHttpMessageConverters.DefaultServerBuilder();
  }

  interface Builder<T extends Builder<T>> {

    /**
     * Register default converters using classpath detection.
     * Manual registrations like {@link #withJsonConverter(HttpMessageConverter)} will
     * override auto-detected ones.
     */
    T registerDefaults();

    /**
     * Override the default String {@code HttpMessageConverter}
     * with any converter supporting String conversion.
     *
     * @param stringMessageConverter the converter instance to use
     * @see StringHttpMessageConverter
     */
    T withStringConverter(HttpMessageConverter<?> stringMessageConverter);

    /**
     * Override the default Jackson 3.x JSON {@code HttpMessageConverter}
     * with any converter supporting the JSON format.
     *
     * @param jsonMessageConverter the converter instance to use
     * @see infra.http.converter.json.JacksonJsonHttpMessageConverter
     */
    T withJsonConverter(HttpMessageConverter<?> jsonMessageConverter);

    /**
     * Override the default Jackson 3.x XML {@code HttpMessageConverter}
     * with any converter supporting the XML format.
     *
     * @param xmlMessageConverter the converter instance to use
     * @see infra.http.converter.xml.JacksonXmlHttpMessageConverter
     */
    T withXmlConverter(HttpMessageConverter<?> xmlMessageConverter);

    /**
     * Override the default Jackson 3.x Smile {@code HttpMessageConverter}
     * with any converter supporting the Smile format.
     *
     * @param smileMessageConverter the converter instance to use
     * @see infra.http.converter.smile.JacksonSmileHttpMessageConverter
     */
    T withSmileConverter(HttpMessageConverter<?> smileMessageConverter);

    /**
     * Override the default Jackson 3.x CBOR {@code HttpMessageConverter}
     * with any converter supporting the CBOR format.
     *
     * @param cborMessageConverter the converter instance to use
     * @see infra.http.converter.cbor.JacksonCborHttpMessageConverter
     */
    T withCborConverter(HttpMessageConverter<?> cborMessageConverter);

    /**
     * Override the default Jackson 3.x Yaml {@code HttpMessageConverter}
     * with any converter supporting the Yaml format.
     *
     * @param yamlMessageConverter the converter instance to use
     * @see infra.http.converter.yaml.JacksonYamlHttpMessageConverter
     */
    T withYamlConverter(HttpMessageConverter<?> yamlMessageConverter);

    /**
     * Add a custom {@code HttpMessageConverter} to the list of converters, ahead of the default converters.
     *
     * @param customConverter the converter instance to add
     */
    T addCustomConverter(HttpMessageConverter<?> customConverter);

    /**
     * Add a consumer for configuring the selected message converters.
     *
     * @param configurer the configurer to use
     */
    T configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer);

    /**
     * Build and return the {@link HttpMessageConverters} instance configured by this builder.
     */
    HttpMessageConverters build();
  }

  /**
   * Client builder for an {@link HttpMessageConverters} instance.
   */
  interface ClientBuilder extends Builder<ClientBuilder> {
  }

  /**
   * Server builder for an {@link HttpMessageConverters} instance.
   */
  interface ServerBuilder extends Builder<ServerBuilder> {
  }

}

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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import infra.http.MediaType;
import infra.http.converter.cbor.JacksonCborHttpMessageConverter;
import infra.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import infra.http.converter.feed.AtomFeedHttpMessageConverter;
import infra.http.converter.feed.RssChannelHttpMessageConverter;
import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.http.converter.json.JsonbHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.smile.JacksonSmileHttpMessageConverter;
import infra.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import infra.http.converter.xml.JacksonXmlHttpMessageConverter;
import infra.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import infra.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import infra.http.converter.yaml.JacksonYamlHttpMessageConverter;
import infra.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * Default implementation for {@link HttpMessageConverters}.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class DefaultHttpMessageConverters implements HttpMessageConverters {

  private final List<HttpMessageConverter<?>> messageConverters;

  DefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  @Override
  public boolean isEmpty() {
    return this.messageConverters.isEmpty();
  }

  @Override
  public List<HttpMessageConverter<?>> asList() {
    return Collections.unmodifiableList(messageConverters);
  }

  @Override
  public Iterator<HttpMessageConverter<?>> iterator() {
    return this.messageConverters.iterator();
  }

  abstract static class DefaultBuilder {

    private static final boolean JACKSON_PRESENT;

    private static final boolean JACKSON_2_PRESENT;

    private static final boolean GSON_PRESENT;

    private static final boolean JSONB_PRESENT;

    private static final boolean JACKSON_XML_PRESENT;

    private static final boolean JACKSON_2_XML_PRESENT;

    private static final boolean JAXB_2_PRESENT;

    private static final boolean JACKSON_SMILE_PRESENT;

    private static final boolean JACKSON_2_SMILE_PRESENT;

    private static final boolean JACKSON_CBOR_PRESENT;

    private static final boolean JACKSON_2_CBOR_PRESENT;

    private static final boolean JACKSON_YAML_PRESENT;

    private static final boolean JACKSON_2_YAML_PRESENT;

    private static final boolean ROME_PRESENT;

    boolean registerDefaults;

    @Nullable ByteArrayHttpMessageConverter byteArrayConverter;

    @Nullable HttpMessageConverter<?> stringConverter;

    @Nullable HttpMessageConverter<?> resourceConverter;

    @Nullable HttpMessageConverter<?> resourceRegionConverter;

    @Nullable Consumer<HttpMessageConverter<?>> configurer;

    @Nullable HttpMessageConverter<?> jsonConverter;

    @Nullable HttpMessageConverter<?> xmlConverter;

    @Nullable HttpMessageConverter<?> smileConverter;

    @Nullable HttpMessageConverter<?> cborConverter;

    @Nullable HttpMessageConverter<?> yamlConverter;

    @Nullable HttpMessageConverter<?> protobufConverter;

    @Nullable HttpMessageConverter<?> atomConverter;

    @Nullable HttpMessageConverter<?> rssConverter;

    final List<HttpMessageConverter<?>> customConverters = new ArrayList<>();

    static {
      ClassLoader classLoader = DefaultBuilder.class.getClassLoader();
      JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
      JACKSON_2_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
              ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
      GSON_PRESENT = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
      JSONB_PRESENT = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
      JACKSON_SMILE_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
      JACKSON_2_SMILE_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
      JAXB_2_PRESENT = ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
      JACKSON_XML_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.xml.XmlMapper", classLoader);
      JACKSON_2_XML_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
      JACKSON_CBOR_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
      JACKSON_2_CBOR_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
      JACKSON_YAML_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.yaml.YAMLMapper", classLoader);
      JACKSON_2_YAML_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", classLoader);
      ROME_PRESENT = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
    }

    void setStringConverter(HttpMessageConverter<?> stringConverter) {
      checkConverterSupports(stringConverter, MediaType.TEXT_PLAIN);
      this.stringConverter = stringConverter;
    }

    void setJsonConverter(HttpMessageConverter<?> jsonConverter) {
      checkConverterSupports(jsonConverter, MediaType.APPLICATION_JSON);
      this.jsonConverter = jsonConverter;
    }

    void setXmlConverter(HttpMessageConverter<?> xmlConverter) {
      checkConverterSupports(xmlConverter, MediaType.TEXT_XML);
      this.xmlConverter = xmlConverter;
    }

    void setSmileConverter(HttpMessageConverter<?> smileConverter) {
      checkConverterSupports(smileConverter, new MediaType("application", "x-jackson-smile"));
      this.smileConverter = smileConverter;
    }

    void setCborConverter(HttpMessageConverter<?> cborConverter) {
      checkConverterSupports(cborConverter, MediaType.APPLICATION_CBOR);
      this.cborConverter = cborConverter;
    }

    void setYamlConverter(HttpMessageConverter<?> yamlConverter) {
      checkConverterSupports(yamlConverter, MediaType.APPLICATION_YAML);
      this.yamlConverter = yamlConverter;
    }

    private void checkConverterSupports(HttpMessageConverter<?> converter, MediaType mediaType) {
      for (MediaType supportedMediaType : converter.getSupportedMediaTypes()) {
        if (mediaType.equalsTypeAndSubtype(supportedMediaType)) {
          return;
        }
      }
      throw new IllegalArgumentException("converter should support '" + mediaType + "'");
    }

    void addCustomMessageConverter(HttpMessageConverter<?> customConverter) {
      Assert.notNull(customConverter, "'customConverter' is required");
      this.customConverters.add(customConverter);
    }

    void addMessageConverterConfigurer(Consumer<HttpMessageConverter<?>> configurer) {
      this.configurer = (this.configurer != null) ? configurer.andThen(this.configurer) : configurer;
    }

    List<HttpMessageConverter<?>> getBaseConverters() {
      ArrayList<HttpMessageConverter<?>> converters = new ArrayList<>();
      if (this.byteArrayConverter != null) {
        converters.add(this.byteArrayConverter);
      }
      if (this.stringConverter != null) {
        converters.add(this.stringConverter);
      }
      return converters;
    }

    List<HttpMessageConverter<?>> getCoreConverters() {
      List<HttpMessageConverter<?>> converters = new ArrayList<>();
      if (this.jsonConverter != null) {
        converters.add(this.jsonConverter);
      }
      if (this.smileConverter != null) {
        converters.add(this.smileConverter);
      }
      if (this.cborConverter != null) {
        converters.add(this.cborConverter);
      }
      if (this.yamlConverter != null) {
        converters.add(this.yamlConverter);
      }
      if (this.xmlConverter != null) {
        converters.add(this.xmlConverter);
      }
      if (this.protobufConverter != null) {
        converters.add(this.protobufConverter);
      }
      if (this.atomConverter != null) {
        converters.add(this.atomConverter);
      }
      if (this.rssConverter != null) {
        converters.add(this.rssConverter);
      }
      return converters;
    }

    List<HttpMessageConverter<?>> getCustomConverters() {
      return this.customConverters;
    }

    void detectMessageConverters() {
      this.byteArrayConverter = new ByteArrayHttpMessageConverter();

      if (this.stringConverter == null) {
        this.stringConverter = new StringHttpMessageConverter();
      }
      if (this.jsonConverter == null) {
        if (JACKSON_PRESENT) {
          this.jsonConverter = new JacksonJsonHttpMessageConverter();
        }
        else if (JACKSON_2_PRESENT) {
          this.jsonConverter = new MappingJackson2HttpMessageConverter();
        }
        else if (GSON_PRESENT) {
          this.jsonConverter = new GsonHttpMessageConverter();
        }
        else if (JSONB_PRESENT) {
          this.jsonConverter = new JsonbHttpMessageConverter();
        }
      }

      if (this.xmlConverter == null) {
        if (JACKSON_XML_PRESENT) {
          this.xmlConverter = new JacksonXmlHttpMessageConverter();
        }
        else if (JACKSON_2_XML_PRESENT) {
          this.xmlConverter = new MappingJackson2XmlHttpMessageConverter();
        }
        else if (JAXB_2_PRESENT) {
          this.xmlConverter = new Jaxb2RootElementHttpMessageConverter();
        }
      }

      if (this.smileConverter == null) {
        if (JACKSON_SMILE_PRESENT) {
          this.smileConverter = new JacksonSmileHttpMessageConverter();
        }
        else if (JACKSON_2_SMILE_PRESENT) {
          this.smileConverter = new MappingJackson2SmileHttpMessageConverter();
        }
      }

      if (this.cborConverter == null) {
        if (JACKSON_CBOR_PRESENT) {
          this.cborConverter = new JacksonCborHttpMessageConverter();
        }
        else if (JACKSON_2_CBOR_PRESENT) {
          this.cborConverter = new MappingJackson2CborHttpMessageConverter();
        }
      }

      if (this.yamlConverter == null) {
        if (JACKSON_YAML_PRESENT) {
          this.yamlConverter = new JacksonYamlHttpMessageConverter();
        }
        else if (JACKSON_2_YAML_PRESENT) {
          this.yamlConverter = new MappingJackson2YamlHttpMessageConverter();
        }
      }

      if (ROME_PRESENT) {
        if (this.atomConverter == null) {
          this.atomConverter = new AtomFeedHttpMessageConverter();
        }
        if (this.rssConverter == null) {
          this.rssConverter = new RssChannelHttpMessageConverter();
        }
      }
    }

  }

  static class DefaultClientBuilder extends DefaultBuilder implements ClientBuilder {

    @Override
    public DefaultClientBuilder registerDefaults() {
      this.registerDefaults = true;
      return this;
    }

    @Override
    public ClientBuilder withStringConverter(HttpMessageConverter<?> stringConverter) {
      setStringConverter(stringConverter);
      return this;
    }

    @Override
    public ClientBuilder withJsonConverter(HttpMessageConverter<?> jsonConverter) {
      setJsonConverter(jsonConverter);
      return this;
    }

    @Override
    public ClientBuilder withXmlConverter(HttpMessageConverter<?> xmlConverter) {
      setXmlConverter(xmlConverter);
      return this;
    }

    @Override
    public ClientBuilder withSmileConverter(HttpMessageConverter<?> smileConverter) {
      setSmileConverter(smileConverter);
      return this;
    }

    @Override
    public ClientBuilder withCborConverter(HttpMessageConverter<?> cborConverter) {
      setCborConverter(cborConverter);
      return this;
    }

    @Override
    public ClientBuilder withYamlConverter(HttpMessageConverter<?> yamlConverter) {
      setYamlConverter(yamlConverter);
      return this;
    }

    @Override
    public ClientBuilder addCustomConverter(HttpMessageConverter<?> converter) {
      addCustomMessageConverter(converter);
      return this;
    }

    @Override
    public ClientBuilder addCustomConverters(@Nullable List<HttpMessageConverter<?>> converters) {
      if (converters != null) {
        for (HttpMessageConverter<?> converter : converters) {
          addCustomMessageConverter(converter);
        }
      }
      return this;
    }

    @Override
    public ClientBuilder configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
      addMessageConverterConfigurer(configurer);
      return this;
    }

    @Override
    public HttpMessageConverters build() {
      if (registerDefaults) {
        this.resourceConverter = new ResourceHttpMessageConverter(false);
        detectMessageConverters();
      }
      ArrayList<HttpMessageConverter<?>> partConverters = new ArrayList<>(getCustomConverters());
      ArrayList<HttpMessageConverter<?>> allConverters = new ArrayList<>(getCustomConverters());
      if (registerDefaults) {
        partConverters.addAll(getCoreConverters());
        allConverters.addAll(getBaseConverters());
        if (resourceConverter != null) {
          allConverters.add(resourceConverter);
        }
      }
      if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
        allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
      }
      if (registerDefaults) {
        allConverters.addAll(getCoreConverters());
      }
      if (configurer != null) {
        allConverters.forEach(configurer);
      }
      return new DefaultHttpMessageConverters(allConverters);
    }
  }

  static class DefaultServerBuilder extends DefaultBuilder implements ServerBuilder {

    @Override
    public ServerBuilder registerDefaults() {
      this.registerDefaults = true;
      return this;
    }

    @Override
    public ServerBuilder withStringConverter(HttpMessageConverter<?> stringConverter) {
      setStringConverter(stringConverter);
      return this;
    }

    @Override
    public ServerBuilder withJsonConverter(HttpMessageConverter<?> jsonConverter) {
      setJsonConverter(jsonConverter);
      return this;
    }

    @Override
    public ServerBuilder withXmlConverter(HttpMessageConverter<?> xmlConverter) {
      setXmlConverter(xmlConverter);
      return this;
    }

    @Override
    public ServerBuilder withSmileConverter(HttpMessageConverter<?> smileConverter) {
      setSmileConverter(smileConverter);
      return this;
    }

    @Override
    public ServerBuilder withCborConverter(HttpMessageConverter<?> cborConverter) {
      setCborConverter(cborConverter);
      return this;
    }

    @Override
    public ServerBuilder withYamlConverter(HttpMessageConverter<?> yamlConverter) {
      setYamlConverter(yamlConverter);
      return this;
    }

    @Override
    public ServerBuilder addCustomConverter(HttpMessageConverter<?> converter) {
      addCustomMessageConverter(converter);
      return this;
    }

    @Override
    public ServerBuilder addCustomConverters(@Nullable List<HttpMessageConverter<?>> converters) {
      if (converters != null) {
        for (HttpMessageConverter<?> converter : converters) {
          addCustomMessageConverter(converter);
        }
      }
      return this;
    }

    @Override
    public ServerBuilder configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
      addMessageConverterConfigurer(configurer);
      return this;
    }

    @Override
    public HttpMessageConverters build() {
      if (registerDefaults) {
        this.resourceConverter = new ResourceHttpMessageConverter();
        this.resourceRegionConverter = new ResourceRegionHttpMessageConverter();
        detectMessageConverters();
      }
      ArrayList<HttpMessageConverter<?>> partConverters = new ArrayList<>(getCustomConverters());
      ArrayList<HttpMessageConverter<?>> allConverters = new ArrayList<>(getCustomConverters());
      if (registerDefaults) {
        partConverters.addAll(getCoreConverters());
        allConverters.addAll(getBaseConverters());
        if (resourceConverter != null) {
          allConverters.add(resourceConverter);
        }
        if (resourceRegionConverter != null) {
          allConverters.add(resourceRegionConverter);
        }
      }
      if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
        allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
      }
      if (registerDefaults) {
        allConverters.addAll(getCoreConverters());
      }
      if (configurer != null) {
        allConverters.forEach(configurer);
      }
      return new DefaultHttpMessageConverters(allConverters);
    }
  }

}

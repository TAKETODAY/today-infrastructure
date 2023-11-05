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

package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.converter.AbstractGenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConversionException;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.TypeUtils;

/**
 * Abstract base class for Jackson based and content type independent
 * {@link HttpMessageConverter} implementations.
 *
 * <p>Compatible with Jackson 2.9 to 2.12.
 *
 * @author Arjen Poutsma
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MappingJackson2HttpMessageConverter
 * @since 4.0
 */
public abstract class AbstractJackson2HttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {
  private static final HashMap<String, JsonEncoding> ENCODINGS;

  static {
    ENCODINGS = CollectionUtils.newHashMap(JsonEncoding.values().length);
    for (JsonEncoding encoding : JsonEncoding.values()) {
      ENCODINGS.put(encoding.getJavaName(), encoding);
    }
    ENCODINGS.put("US-ASCII", JsonEncoding.UTF8);
  }

  protected ObjectMapper defaultObjectMapper;

  @Nullable
  private Map<Class<?>, Map<MediaType, ObjectMapper>> objectMapperRegistrations;

  @Nullable
  private Boolean prettyPrint;

  @Nullable
  private final PrettyPrinter ssePrettyPrinter;

  protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper) {
    this.defaultObjectMapper = objectMapper;
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
    this.ssePrettyPrinter = prettyPrinter;
  }

  protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType supportedMediaType) {
    this(objectMapper);
    setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
  }

  protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType... supportedMediaTypes) {
    this(objectMapper);
    setSupportedMediaTypes(List.of(supportedMediaTypes));
  }

  /**
   * Configure the main {@code ObjectMapper} to use for Object conversion.
   * If not set, a default {@link ObjectMapper} instance is created.
   * <p>Setting a custom-configured {@code ObjectMapper} is one way to take
   * further control of the JSON serialization process. For example, an extended
   * {@link com.fasterxml.jackson.databind.ser.SerializerFactory}
   * can be configured that provides custom serializers for specific types.
   * Another option for refining the serialization process is to use Jackson's
   * provided annotations on the types to be serialized, in which case a
   * custom-configured ObjectMapper is unnecessary.
   *
   * @see #registerObjectMappersForType(Class, Consumer)
   */
  public void setObjectMapper(ObjectMapper objectMapper) {
    Assert.notNull(objectMapper, "ObjectMapper must not be null");
    this.defaultObjectMapper = objectMapper;
    configurePrettyPrint();
  }

  /**
   * Return the main {@code ObjectMapper} in use.
   */
  public ObjectMapper getObjectMapper() {
    return this.defaultObjectMapper;
  }

  /**
   * Configure the {@link ObjectMapper} instances to use for the given
   * {@link Class}. This is useful when you want to deviate from the
   * {@link #getObjectMapper() default} ObjectMapper or have the
   * {@code ObjectMapper} vary by {@code MediaType}.
   * <p><strong>Note:</strong> Use of this method effectively turns off use of
   * the default {@link #getObjectMapper() ObjectMapper} and
   * {@link #setSupportedMediaTypes(List) supportedMediaTypes} for the given
   * class. Therefore it is important for the mappings configured here to
   * {@link MediaType#includes(MediaType) include} every MediaType that must
   * be supported for the given class.
   *
   * @param clazz the type of Object to register ObjectMapper instances for
   * @param registrar a consumer to populate or otherwise update the
   * MediaType-to-ObjectMapper associations for the given Class
   */
  public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MediaType, ObjectMapper>> registrar) {
    if (this.objectMapperRegistrations == null) {
      this.objectMapperRegistrations = new LinkedHashMap<>();
    }
    Map<MediaType, ObjectMapper> registrations =
            this.objectMapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());
    registrar.accept(registrations);
  }

  /**
   * Return ObjectMapper registrations for the given class, if any.
   *
   * @param clazz the class to look up for registrations for
   * @return a map with registered MediaType-to-ObjectMapper registrations,
   * or empty if in case of no registrations for the given class.
   */
  @Nullable
  public Map<MediaType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
    for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
      if (entry.getKey().isAssignableFrom(clazz)) {
        return entry.getValue();
      }
    }
    return Collections.emptyMap();
  }

  @Override
  public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
    List<MediaType> result = null;
    for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
      if (entry.getKey().isAssignableFrom(clazz)) {
        if (result == null) {
          result = new ArrayList<>(entry.getValue().size());
        }
        result.addAll(entry.getValue().keySet());
      }
    }
    if (CollectionUtils.isNotEmpty(result)) {
      return result;
    }
    return ProblemDetail.class.isAssignableFrom(clazz)
           ? getMediaTypesForProblemDetail()
           : getSupportedMediaTypes();
  }

  private Map<Class<?>, Map<MediaType, ObjectMapper>> getObjectMapperRegistrations() {
    return this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap();
  }

  /**
   * Return the supported media type(s) for {@link ProblemDetail}.
   * By default, an empty list, unless overridden in subclasses.
   */
  protected List<MediaType> getMediaTypesForProblemDetail() {
    return Collections.emptyList();
  }

  /**
   * Whether to use the {@link DefaultPrettyPrinter} when writing JSON.
   * This is a shortcut for setting up an {@code ObjectMapper} as follows:
   * <pre class="code">
   * ObjectMapper mapper = new ObjectMapper();
   * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
   * converter.setObjectMapper(mapper);
   * </pre>
   */
  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
    configurePrettyPrint();
  }

  private void configurePrettyPrint() {
    if (this.prettyPrint != null) {
      this.defaultObjectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
    }
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return canRead(clazz, null, mediaType);
  }

  @Override
  public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
    if (!canRead(mediaType)) {
      return false;
    }
    JavaType javaType = getJavaType(type, contextClass);
    ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), mediaType);
    if (objectMapper == null) {
      return false;
    }
    AtomicReference<Throwable> causeRef = new AtomicReference<>();
    if (objectMapper.canDeserialize(javaType, causeRef)) {
      return true;
    }
    logWarningIfNecessary(javaType, causeRef.get());
    return false;
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    if (!canWrite(mediaType)) {
      return false;
    }
    if (mediaType != null && mediaType.getCharset() != null) {
      Charset charset = mediaType.getCharset();
      if (!ENCODINGS.containsKey(charset.name())) {
        return false;
      }
    }
    ObjectMapper objectMapper = selectObjectMapper(clazz, mediaType);
    if (objectMapper == null) {
      return false;
    }
    AtomicReference<Throwable> causeRef = new AtomicReference<>();
    if (objectMapper.canSerialize(clazz, causeRef)) {
      return true;
    }
    logWarningIfNecessary(clazz, causeRef.get());
    return false;
  }

  /**
   * Select an ObjectMapper to use, either the main ObjectMapper or another
   * if the handling for the given Class has been customized through
   * {@link #registerObjectMappersForType(Class, Consumer)}.
   */
  @Nullable
  private ObjectMapper selectObjectMapper(Class<?> targetType, @Nullable MediaType targetMediaType) {
    if (targetMediaType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
      return this.defaultObjectMapper;
    }
    for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
      if (typeEntry.getKey().isAssignableFrom(targetType)) {
        for (Map.Entry<MediaType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
          if (objectMapperEntry.getKey().includes(targetMediaType)) {
            return objectMapperEntry.getValue();
          }
        }
        // No matching registrations
        return null;
      }
    }
    // No registrations
    return this.defaultObjectMapper;
  }

  /**
   * Determine whether to log the given exception coming from a
   * {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} check.
   *
   * @param type the class that Jackson tested for (de-)serializability
   * @param cause the Jackson-thrown exception to evaluate
   * (typically a {@link JsonMappingException})
   */
  protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
    if (cause == null) {
      return;
    }

    // Do not log warning for serializer not found (note: different message wording on Jackson 2.9)
    boolean debugLevel = (cause instanceof JsonMappingException && cause.getMessage().startsWith("Cannot find"));

    if (debugLevel ? logger.isDebugEnabled() : logger.isWarnEnabled()) {
      String msg = "Failed to evaluate Jackson "
              + (type instanceof JavaType ? "de" : "") + "serialization for type [" + type + "]";
      if (debugLevel) {
        logger.debug(msg, cause);
      }
      else if (logger.isDebugEnabled()) {
        logger.warn(msg, cause);
      }
      else {
        logger.warn("{}: {}", msg, cause.toString());
      }
    }
  }

  @Override
  public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    JavaType javaType = getJavaType(type, contextClass);
    return readJavaType(javaType, inputMessage);
  }

  @Override
  protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    JavaType javaType = getJavaType(clazz, null);
    return readJavaType(javaType, inputMessage);
  }

  private Object readJavaType(JavaType javaType, HttpInputMessage inputMessage) throws IOException {
    MediaType contentType = inputMessage.getHeaders().getContentType();
    Charset charset = getCharset(contentType);

    ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), contentType);
    Assert.state(objectMapper != null, "No ObjectMapper for " + javaType);

    boolean isUnicode = ENCODINGS.containsKey(charset.name()) ||
            "UTF-16".equals(charset.name()) ||
            "UTF-32".equals(charset.name());
    try {
      InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
      if (inputMessage instanceof MappingJacksonInputMessage mappingJacksonInputMessage) {
        Class<?> deserializationView = mappingJacksonInputMessage.getDeserializationView();
        if (deserializationView != null) {
          ObjectReader objectReader = objectMapper.readerWithView(deserializationView).forType(javaType);
          objectReader = customizeReader(objectReader, javaType);
          if (isUnicode) {
            return objectReader.readValue(inputStream);
          }
          else {
            Reader reader = new InputStreamReader(inputStream, charset);
            return objectReader.readValue(reader);
          }
        }
      }

      ObjectReader objectReader = objectMapper.reader().forType(javaType);
      objectReader = customizeReader(objectReader, javaType);
      if (isUnicode) {
        return objectReader.readValue(inputStream);
      }
      else {
        Reader reader = new InputStreamReader(inputStream, charset);
        return objectReader.readValue(reader);
      }
    }
    catch (InvalidDefinitionException ex) {
      throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
    }
    catch (JsonProcessingException ex) {
      throw new HttpMessageNotReadableException("JSON parse error: " + ex.getOriginalMessage(), ex, inputMessage);
    }
  }

  /**
   * Subclasses can use this method to customize {@link ObjectReader} used
   * for reading values.
   *
   * @param reader the reader instance to customize
   * @param javaType the target type of element values to read to
   * @return the customized {@link ObjectReader}
   */
  protected ObjectReader customizeReader(ObjectReader reader, JavaType javaType) {
    return reader;
  }

  /**
   * Determine the charset to use for JSON input.
   * <p>By default this is either the charset from the input {@code MediaType}
   * or otherwise falling back on {@code UTF-8}. Can be overridden in subclasses.
   *
   * @param contentType the content type of the HTTP input message
   * @return the charset to use
   */
  protected Charset getCharset(@Nullable MediaType contentType) {
    if (contentType != null && contentType.getCharset() != null) {
      return contentType.getCharset();
    }
    else {
      return StandardCharsets.UTF_8;
    }
  }

  @Override
  protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    MediaType contentType = outputMessage.getHeaders().getContentType();
    JsonEncoding encoding = getJsonEncoding(contentType);

    ObjectMapper objectMapper = this.defaultObjectMapper;
    if (objectMapperRegistrations != null) {
      Class<?> clazz = object instanceof MappingJacksonValue mappingJacksonValue
                       ? mappingJacksonValue.getValue().getClass() : object.getClass();
      objectMapper = selectObjectMapper(clazz, contentType);
      if (objectMapper == null) {
        throw new IllegalStateException("No ObjectMapper for " + clazz.getName());
      }
    }

    OutputStream outputStream = StreamUtils.nonClosing(outputMessage.getBody());
    try (JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, encoding)) {
      writePrefix(generator, object);

      Object value = object;
      Class<?> serializationView = null;
      FilterProvider filters = null;
      JavaType javaType = null;

      if (object instanceof MappingJacksonValue mappingJacksonValue) {
        value = mappingJacksonValue.getValue();
        serializationView = mappingJacksonValue.getSerializationView();
        filters = mappingJacksonValue.getFilters();
      }
      if (type != null && TypeUtils.isAssignable(type, value.getClass())) {
        javaType = getJavaType(type, null);
      }

      ObjectWriter objectWriter = serializationView != null
                                  ? objectMapper.writerWithView(serializationView) : objectMapper.writer();
      if (filters != null) {
        objectWriter = objectWriter.with(filters);
      }
      if (javaType != null && (javaType.isContainerType() || javaType.isTypeOrSubTypeOf(Optional.class))) {
        objectWriter = objectWriter.forType(javaType);
      }
      SerializationConfig config = objectWriter.getConfig();
      if (contentType != null
              && config.isEnabled(SerializationFeature.INDENT_OUTPUT)
              && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM)) {
        objectWriter = objectWriter.with(this.ssePrettyPrinter);
      }
      objectWriter = customizeWriter(objectWriter, javaType, contentType);
      objectWriter.writeValue(generator, value);

      writeSuffix(generator, object);
      generator.flush();
    }
    catch (InvalidDefinitionException ex) {
      throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
    }
    catch (JsonProcessingException ex) {
      throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
    }
  }

  /**
   * Subclasses can use this method to customize {@link ObjectWriter} used
   * for writing values.
   *
   * @param writer the writer instance to customize
   * @param javaType the type of element values to write
   * @param contentType the selected media type
   * @return the customized {@link ObjectWriter}
   */
  protected ObjectWriter customizeWriter(
          ObjectWriter writer, @Nullable JavaType javaType, @Nullable MediaType contentType) {

    return writer;
  }

  /**
   * Write a prefix before the main content.
   *
   * @param generator the generator to use for writing content.
   * @param object the object to write to the output message.
   */
  protected void writePrefix(JsonGenerator generator, Object object) throws IOException { }

  /**
   * Write a suffix after the main content.
   *
   * @param generator the generator to use for writing content.
   * @param object the object to write to the output message.
   */
  protected void writeSuffix(JsonGenerator generator, Object object) throws IOException { }

  /**
   * Return the Jackson {@link JavaType} for the specified type and context class.
   *
   * @param type the generic type to return the Jackson JavaType for
   * @param contextClass a context class for the target type, for example a class
   * in which the target type appears in a method signature (can be {@code null})
   * @return the Jackson JavaType
   */
  protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
    return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
  }

  /**
   * Determine the JSON encoding to use for the given content type.
   *
   * @param contentType the media type as requested by the caller
   * @return the JSON encoding to use (never {@code null})
   */
  protected JsonEncoding getJsonEncoding(@Nullable MediaType contentType) {
    if (contentType != null && contentType.getCharset() != null) {
      Charset charset = contentType.getCharset();
      JsonEncoding encoding = ENCODINGS.get(charset.name());
      if (encoding != null) {
        return encoding;
      }
    }
    return JsonEncoding.UTF8;
  }

  @Override
  @Nullable
  protected MediaType getDefaultContentType(Object object) throws IOException {
    if (object instanceof MappingJacksonValue mappingJacksonValue) {
      object = mappingJacksonValue.getValue();
    }
    return super.getDefaultContentType(object);
  }

  @Override
  protected Long getContentLength(Object object, @Nullable MediaType contentType) throws IOException {
    if (object instanceof MappingJacksonValue mappingJacksonValue) {
      object = mappingJacksonValue.getValue();
    }
    return super.getContentLength(object, contentType);
  }

  @Override
  protected boolean supportsRepeatableWrites(Object o) {
    return true;
  }

}

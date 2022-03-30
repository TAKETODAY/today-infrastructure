/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.test.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.function.Supplier;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;

/**
 * AssertJ based JSON tester backed by Jackson. Usually instantiated via
 * {@link #initFields(Object, ObjectMapper)}, for example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private JacksonTester&lt;ExampleObject&gt; json;
 *
 *     &#064;Before
 *     public void setup() {
 *         ObjectMapper objectMapper = new ObjectMapper();
 *         JacksonTester.initFields(this, objectMapper);
 *     }
 *
 *     &#064;Test
 *     public void testWriteJson() throws IOException {
 *         ExampleObject object = //...
 *         assertThat(json.write(object)).isEqualToJson("expected.json");
 *     }
 *
 * }
 * </pre>
 *
 * See {@link AbstractJsonMarshalTester} for more details.
 *
 * @param <T> the type under test
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Diego Berrueta
 * @since 4.0
 */
public class JacksonTester<T> extends AbstractJsonMarshalTester<T> {

  private final ObjectMapper objectMapper;

  private Class<?> view;

  /**
   * Create a new {@link JacksonTester} instance.
   *
   * @param objectMapper the Jackson object mapper
   */
  protected JacksonTester(ObjectMapper objectMapper) {
    Assert.notNull(objectMapper, "ObjectMapper must not be null");
    this.objectMapper = objectMapper;
  }

  /**
   * Create a new {@link JacksonTester} instance.
   *
   * @param resourceLoadClass the source class used to load resources
   * @param type the type under test
   * @param objectMapper the Jackson object mapper
   */
  public JacksonTester(Class<?> resourceLoadClass, ResolvableType type, ObjectMapper objectMapper) {
    this(resourceLoadClass, type, objectMapper, null);
  }

  public JacksonTester(Class<?> resourceLoadClass, ResolvableType type, ObjectMapper objectMapper, Class<?> view) {
    super(resourceLoadClass, type);
    Assert.notNull(objectMapper, "ObjectMapper must not be null");
    this.objectMapper = objectMapper;
    this.view = view;
  }

  @Override
  protected JsonContent<T> getJsonContent(String json) {
    Configuration configuration = Configuration.builder().jsonProvider(new JacksonJsonProvider(this.objectMapper))
            .mappingProvider(new JacksonMappingProvider(this.objectMapper)).build();
    return new JsonContent<>(getResourceLoadClass(), getType(), json, configuration);
  }

  @Override
  protected T readObject(InputStream inputStream, ResolvableType type) throws IOException {
    return getObjectReader(type).readValue(inputStream);
  }

  @Override
  protected T readObject(Reader reader, ResolvableType type) throws IOException {
    return getObjectReader(type).readValue(reader);
  }

  private ObjectReader getObjectReader(ResolvableType type) {
    ObjectReader objectReader = this.objectMapper.readerFor(getType(type));
    if (this.view != null) {
      return objectReader.withView(this.view);
    }
    return objectReader;
  }

  @Override
  protected String writeObject(T value, ResolvableType type) throws IOException {
    return getObjectWriter(type).writeValueAsString(value);
  }

  private ObjectWriter getObjectWriter(ResolvableType type) {
    ObjectWriter objectWriter = this.objectMapper.writerFor(getType(type));
    if (this.view != null) {
      return objectWriter.withView(this.view);
    }
    return objectWriter;
  }

  private JavaType getType(ResolvableType type) {
    return this.objectMapper.constructType(type.getType());
  }

  /**
   * Utility method to initialize {@link JacksonTester} fields. See {@link JacksonTester
   * class-level documentation} for example usage.
   *
   * @param testInstance the test instance
   * @param objectMapper the object mapper
   * @see #initFields(Object, ObjectMapper)
   */
  public static void initFields(Object testInstance, ObjectMapper objectMapper) {
    new JacksonFieldInitializer().initFields(testInstance, objectMapper);
  }

  /**
   * Utility method to initialize {@link JacksonTester} fields. See {@link JacksonTester
   * class-level documentation} for example usage.
   *
   * @param testInstance the test instance
   * @param objectMapperFactory a factory to create the object mapper
   * @see #initFields(Object, ObjectMapper)
   */
  public static void initFields(Object testInstance, Supplier<ObjectMapper> objectMapperFactory) {
    new JacksonFieldInitializer().initFields(testInstance, objectMapperFactory);
  }

  /**
   * Returns a new instance of {@link JacksonTester} with the view that should be used
   * for json serialization/deserialization.
   *
   * @param view the view class
   * @return the new instance
   */
  public JacksonTester<T> forView(Class<?> view) {
    return new JacksonTester<>(getResourceLoadClass(), getType(), this.objectMapper, view);
  }

  /**
   * {@link FieldInitializer} for Jackson.
   */
  private static class JacksonFieldInitializer extends FieldInitializer<ObjectMapper> {

    protected JacksonFieldInitializer() {
      super(JacksonTester.class);
    }

    @Override
    protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
            ObjectMapper marshaller) {
      return new JacksonTester<>(resourceLoadClass, type, marshaller);
    }

  }

}

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

import java.io.IOException;
import java.io.Reader;
import java.util.function.Supplier;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import jakarta.json.bind.Jsonb;

/**
 * AssertJ based JSON tester backed by Jsonb. Usually instantiated via
 * {@link #initFields(Object, Jsonb)}, for example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 * 	private JsonbTester&lt;ExampleObject&gt; json;
 *
 * 	&#064;Before
 * 	public void setup() {
 * 		Jsonb jsonb = JsonbBuilder.create();
 * 		JsonbTester.initFields(this, jsonb);
 *  }
 *
 * 	&#064;Test
 * 	public void testWriteJson() throws IOException {
 * 		ExampleObject object = // ...
 * 		assertThat(json.write(object)).isEqualToJson(&quot;expected.json&quot;);
 *  }
 *
 * }
 * </pre>
 *
 * See {@link AbstractJsonMarshalTester} for more details.
 *
 * @param <T> the type under test
 * @author Eddú Meléndez
 * @since 4.0
 */
public class JsonbTester<T> extends AbstractJsonMarshalTester<T> {

  private final Jsonb jsonb;

  /**
   * Create a new uninitialized {@link JsonbTester} instance.
   *
   * @param jsonb the Jsonb instance
   */
  protected JsonbTester(Jsonb jsonb) {
    Assert.notNull(jsonb, "Jsonb is required");
    this.jsonb = jsonb;
  }

  /**
   * Create a new {@link JsonbTester} instance.
   *
   * @param resourceLoadClass the source class used to load resources
   * @param type the type under test
   * @param jsonb the Jsonb instance
   * @see #initFields(Object, Jsonb)
   */
  public JsonbTester(Class<?> resourceLoadClass, ResolvableType type, Jsonb jsonb) {
    super(resourceLoadClass, type);
    Assert.notNull(jsonb, "Jsonb is required");
    this.jsonb = jsonb;
  }

  @Override
  protected String writeObject(T value, ResolvableType type) throws IOException {
    return this.jsonb.toJson(value, type.getType());
  }

  @Override
  protected T readObject(Reader reader, ResolvableType type) throws IOException {
    return this.jsonb.fromJson(reader, type.getType());
  }

  /**
   * Utility method to initialize {@link JsonbTester} fields. See {@link JsonbTester
   * class-level documentation} for example usage.
   *
   * @param testInstance the test instance
   * @param jsonb the Jsonb instance
   */
  public static void initFields(Object testInstance, Jsonb jsonb) {
    new JsonbFieldInitializer().initFields(testInstance, jsonb);
  }

  /**
   * Utility method to initialize {@link JsonbTester} fields. See {@link JsonbTester
   * class-level documentation} for example usage.
   *
   * @param testInstance the test instance
   * @param jsonb an object factory to create the Jsonb instance
   */
  public static void initFields(Object testInstance, Supplier<Jsonb> jsonb) {
    new JsonbFieldInitializer().initFields(testInstance, jsonb);
  }

  /**
   * {@link FieldInitializer} for Jsonb.
   */
  private static class JsonbFieldInitializer extends FieldInitializer<Jsonb> {

    protected JsonbFieldInitializer() {
      super(JsonbTester.class);
    }

    @Override
    protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
            Jsonb marshaller) {
      return new JsonbTester<>(resourceLoadClass, type, marshaller);
    }

  }

}

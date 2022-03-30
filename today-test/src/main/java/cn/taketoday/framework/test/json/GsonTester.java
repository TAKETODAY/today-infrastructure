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

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Supplier;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;

/**
 * AssertJ based JSON tester backed by Gson. Usually instantiated via
 * {@link #initFields(Object, Gson)}, for example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private GsonTester&lt;ExampleObject&gt; json;
 *
 *     &#064;Before
 *     public void setup() {
 *         Gson gson = new GsonBuilder().create();
 *         GsonTester.initFields(this, gson);
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
 * @since 4.0
 */
public class GsonTester<T> extends AbstractJsonMarshalTester<T> {

  private final Gson gson;

  /**
   * Create a new uninitialized {@link GsonTester} instance.
   *
   * @param gson the Gson instance
   */
  protected GsonTester(Gson gson) {
    Assert.notNull(gson, "Gson must not be null");
    this.gson = gson;
  }

  /**
   * Create a new {@link GsonTester} instance.
   *
   * @param resourceLoadClass the source class used to load resources
   * @param type the type under test
   * @param gson the Gson instance
   * @see #initFields(Object, Gson)
   */
  public GsonTester(Class<?> resourceLoadClass, ResolvableType type, Gson gson) {
    super(resourceLoadClass, type);
    Assert.notNull(gson, "Gson must not be null");
    this.gson = gson;
  }

  @Override
  protected String writeObject(T value, ResolvableType type) throws IOException {
    return this.gson.toJson(value, type.getType());
  }

  @Override
  protected T readObject(Reader reader, ResolvableType type) throws IOException {
    return this.gson.fromJson(reader, type.getType());
  }

  /**
   * Utility method to initialize {@link GsonTester} fields. See {@link GsonTester
   * class-level documentation} for example usage.
   *
   * @param testInstance the test instance
   * @param gson the Gson instance
   */
  public static void initFields(Object testInstance, Gson gson) {
    new GsonFieldInitializer().initFields(testInstance, gson);
  }

  /**
   * Utility method to initialize {@link GsonTester} fields. See {@link GsonTester
   * class-level documentation} for example usage.
   *
   * @param testInstance the test instance
   * @param gson an object factory to create the Gson instance
   */
  public static void initFields(Object testInstance, Supplier<Gson> gson) {
    new GsonFieldInitializer().initFields(testInstance, gson);
  }

  /**
   * {@link FieldInitializer} for Gson.
   */
  private static class GsonFieldInitializer extends FieldInitializer<Gson> {

    protected GsonFieldInitializer() {
      super(GsonTester.class);
    }

    @Override
    protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
            Gson marshaller) {
      return new GsonTester<>(resourceLoadClass, type, marshaller);
    }

  }

}

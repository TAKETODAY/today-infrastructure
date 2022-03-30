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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;

/**
 * AssertJ based JSON tester that works with basic JSON strings. Allows testing of JSON
 * payloads created from any source, for example:<pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private BasicJsonTester json = new BasicJsonTester(getClass());
 *
 *     &#064;Test
 *     public void testWriteJson() throws IOException {
 *         assertThat(json.from("example.json")).extractingJsonPathStringValue("@.name")
 * .isEqualTo("Spring");
 *     }
 *
 * }
 * </pre>
 *
 * See {@link AbstractJsonMarshalTester} for more details.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0
 */
public class BasicJsonTester {

  private JsonLoader loader;

  /**
   * Create a new uninitialized {@link BasicJsonTester} instance.
   */
  protected BasicJsonTester() {
  }

  /**
   * Create a new {@link BasicJsonTester} instance that will load resources as UTF-8.
   *
   * @param resourceLoadClass the source class used to load resources
   */
  public BasicJsonTester(Class<?> resourceLoadClass) {
    this(resourceLoadClass, null);
  }

  /**
   * Create a new {@link BasicJsonTester} instance.
   *
   * @param resourceLoadClass the source class used to load resources
   * @param charset the charset used to load resources
   * @since 4.0
   */
  public BasicJsonTester(Class<?> resourceLoadClass, Charset charset) {
    Assert.notNull(resourceLoadClass, "ResourceLoadClass must not be null");
    this.loader = new JsonLoader(resourceLoadClass, charset);
  }

  /**
   * Initialize the marshal tester for use, configuring it to load JSON resources as
   * UTF-8.
   *
   * @param resourceLoadClass the source class used when loading relative classpath
   * resources
   */
  protected final void initialize(Class<?> resourceLoadClass) {
    initialize(resourceLoadClass, null);
  }

  /**
   * Initialize the marshal tester for use.
   *
   * @param resourceLoadClass the source class used when loading relative classpath
   * resources
   * @param charset the charset used when loading relative classpath resources
   * @since 4.0
   */
  protected final void initialize(Class<?> resourceLoadClass, Charset charset) {
    if (this.loader == null) {
      this.loader = new JsonLoader(resourceLoadClass, charset);
    }
  }

  /**
   * Create JSON content from the specified String source. The source can contain the
   * JSON itself or, if it ends with {@code .json}, the name of a resource to be loaded
   * using {@code resourceLoadClass}.
   *
   * @param source the JSON content or a {@code .json} resource name
   * @return the JSON content
   */
  public JsonContent<Object> from(CharSequence source) {
    verify();
    return getJsonContent(this.loader.getJson(source));
  }

  /**
   * Create JSON content from the specified resource path.
   *
   * @param path the path of the resource to load
   * @param resourceLoadClass the source class used to load the resource
   * @return the JSON content
   */
  public JsonContent<Object> from(String path, Class<?> resourceLoadClass) {
    verify();
    return getJsonContent(this.loader.getJson(path, resourceLoadClass));
  }

  /**
   * Create JSON content from the specified JSON bytes.
   *
   * @param source the bytes of JSON
   * @return the JSON content
   */
  public JsonContent<Object> from(byte[] source) {
    verify();
    return getJsonContent(this.loader.getJson(source));
  }

  /**
   * Create JSON content from the specified JSON file.
   *
   * @param source the file containing JSON
   * @return the JSON content
   */
  public JsonContent<Object> from(File source) {
    verify();
    return getJsonContent(this.loader.getJson(source));
  }

  /**
   * Create JSON content from the specified JSON input stream.
   *
   * @param source the input stream containing JSON
   * @return the JSON content
   */
  public JsonContent<Object> from(InputStream source) {
    verify();
    return getJsonContent(this.loader.getJson(source));
  }

  /**
   * Create JSON content from the specified JSON resource.
   *
   * @param source the resource containing JSON
   * @return the JSON content
   */
  public JsonContent<Object> from(Resource source) {
    verify();
    return getJsonContent(this.loader.getJson(source));
  }

  private void verify() {
    Assert.state(this.loader != null, "Uninitialized BasicJsonTester");
  }

  private JsonContent<Object> getJsonContent(String json) {
    return new JsonContent<>(this.loader.getResourceLoadClass(), null, json);
  }

}

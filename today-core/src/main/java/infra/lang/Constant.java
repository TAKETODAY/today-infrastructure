/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.lang;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A collection of commonly used constants across the application.
 * This interface defines a set of predefined values, such as strings, characters,
 * and arrays, that are frequently referenced in various parts of the codebase.
 * These constants are designed to provide consistency, improve readability,
 * and reduce the risk of errors caused by hard-coded values.
 *
 * <p>Constants defined here are grouped into logical categories for ease of use.
 * They include file-related constants, HTTP protocol identifiers, default encodings,
 * empty arrays, and special-purpose markers like {@link #DEFAULT_NONE}.
 *
 * <h3>Usage Examples</h3>
 *
 * Using {@code DEFAULT_NONE} as a fallback value in annotations:
 * <pre>{@code
 * @RequestParam(defaultValue = Constant.DEFAULT_NONE)
 * public void exampleMethod(@RequestParam String param) {
 *   // Method implementation
 * }
 * }</pre>
 *
 * Accessing predefined empty arrays:
 * <pre>{@code
 * String[] emptyStringArray = Constant.EMPTY_STRING_ARRAY;
 * }</pre>
 *
 * Referencing common string constants:
 * <pre>{@code
 * String httpProtocol = Constant.HTTP;
 * String httpsProtocol = Constant.HTTPS;
 * }</pre>
 *
 * Using path separators in file operations:
 * <pre>{@code
 * String filePath = "folder" + Constant.PATH_SEPARATOR + "file.txt";
 * }</pre>
 *
 * <h3>Special Notes</h3>
 *
 * The constant {@link #DEFAULT_NONE} is specifically designed to serve as a
 * non-null placeholder in annotation attributes where {@code null} is not allowed.
 * Its unique value ensures it will never conflict with user-defined inputs.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.web.annotation.RequestParam#defaultValue()
 * @see infra.web.annotation.RequestHeader#defaultValue()
 * @see infra.web.annotation.CookieValue#defaultValue()
 * @since 2018-01-16 10:56
 */
public interface Constant extends Serializable {

  /**
   * Constant defining a value for no default - as a replacement for
   * {@code null} which we cannot use in annotation attributes.
   * <p>This is an artificial arrangement of 16 unicode characters,
   * with its sole purpose being to never match user-declared values.
   *
   * @see infra.web.annotation.RequestParam#defaultValue()
   * @see infra.web.annotation.RequestHeader#defaultValue()
   * @see infra.web.annotation.CookieValue#defaultValue()
   */
  String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

  String HTTP = "http";

  String HTTPS = "https";

  byte[] EMPTY_BYTES = {};
  Field[] EMPTY_FIELDS = {};
  Method[] EMPTY_METHODS = {};
  Object[] EMPTY_OBJECTS = {};
  Class<?>[] EMPTY_CLASSES = {};
  String[] EMPTY_STRING_ARRAY = {};
  Annotation[] EMPTY_ANNOTATIONS = {};

  //
  // ----------------------------------------------------------------

  String SOURCE_FILE = "<generated>";

  String SUID_FIELD_NAME = "serialVersionUID";

  //@since 2.1.6

  /** The package separator character: {@code '.'}. */
  char PACKAGE_SEPARATOR = '.';

  /** The path separator character: {@code '/'}. */
  char PATH_SEPARATOR = '/';

  /**
   * The default charset.
   */
  Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  String BLANK = "";

  String VALUE = "value";

  String DEFAULT = "default";

}

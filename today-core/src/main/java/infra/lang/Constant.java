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

package infra.lang;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-01-16 10:56
 */
public interface Constant extends Serializable {

  String PROPERTIES_SUFFIX = ".properties";

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

  String QUOTATION_MARKS = "\"";

  int[] EMPTY_INT_ARRAY = {};

  String[] EMPTY_STRING_ARRAY = {};

  byte[] EMPTY_BYTES = {};
  File[] EMPTY_FILES = {};
  Field[] EMPTY_FIELDS = {};
  Method[] EMPTY_METHODS = {};
  Object[] EMPTY_OBJECTS = {};
  Class<?>[] EMPTY_CLASSES = {};
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

  String DEFAULT_ENCODING = "UTF-8";

  /**
   * The default charset.
   */
  Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  String SPLIT_REGEXP = "[;|,]";

  String BLANK = "";
  String VALUE = "value";
  String DEFAULT = "default";

}

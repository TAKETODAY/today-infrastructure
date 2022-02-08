/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.lang;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author TODAY 2018-01-16 10:56
 */
public interface Constant extends Serializable {
  String VERSION = "4.0";
  String PROPERTIES_SUFFIX = ".properties";

  /**
   * Constant defining a value for no default - as a replacement for
   * {@code null} which we cannot use in annotation attributes.
   * <p>This is an artificial arrangement of 16 unicode characters,
   * with its sole purpose being to never match user-declared values.
   *
   * @see cn.taketoday.web.annotation.RequestParam#defaultValue()
   * @see cn.taketoday.web.annotation.RequestHeader#defaultValue()
   * @see cn.taketoday.web.annotation.CookieValue#defaultValue()
   */
  String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

  String HTTP = "http";
  String HTTPS = "https";

  String QUOTATION_MARKS = "\"";

  String META_INFO_beans = "META-INF/beans";

  int DEFAULT_CAPACITY = 0; // @since 3.0

  String KEY_ROOT = "root";
  String KEY_RESULT = "result";

  int[] EMPTY_INT_ARRAY = {};

  String[] EMPTY_STRING_ARRAY = {};

  File[] EMPTY_FILE_ARRAY = {};
  Field[] EMPTY_FIELD_ARRAY = {};
  Method[] EMPTY_METHOD_ARRAY = {};
  Object[] EMPTY_OBJECT_ARRAY = {};
  Class<?>[] EMPTY_CLASS_ARRAY = {};
  Annotation[] EMPTY_ANNOTATION_ARRAY = {};

  //
  // ----------------------------------------------------------------

  String SOURCE_FILE = "<cglibGenerated>";
  String AOP_SOURCE_FILE = "<aopGenerated>";
  String SUID_FIELD_NAME = "serialVersionUID";

  //@since 2.1.6

  /** The package separator character: {@code '.'}. */
  char PACKAGE_SEPARATOR = '.';
  /** The path separator character: {@code '/'}. */
  char PATH_SEPARATOR = '/';

  String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

  String DEFAULT_ENCODING = "UTF-8";
  Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  String SPLIT_REGEXP = "[;|,]";

  String BLANK = "";
  String VALUE = "value";
  String EQUALS = "equals";
  String HASH_CODE = "hashCode";
  String TO_STRING = "toString";
  String ANNOTATION_TYPE = "annotationType";
  String DEFAULT = "default";

  String TYPE = "type";

}

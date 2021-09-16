/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.core;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.cglib.core.Signature;
import cn.taketoday.cglib.core.TypeUtils;
import cn.taketoday.util.ClassUtils;

import static cn.taketoday.cglib.core.TypeUtils.parseType;

/**
 * @author TODAY <br>
 * 2018-01-16 10:56
 */
public interface Constant extends Serializable {
  String VERSION = "4.0";
  String ENV_SERVLET = "javax.servlet.Servlet";
  boolean RUN_IN_SERVLET = ClassUtils.isPresent(ENV_SERVLET); // @since 3.0.3

  String HTTP = "http";
  String HTTPS = "https";

  String QUOTATION_MARKS = "\"";

  String META_INFO_beans = "META-INF/beans";
  String META_INFO_listeners = "META-INF/listeners";

  int DEFAULT_CAPACITY = 0; // @since 3.0

  String KEY_ROOT = "root";
  String KEY_RESULT = "result";

  int[] EMPTY_INT_ARRAY = {};

  String[] EMPTY_STRING_ARRAY = {};

  File[] EMPTY_FILE_ARRAY = {};
  Type[] TYPES_EMPTY_ARRAY = {};
  Field[] EMPTY_FIELD_ARRAY = {};
  Method[] EMPTY_METHOD_ARRAY = {};
  Object[] EMPTY_OBJECT_ARRAY = {};
  Class<?>[] EMPTY_CLASS_ARRAY = {};
  Annotation[] EMPTY_ANNOTATION_ARRAY = {};

  Serializable EMPTY_OBJECT = EmptyObject.INSTANCE;

  //
  // ----------------------------------------------------------------

  Signature SIG_STATIC = TypeUtils.parseSignature("void <clinit>()");

  Type TYPE_CONSTANT = Type.fromClass(Constant.class);
  Type TYPE_OBJECT_ARRAY = parseType("Object[]");
  Type TYPE_CLASS_ARRAY = parseType("Class[]");
  Type TYPE_STRING_ARRAY = parseType("String[]");

  Type TYPE_TYPE = Type.fromClass(Type.class);
  Type TYPE_ERROR = parseType("Error");
  //  Type TYPE_SYSTEM = parseType("System");
  Type TYPE_LONG = parseType("Long");
  Type TYPE_BYTE = parseType("Byte");
  Type TYPE_CLASS = parseType("Class");
  Type TYPE_FLOAT = parseType("Float");
  Type TYPE_SHORT = parseType("Short");
  Type TYPE_OBJECT = parseType("Object");
  Type TYPE_DOUBLE = parseType("Double");
  Type TYPE_STRING = parseType("String");
  Type TYPE_NUMBER = parseType("Number");
  Type TYPE_BOOLEAN = parseType("Boolean");
  Type TYPE_INTEGER = parseType("Integer");
  Type TYPE_CHARACTER = parseType("Character");
  Type TYPE_THROWABLE = parseType("Throwable");
  //  Type TYPE_CLASS_LOADER = parseType("ClassLoader");
  Type TYPE_STRING_BUFFER = parseType("StringBuffer");
  Type TYPE_BIG_INTEGER = parseType("java.math.BigInteger");
  Type TYPE_BIG_DECIMAL = parseType("java.math.BigDecimal");
  Type TYPE_RUNTIME_EXCEPTION = parseType("RuntimeException");
  Type TYPE_SIGNATURE = Type.fromClass(Signature.class);

  String STATIC_NAME = "<clinit>";
  String SOURCE_FILE = "<cglibGenerated>";
  String AOP_SOURCE_FILE = "<aopGenerated>";
  String SUID_FIELD_NAME = "serialVersionUID";

  int PRIVATE_FINAL_STATIC = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC;

  int SWITCH_STYLE_TRIE = 0;
  int SWITCH_STYLE_HASH = 1;
  int SWITCH_STYLE_HASHONLY = 2;

  //@since 2.1.6

  String CONSTRUCTOR_NAME = "<init>";
  String STATIC_CLASS_INIT = STATIC_NAME;

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

/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 * <p>
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.context.asm.Opcodes;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.Signature;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.factory.PropertySetter;
import cn.taketoday.context.io.Resource;

import static cn.taketoday.context.cglib.core.TypeUtils.parseType;

/**
 * @author TODAY <br>
 * 2018-01-16 10:56
 */
public interface Constant extends Serializable {

  String META_INFO_beans = "META-INF/beans";
  String META_INFO_listeners = "META-INF/listeners";

  int DEFAULT_CAPACITY = 0; // @since 3.0

  String CONTEXT_VERSION = "3.1";

  String KEY_ROOT = "root";
  String KEY_RESULT = "result";

  File[] EMPTY_FILE_ARRAY = {};
  Type[] TYPES_EMPTY_ARRAY = {};
  Field[] EMPTY_FIELD_ARRAY = {};
  Method[] EMPTY_METHOD_ARRAY = {};
  Object[] EMPTY_OBJECT_ARRAY = {};
  Class<?>[] EMPTY_CLASS_ARRAY = {};
  Resource[] EMPTY_RESOURCE_ARRAY = {};
  PropertySetter[] EMPTY_PROPERTY_VALUE = {};
  Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  AnnotationAttributes[] EMPTY_ANNOTATION_ATTRIBUTES = {};

  Serializable EMPTY_OBJECT = EmptyObject.INSTANCE;

  //
  // ----------------------------------------------------------------

  Signature SIG_STATIC = TypeUtils.parseSignature("void <clinit>()");

  Type TYPE_CONSTANT = Type.getType(Constant.class);
  Type TYPE_OBJECT_ARRAY = parseType("Object[]");
  Type TYPE_CLASS_ARRAY = parseType("Class[]");
  Type TYPE_STRING_ARRAY = parseType("String[]");

  Type TYPE_TYPE = Type.getType(Type.class);
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
  Type TYPE_SIGNATURE = parseType(Signature.class);

  String STATIC_NAME = "<clinit>";
  String SOURCE_FILE = "<cglibGenerated>";
  String AOP_SOURCE_FILE = "<aopGenerated>";
  String SUID_FIELD_NAME = "serialVersionUID";

  int PRIVATE_FINAL_STATIC = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC;

  int SWITCH_STYLE_TRIE = 0;
  int SWITCH_STYLE_HASH = 1;
  int SWITCH_STYLE_HASHONLY = 2;

  //@off
    /** Bytes per Kilobyte.*/
    long 	BYTES_PER_KB 			= 1024;
    /** Bytes per Megabyte. */
    long 	BYTES_PER_MB 			= BYTES_PER_KB * 1024;
    /** Bytes per Gigabyte.*/
    long 	BYTES_PER_GB 			= BYTES_PER_MB * 1024;
    /** Bytes per Terabyte.*/
    long 	BYTES_PER_TB 			= BYTES_PER_GB * 1024;

    //@since 2.1.6
    String	ENABLE_FULL_PROTOTYPE	= "enable.full.prototype";
    String	ENABLE_FULL_LIFECYCLE	= "enable.full.lifecycle";
    String[]  EMPTY_STRING_ARRAY	= new String[0];
    String  CONSTRUCTOR_NAME 		= "<init>";
    String  STATIC_CLASS_INIT 		= STATIC_NAME;
    /** Suffix for array class names: {@code "[]"}. */
    String  ARRAY_SUFFIX 			= "[]";
    /** Prefix for internal array class names: {@code "["}. */
    String  INTERNAL_ARRAY_PREFIX 	= "[";
    /** Prefix for internal non-primitive array class names: {@code "[L"}. */
    String  NON_PRIMITIVE_ARRAY_PREFIX = "[L";
    /** The package separator character: {@code '.'}. */
    char 	PACKAGE_SEPARATOR 		= '.';
    /** The path separator character: {@code '/'}. */
    char 	PATH_SEPARATOR 			= '/';
    char 	WINDOWS_PATH_SEPARATOR 	= '\\';
    char 	INNER_CLASS_SEPARATOR 	= '$';
    String	ENV						= "env";
    String 	EL_PREFIX 				= "${";

    String 	DEFAULT_DATE_FORMAT		= "yyyy-MM-dd HH:mm:ss.SSS";

    /** The CGLIB class separator: {@code "$$"}. */
    String 	CGLIB_CLASS_SEPARATOR 	= "$$";
    char 	CGLIB_CHAR_SEPARATOR 	= INNER_CLASS_SEPARATOR;

    String 	PROTOCOL_JAR 			= "jar";
    String 	PROTOCOL_FILE 			= "file";
    String 	JAR_ENTRY_URL_PREFIX 	= "jar:file:";
    String 	JAR_SEPARATOR 			= "!/";
    int[] 	EMPTY_INT_ARRAY 		= new int[0];


    String	KEY_USE_SIMPLE_NAME		= "context.name.simple";
    String 	KEY_ACTIVE_PROFILES 	= "context.active.profiles";

    String 	DEFAULT_ENCODING 		= "UTF-8";
    Charset DEFAULT_CHARSET 		= StandardCharsets.UTF_8;

    String 	GET_BEAN 				= "getBean";
    String 	ON_APPLICATION_EVENT 	= "onApplicationEvent";
    String 	PROPERTIES_SUFFIX 		= ".properties";
    String 	PLACE_HOLDER_PREFIX 	= "#{";
    char 	PLACE_HOLDER_SUFFIX 	= '}';
    String 	SPLIT_REGEXP 			= "[;|,]";

    String	BLANK					= "";
    String	CLASS_FILE_SUFFIX		= ".class";
    String 	SCOPE 					= "scope";
    String 	VALUE 					= "value";
    String 	EQUALS 					= "equals";
    String 	HASH_CODE 				= "hashCode";
    String 	TO_STRING 				= "toString";
    String 	ANNOTATION_TYPE 		= "annotationType";
    String	DEFAULT					= "default";

    /**********************************************
     * @since 2.1.2
     */
    String 	CLASS_PATH_PREFIX 		= "classpath:";
    String	INIT_METHODS			    = "initMethods";
    String	DESTROY_METHODS			  = "destroyMethods";
    String 	TYPE 					        = "type";
    /**
     **********************************************/ //@on

  /**
   * @since 2.1.7
   */
  String SINGLETON = "singleton";

  /**
   * @since 2.1.7
   */
  String PROTOTYPE = "prototype";

  /** URL prefix for loading from the file system: "file:". */
  String FILE_URL_PREFIX = "file:";
  /** URL prefix for loading from a jar file: "jar:". */
  String JAR_URL_PREFIX = "jar:";
  /** URL prefix for loading from a war file on Tomcat: "war:". */
  String WAR_URL_PREFIX = "war:";
  /** URL protocol for a file in the file system: "file". */
  String URL_PROTOCOL_FILE = PROTOCOL_FILE;
  /** URL protocol for an entry from a jar file: "jar". */
  String URL_PROTOCOL_JAR = PROTOCOL_JAR;
  /** URL protocol for an entry from a war file: "war". */
  String URL_PROTOCOL_WAR = "war";
  /** URL protocol for an entry from a zip file: "zip". */
  String URL_PROTOCOL_ZIP = "zip";
  /** URL protocol for an entry from a WebSphere jar file: "wsjar". */
  String URL_PROTOCOL_WSJAR = "wsjar";
  /** URL protocol for an entry from a JBoss jar file: "vfszip". */
  String URL_PROTOCOL_VFSZIP = "vfszip";
  /** URL protocol for a JBoss file system resource: "vfsfile". */
  String URL_PROTOCOL_VFSFILE = "vfsfile";
  /** File extension for a regular jar file: ".jar". */
  String JAR_FILE_EXTENSION = ".jar";
  /** Separator between JAR URL and file path within the JAR: "!/". */
  String JAR_URL_SEPARATOR = JAR_SEPARATOR;
  /** Special separator between WAR URL and jar part on Tomcat. */
  String WAR_URL_SEPARATOR = "*/";

  String TOP_PATH = "..";
  String CURRENT_PATH = ".";
  String FOLDER_SEPARATOR = "/";
  String WINDOWS_FOLDER_SEPARATOR = "\\";
  char EXTENSION_SEPARATOR = PACKAGE_SEPARATOR;

  /* Indicates the ASM API version that is used throughout cglib */
  //  int         ASM_API                 = ASM7;

  /*************************************************
   * Parameter Types
   */
  byte TYPE_NULL = 0x00;
  byte TYPE_THROWING = 0x01;
  byte TYPE_ARGUMENT = 0x02;
  byte TYPE_ARGUMENTS = 0x03;
  byte TYPE_RETURNING = 0x04;
  byte TYPE_ANNOTATED = 0x05;
  byte TYPE_JOIN_POINT = 0x06;
  byte TYPE_ATTRIBUTE = 0x07;
}

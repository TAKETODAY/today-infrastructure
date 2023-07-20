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

package cn.taketoday.aot.nativex;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.util.MimeType;

/**
 * Tests for {@link ReflectionHintsWriter}.
 *
 * @author Sebastien Deleuze
 */
public class ReflectionHintsWriterTests {

  @Test
  void empty() throws JSONException {
    ReflectionHints hints = new ReflectionHints();
    assertEquals("[]", hints);
  }

  @Test
  void one() throws JSONException {
    ReflectionHints hints = new ReflectionHints();
    hints.registerType(StringDecoder.class, builder -> builder
            .onReachableType(String.class)
            .withMembers(MemberCategory.PUBLIC_FIELDS, MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INTROSPECT_DECLARED_METHODS,
                    MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.PUBLIC_CLASSES, MemberCategory.DECLARED_CLASSES)
            .withField("DEFAULT_CHARSET")
            .withField("defaultCharset")
            .withConstructor(TypeReference.listOf(List.class, boolean.class, MimeType.class), ExecutableMode.INTROSPECT)
            .withMethod("setDefaultCharset", List.of(TypeReference.of(Charset.class)), ExecutableMode.INVOKE)
            .withMethod("getDefaultCharset", Collections.emptyList(), ExecutableMode.INTROSPECT));
    assertEquals("""
            [
            	{
            		"name": "cn.taketoday.core.codec.StringDecoder",
            		"condition": { "typeReachable": "java.lang.String" },
            		"allPublicFields": true,
            		"allDeclaredFields": true,
            		"queryAllPublicConstructors": true,
            		"queryAllDeclaredConstructors": true,
            		"allPublicConstructors": true,
            		"allDeclaredConstructors": true,
            		"queryAllPublicMethods": true,
            		"queryAllDeclaredMethods": true,
            		"allPublicMethods": true,
            		"allDeclaredMethods": true,
            		"allPublicClasses": true,
            		"allDeclaredClasses": true,
            		"fields": [
            			{ "name": "DEFAULT_CHARSET" },
            			{ "name": "defaultCharset" }
            		],
            		"methods": [
            			{ "name": "setDefaultCharset", "parameterTypes": [ "java.nio.charset.Charset" ] }
            		],
            		"queriedMethods":  [
            			{ "name": "<init>", "parameterTypes": [ "java.util.List", "boolean", "cn.taketoday.util.MimeType" ] },
            			{ "name": "getDefaultCharset", "parameterTypes": [ ] }
            		]
            	}
            ]""", hints);
  }

  @Test
  void two() throws JSONException {
    ReflectionHints hints = new ReflectionHints();
    hints.registerType(Integer.class, builder -> {
    });
    hints.registerType(Long.class, builder -> {
    });

    assertEquals("""
            [
            	{ "name": "java.lang.Integer" },
            	{ "name": "java.lang.Long" }
            ]""", hints);
  }

  @Test
  void queriedMethods() throws JSONException {
    ReflectionHints hints = new ReflectionHints();
    hints.registerType(Integer.class, builder -> builder.withMethod("parseInt",
            TypeReference.listOf(String.class), ExecutableMode.INTROSPECT));

    assertEquals("""
            [
            	{
            		"name": "java.lang.Integer",
            		"queriedMethods": [
            			{
            				"name": "parseInt",
            				"parameterTypes": ["java.lang.String"]
            			}
            		]
            	}
            ]
            """, hints);
  }

  @Test
  void methods() throws JSONException {
    ReflectionHints hints = new ReflectionHints();
    hints.registerType(Integer.class, builder -> builder.withMethod("parseInt",
            TypeReference.listOf(String.class), ExecutableMode.INVOKE));

    assertEquals("""
            [
            	{
            		"name": "java.lang.Integer",
            		"methods": [
            			{
            				"name": "parseInt",
            				"parameterTypes": ["java.lang.String"]
            			}
            		]
            	}
            ]
            """, hints);
  }

  @Test
  void methodWithInnerClassParameter() throws JSONException {
    ReflectionHints hints = new ReflectionHints();
    hints.registerType(Integer.class, builder -> builder.withMethod("test",
            TypeReference.listOf(Inner.class), ExecutableMode.INVOKE));

    assertEquals("""
            [
            	{
            		"name": "java.lang.Integer",
            		"methods": [
            			{
            				"name": "test",
            				"parameterTypes": ["cn.taketoday.aot.nativex.ReflectionHintsWriterTests$Inner"]
            			}
            		]
            	}
            ]
            """, hints);
  }

  @Test
  void methodAndQueriedMethods() throws JSONException {
    ReflectionHints hints = new ReflectionHints();
    hints.registerType(Integer.class, builder -> builder.withMethod("parseInt",
            TypeReference.listOf(String.class), ExecutableMode.INVOKE));
    hints.registerType(Integer.class, builder -> builder.withMethod("parseInt",
            TypeReference.listOf(String.class, int.class), ExecutableMode.INTROSPECT));

    assertEquals("""
            [
            	{
            		"name": "java.lang.Integer",
            		"queriedMethods": [
            			{
            				"name": "parseInt",
            				"parameterTypes": ["java.lang.String", "int"]
            			}
            		],
            		"methods": [
            			{
            				"name": "parseInt",
            				"parameterTypes": ["java.lang.String"]
            			}
            		]
            	}
            ]
            """, hints);
  }

  @Test
  void ignoreLambda() throws JSONException {
    Runnable anonymousRunnable = () -> { };
    ReflectionHints hints = new ReflectionHints();
    hints.registerType(anonymousRunnable.getClass());
    assertEquals("[]", hints);
  }

  private void assertEquals(String expectedString, ReflectionHints hints) throws JSONException {
    StringWriter out = new StringWriter();
    BasicJsonWriter writer = new BasicJsonWriter(out, "\t");
    ReflectionHintsWriter.write(writer, hints);
    JSONAssert.assertEquals(expectedString, out.toString(), JSONCompareMode.NON_EXTENSIBLE);
  }

  static class Inner {

  }

}

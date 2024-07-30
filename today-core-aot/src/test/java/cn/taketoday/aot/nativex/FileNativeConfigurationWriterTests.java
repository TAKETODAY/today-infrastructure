/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.nativex;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ProxyHints;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.SerializationHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileNativeConfigurationWriter}.
 *
 * @author Sebastien Deleuze
 * @author Janne Valkealahti
 * @author Sam Brannen
 */
class FileNativeConfigurationWriterTests {

	@TempDir
	static Path tempDir;


	@Test
	void emptyConfig() {
		Path empty = tempDir.resolve("empty");
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(empty);
		generator.write(new RuntimeHints());
		assertThat(empty.toFile().listFiles()).isNull();
	}

	@Test
	void serializationConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		SerializationHints serializationHints = hints.serialization();
		serializationHints.registerType(Integer.class);
		serializationHints.registerType(Long.class);
		generator.write(hints);
		assertEquals("""
				[
					{ "name": "java.lang.Integer" },
					{ "name": "java.lang.Long" }
				]""", "serialization-config.json");
	}

	@Test
	void proxyConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ProxyHints proxyHints = hints.proxies();
		proxyHints.registerJdkProxy(Function.class);
		proxyHints.registerJdkProxy(Function.class, Consumer.class);
		generator.write(hints);
		assertEquals("""
				[
					{ "interfaces": [ "java.util.function.Function" ] },
					{ "interfaces": [ "java.util.function.Function", "java.util.function.Consumer" ] }
				]""", "proxy-config.json");
	}

	@Test
	void reflectionConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ReflectionHints reflectionHints = hints.reflection();
		reflectionHints.registerType(StringDecoder.class, builder -> builder
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
				.withMethod("setDefaultCharset", TypeReference.listOf(Charset.class), ExecutableMode.INVOKE)
				.withMethod("getDefaultCharset", Collections.emptyList(), ExecutableMode.INTROSPECT));
		generator.write(hints);
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
				]""", "reflect-config.json");
	}

	@Test
	void jniConfig() throws IOException, JSONException {
		// same format as reflection so just test basic file generation
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ReflectionHints jniHints = hints.jni();
		jniHints.registerType(StringDecoder.class, builder -> builder.onReachableType(String.class));
		generator.write(hints);
		assertEquals("""
				[
					{
						"name": "cn.taketoday.core.codec.StringDecoder",
						"condition": { "typeReachable": "java.lang.String" }
					}
				]""", "jni-config.json");
	}

	@Test
	void resourceConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ResourceHints resourceHints = hints.resources();
		resourceHints.registerPattern("com/example/test.properties");
		resourceHints.registerPattern("com/example/another.properties");
		generator.write(hints);
		assertEquals("""
				{
					"resources": {
						"includes": [
							{"pattern": "\\\\Qcom/example/test.properties\\\\E"},
							{"pattern": "\\\\Q/\\\\E"},
							{"pattern": "\\\\Qcom\\\\E"},
							{"pattern": "\\\\Qcom/example\\\\E"},
							{"pattern": "\\\\Qcom/example/another.properties\\\\E"}
						]
					}
				}""", "resource-config.json");
	}

	@Test
	void namespace() {
		String groupId = "foo.bar";
		String artifactId = "baz";
		String filename = "resource-config.json";
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir, groupId, artifactId);
		RuntimeHints hints = new RuntimeHints();
		ResourceHints resourceHints = hints.resources();
		resourceHints.registerPattern("com/example/test.properties");
		generator.write(hints);
		Path jsonFile = tempDir.resolve("META-INF").resolve("native-image").resolve(groupId).resolve(artifactId).resolve(filename);
		assertThat(jsonFile.toFile()).exists();
	}

	private void assertEquals(String expectedString, String filename) throws IOException, JSONException {
		Path jsonFile = tempDir.resolve("META-INF").resolve("native-image").resolve(filename);
		String content = Files.readString(jsonFile);
		JSONAssert.assertEquals(expectedString, content, JSONCompareMode.NON_EXTENSIBLE);
	}

}

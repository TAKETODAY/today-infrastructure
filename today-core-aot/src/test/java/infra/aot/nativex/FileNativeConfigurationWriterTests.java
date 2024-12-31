/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aot.nativex;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ProxyHints;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.ResourceHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.SerializationHints;
import infra.aot.hint.TypeReference;
import infra.core.codec.StringDecoder;

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
            {
            	"serialization": [
            		{ "type": "java.lang.Integer" },
            		{ "type": "java.lang.Long" }
            	]
            }
            """);
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
            {
            	"reflection": [
            		{ type: {"proxy": [ "java.util.function.Function" ] } },
            		{ type: {"proxy": [ "java.util.function.Function", "java.util.function.Consumer" ] } }
            	]
            }
            """);
  }

  @Test
  void reflectionConfig() throws IOException, JSONException {
    FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
    RuntimeHints hints = new RuntimeHints();
    ReflectionHints reflectionHints = hints.reflection();
    reflectionHints.registerType(StringDecoder.class, builder -> builder
            .onReachableType(String.class)
            .withMembers(MemberCategory.INVOKE_PUBLIC_FIELDS, MemberCategory.INVOKE_DECLARED_FIELDS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS)
            .withField("DEFAULT_CHARSET")
            .withField("defaultCharset")
            .withMethod("setDefaultCharset", TypeReference.listOf(Charset.class), ExecutableMode.INVOKE));
    generator.write(hints);
    assertEquals("""
            {
            	"reflection": [
            		{
            			"type": "infra.core.codec.StringDecoder",
            			"condition": { "typeReached": "java.lang.String" },
            			"allPublicFields": true,
            			"allDeclaredFields": true,
            			"allPublicConstructors": true,
            			"allDeclaredConstructors": true,
            			"allPublicMethods": true,
            			"allDeclaredMethods": true,
            			"fields": [
            				{ "name": "DEFAULT_CHARSET" },
            				{ "name": "defaultCharset" }
            			],
            			"methods": [
            				{ "name": "setDefaultCharset", "parameterTypes": [ "java.nio.charset.Charset" ] }
            			]
            		}
            	]
            }
            """);
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
            {
            	"jni": [
            		{
            			"type": "infra.core.codec.StringDecoder",
            			"condition": { "typeReached": "java.lang.String" }
            		}
            	]
            }""");
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
            	"resources": [
            			{"glob": "com/example/test.properties"},
            			{"glob": "/"},
            			{"glob": "com"},
            			{"glob": "com/example"},
            			{"glob": "com/example/another.properties"}
            	]
            }""");
  }

  @Test
  void namespace() {
    String groupId = "foo.bar";
    String artifactId = "baz";
    String filename = "reachability-metadata.json";
    FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir, groupId, artifactId);
    RuntimeHints hints = new RuntimeHints();
    ResourceHints resourceHints = hints.resources();
    resourceHints.registerPattern("com/example/test.properties");
    generator.write(hints);
    Path jsonFile = tempDir.resolve("META-INF").resolve("native-image").resolve(groupId).resolve(artifactId).resolve(filename);
    assertThat(jsonFile.toFile()).exists();
  }

  private void assertEquals(String expectedString) throws IOException, JSONException {
    Path jsonFile = tempDir.resolve("META-INF").resolve("native-image").resolve("reachability-metadata.json");
    String content = Files.readString(jsonFile);
    JSONAssert.assertEquals(expectedString, content, JSONCompareMode.NON_EXTENSIBLE);
  }

}

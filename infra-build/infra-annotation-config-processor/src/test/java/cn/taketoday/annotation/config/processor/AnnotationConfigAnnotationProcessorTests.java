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

package cn.taketoday.annotation.config.processor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link AnnotationConfigAnnotationProcessor}.
 *
 * @author Madhura Bhave
 * @author Moritz Halbritter
 * @author Scott Frederick
 */
class AnnotationConfigAnnotationProcessorTests {

  @Test
  void annotatedClass() {
    compile(TestClassConfiguration.class, (properties) -> {
      assertThat(properties).hasSize(7);
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestClassConfiguration.ConditionalOnClass",
              "cn.taketoday.foo,java.io.InputStream,cn.taketoday.annotation.config.processor."
                      + "TestClassConfiguration$Nested");
      assertThat(properties)
              .containsKey("cn.taketoday.annotation.config.processor.TestClassConfiguration");
      assertThat(properties)
              .containsKey("cn.taketoday.annotation.config.processor.TestClassConfiguration$Nested");
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestClassConfiguration.ConditionalOnBean",
              "java.io.OutputStream");
      assertThat(properties).containsEntry("cn.taketoday.annotation.config.processor."
              + "TestClassConfiguration.ConditionalOnSingleCandidate", "java.io.OutputStream");
      assertThat(properties).containsEntry("cn.taketoday.annotation.config.processor."
              + "TestClassConfiguration.ConditionalOnWebApplication", "MOCK");
    });
  }

  @Test
  void annotatedClassWithOnlyAutoConfiguration() {
    compile(TestAutoConfigurationOnlyConfiguration.class, (properties) -> {
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestAutoConfigurationOnlyConfiguration", "");
      assertThat(properties).doesNotContainEntry(
              "cn.taketoday.annotation.config.processor.TestAutoConfigurationOnlyConfiguration.AutoConfigureAfter",
              "");
      assertThat(properties).doesNotContainEntry(
              "cn.taketoday.annotation.config.processor.TestAutoConfigurationOnlyConfiguration.AutoConfigureBefore",
              "");
    });
  }

  @Test
  void annotatedClassWithOnBeanThatHasName() {
    compile(TestOnBeanWithNameClassConfiguration.class, (properties) -> {
      assertThat(properties).hasSize(2);
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestOnBeanWithNameClassConfiguration.ConditionalOnBean",
              "");
    });
  }

  @Test
  void annotatedMethod() {
    process(TestMethodConfiguration.class, (properties) -> assertThat(properties).isNull());
  }

  @Test
  void annotatedClassWithOrder() {
    compile(TestOrderedClassConfiguration.class, (properties) -> {
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestOrderedClassConfiguration.ConditionalOnClass",
              "java.io.InputStream,java.io.OutputStream");
      assertThat(properties).containsEntry("cn.taketoday.annotation.config.processor."
              + "TestOrderedClassConfiguration.AutoConfigureBefore", "test.before1,test.before2");
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestOrderedClassConfiguration.AutoConfigureAfter",
              "java.io.ObjectInputStream");
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestOrderedClassConfiguration.AutoConfigureOrder",
              "123");
    });

  }

  @Test
  void annotatedClassWithAutoConfiguration() {
    compile(TestAutoConfigurationConfiguration.class, (properties) -> {
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestAutoConfigurationConfiguration", "");
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestAutoConfigurationConfiguration.AutoConfigureBefore",
              "java.io.InputStream,test.before1,test.before2");
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestAutoConfigurationConfiguration.AutoConfigureAfter",
              "java.io.OutputStream,test.after1,test.after2");
    });
  }

  @Test
  void annotatedClassWithAutoConfigurationMerged() {
    compile(TestMergedAutoConfigurationConfiguration.class, (properties) -> {
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestMergedAutoConfigurationConfiguration", "");
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestMergedAutoConfigurationConfiguration.AutoConfigureBefore",
              "java.io.InputStream,test.before1,test.before2,java.io.ObjectInputStream,test.before3,test.before4");
      assertThat(properties).containsEntry(
              "cn.taketoday.annotation.config.processor.TestMergedAutoConfigurationConfiguration.AutoConfigureAfter",
              "java.io.OutputStream,test.after1,test.after2,java.io.ObjectOutputStream,test.after3,test.after4");
    });
  }

  @Test
    // gh-19370
  void propertiesAreFullRepeatable() {
    process(TestOrderedClassConfiguration.class, (firstFile) -> {
      String first = getFileContents(firstFile);
      process(TestOrderedClassConfiguration.class, (secondFile) -> {
        String second = getFileContents(secondFile);
        assertThat(first).isEqualTo(second).doesNotContain("#");
      });
    });
  }

  private void compile(Class<?> type, Consumer<Properties> consumer) {
    process(type, (writtenFile) -> consumer.accept(getWrittenProperties(writtenFile)));
  }

  private void process(Class<?> type, Consumer<InputStream> consumer) {
    TestAnnotationConfigAnnotationProcessor processor = new TestAnnotationConfigAnnotationProcessor();
    SourceFile sourceFile = SourceFile.forTestClass(type);
    TestCompiler compiler = TestCompiler.forSystem().withProcessors(processor).withSources(sourceFile);
    compiler.compile((compiled) -> {
      InputStream propertiesFile = compiled.getClassLoader()
              .getResourceAsStream(AnnotationConfigAnnotationProcessor.PROPERTIES_PATH);
      consumer.accept(propertiesFile);
    });
  }

  private Properties getWrittenProperties(InputStream inputStream) {
    try {
      Properties properties = new Properties();
      properties.load(inputStream);
      return properties;
    }
    catch (IOException ex) {
      fail("Error reading properties", ex);
    }
    return null;
  }

  private String getFileContents(InputStream inputStream) {
    try {
      return new String(inputStream.readAllBytes());
    }
    catch (IOException ex) {
      fail("Error reading contents of properties file", ex);
    }
    return null;
  }

}

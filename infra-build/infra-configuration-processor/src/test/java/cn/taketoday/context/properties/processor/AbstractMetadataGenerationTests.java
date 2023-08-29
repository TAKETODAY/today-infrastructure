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

package cn.taketoday.context.properties.processor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.test.CompiledMetadataReader;
import cn.taketoday.context.properties.processor.test.TestConfigurationMetadataAnnotationProcessor;
import cn.taketoday.core.test.tools.ResourceFile;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;

/**
 * Base test infrastructure for metadata generation tests.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
public abstract class AbstractMetadataGenerationTests {

  private static final String ADDITIONAL_METADATA_FILE = "META-INF/additional-infra-configuration-metadata.json";

  protected ConfigurationMetadata compile(Class<?>... types) {
    TestCompiler compiler = TestCompiler.forSystem().withSources(sourceFilesOf(types));
    return compile(compiler);
  }

  protected ConfigurationMetadata compile(String additionalMetadata, Class<?> type, Class<?>... types) {
    TestCompiler compiler = TestCompiler.forSystem()
            .withSources(sourceFilesOf(type))
            .withSources(sourceFilesOf(types))
            .withResources(ResourceFile.of(ADDITIONAL_METADATA_FILE, additionalMetadata));
    return compile(compiler);
  }

  protected ConfigurationMetadata compile(String... source) {
    TestCompiler compiler = TestCompiler.forSystem().withSources(sourceFilesOf(source));
    return compile(compiler);
  }

  private ConfigurationMetadata compile(TestCompiler compiler) {
    TestConfigurationMetadataAnnotationProcessor processor = new TestConfigurationMetadataAnnotationProcessor();
    compiler = compiler.withProcessors(processor);
    AtomicReference<ConfigurationMetadata> configurationMetadata = new AtomicReference<>();
    compiler.compile((compiled) -> configurationMetadata.set(CompiledMetadataReader.getMetadata(compiled)));
    return configurationMetadata.get();
  }

  private List<SourceFile> sourceFilesOf(Class<?>... types) {
    return Arrays.stream(types).map(SourceFile::forTestClass).toList();
  }

  private List<SourceFile> sourceFilesOf(String... content) {
    return Arrays.stream(content).map(SourceFile::of).toList();
  }

}

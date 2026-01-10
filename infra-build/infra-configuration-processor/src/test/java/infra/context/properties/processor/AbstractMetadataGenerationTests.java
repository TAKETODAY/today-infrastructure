/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.processor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import infra.context.properties.processor.metadata.ConfigurationMetadata;
import infra.context.properties.processor.test.CompiledMetadataReader;
import infra.context.properties.processor.test.TestConfigurationMetadataAnnotationProcessor;
import infra.core.test.tools.ResourceFile;
import infra.core.test.tools.SourceFile;
import infra.core.test.tools.TestCompiler;

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

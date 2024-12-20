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

package infra.aot.generate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import infra.aot.hint.RuntimeHints;
import infra.lang.Assert;

/**
 * Default {@link GenerationContext} implementation.
 *
 * <p>Generated classes can be flushed out using {@link #writeGeneratedContent()}
 * which should be called only once after the generation process using this instance
 * has completed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultGenerationContext implements GenerationContext {

  private final Map<String, AtomicInteger> sequenceGenerator;

  private final GeneratedClasses generatedClasses;

  private final GeneratedFiles generatedFiles;

  private final RuntimeHints runtimeHints;

  /**
   * Create a new {@link DefaultGenerationContext} instance backed by the
   * specified {@link ClassNameGenerator} and {@link GeneratedFiles}.
   *
   * @param classNameGenerator the naming convention to use for generated
   * class names
   * @param generatedFiles the generated files
   */
  public DefaultGenerationContext(ClassNameGenerator classNameGenerator, GeneratedFiles generatedFiles) {
    this(classNameGenerator, generatedFiles, new RuntimeHints());
  }

  /**
   * Create a new {@link DefaultGenerationContext} instance backed by the
   * specified {@link ClassNameGenerator}, {@link GeneratedFiles}, and
   * {@link RuntimeHints}.
   *
   * @param classNameGenerator the naming convention to use for generated
   * class names
   * @param generatedFiles the generated files
   * @param runtimeHints the runtime hints
   */
  public DefaultGenerationContext(ClassNameGenerator classNameGenerator, GeneratedFiles generatedFiles,
          RuntimeHints runtimeHints) {
    this(new GeneratedClasses(classNameGenerator), generatedFiles, runtimeHints);
  }

  /**
   * Create a new {@link DefaultGenerationContext} instance backed by the
   * specified items.
   *
   * @param generatedClasses the generated classes
   * @param generatedFiles the generated files
   * @param runtimeHints the runtime hints
   */
  protected DefaultGenerationContext(GeneratedClasses generatedClasses,
          GeneratedFiles generatedFiles, RuntimeHints runtimeHints) {

    Assert.notNull(generatedClasses, "'generatedClasses' is required");
    Assert.notNull(generatedFiles, "'generatedFiles' is required");
    Assert.notNull(runtimeHints, "'runtimeHints' is required");
    this.sequenceGenerator = new ConcurrentHashMap<>();
    this.generatedClasses = generatedClasses;
    this.generatedFiles = generatedFiles;
    this.runtimeHints = runtimeHints;
  }

  /**
   * Create a new {@link DefaultGenerationContext} instance based on the
   * supplied {@code existing} context and feature name.
   *
   * @param existing the existing context upon which to base the new one
   * @param featureName the feature name to use
   */
  protected DefaultGenerationContext(DefaultGenerationContext existing, String featureName) {
    int sequence = existing.sequenceGenerator.computeIfAbsent(featureName, key -> new AtomicInteger()).getAndIncrement();
    if (sequence > 0) {
      featureName += sequence;
    }
    this.sequenceGenerator = existing.sequenceGenerator;
    this.generatedClasses = existing.generatedClasses.withFeatureNamePrefix(featureName);
    this.generatedFiles = existing.generatedFiles;
    this.runtimeHints = existing.runtimeHints;
  }

  @Override
  public GeneratedClasses getGeneratedClasses() {
    return this.generatedClasses;
  }

  @Override
  public GeneratedFiles getGeneratedFiles() {
    return this.generatedFiles;
  }

  @Override
  public RuntimeHints getRuntimeHints() {
    return this.runtimeHints;
  }

  @Override
  public DefaultGenerationContext withName(String name) {
    return new DefaultGenerationContext(this, name);
  }

  /**
   * Write any generated content out to the generated files.
   */
  public void writeGeneratedContent() {
    this.generatedClasses.writeTo(this.generatedFiles);
  }

}

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

package cn.taketoday.test.context.aot;

import cn.taketoday.aot.generate.ClassNameGenerator;
import cn.taketoday.aot.generate.DefaultGenerationContext;
import cn.taketoday.aot.generate.GeneratedFiles;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link DefaultGenerationContext} with a custom implementation of
 * {@link #withName(String)} that is specific to the <em>Infra TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TestContextGenerationContext extends DefaultGenerationContext {

  @Nullable
  private final String featureName;

  /**
   * Create a new {@link TestContextGenerationContext} instance backed by the
   * specified {@link ClassNameGenerator}, {@link GeneratedFiles}, and
   * {@link RuntimeHints}.
   *
   * @param classNameGenerator the naming convention to use for generated class names
   * @param generatedFiles the generated files
   * @param runtimeHints the runtime hints
   */
  TestContextGenerationContext(ClassNameGenerator classNameGenerator, GeneratedFiles generatedFiles,
          RuntimeHints runtimeHints) {
    super(classNameGenerator, generatedFiles, runtimeHints);
    this.featureName = null;
  }

  /**
   * Create a new {@link TestContextGenerationContext} instance based on the
   * supplied {@code existing} context and feature name.
   *
   * @param existing the existing context upon which to base the new one
   * @param featureName the feature name to use
   */
  private TestContextGenerationContext(TestContextGenerationContext existing, String featureName) {
    super(existing, featureName);
    this.featureName = featureName;
  }

  /**
   * Create a new {@link TestContextGenerationContext} instance using the specified
   * feature name to qualify generated assets for a dedicated round of code generation.
   * <p>If <em>this</em> {@code TestContextGenerationContext} has a configured feature
   * name, the existing feature name will prepended to the supplied feature name in
   * order to avoid naming collisions.
   *
   * @param featureName the feature name to use
   * @return a specialized {@link TestContextGenerationContext} for the specified
   * feature name
   */
  @Override
  public TestContextGenerationContext withName(String featureName) {
    if (this.featureName != null) {
      featureName = this.featureName + featureName;
    }
    return new TestContextGenerationContext(this, featureName);
  }

}

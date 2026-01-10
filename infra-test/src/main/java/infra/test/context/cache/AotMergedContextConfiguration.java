/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.cache;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.Collections;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.core.style.DefaultToStringStyler;
import infra.core.style.DefaultValueStyler;
import infra.core.style.ToStringBuilder;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.MergedContextConfiguration;

/**
 * {@link MergedContextConfiguration} implementation based on an AOT-generated
 * {@link ApplicationContextInitializer} that is used to load an AOT-optimized
 * {@link ApplicationContext ApplicationContext}.
 *
 * <p>An {@code ApplicationContext} should not be loaded using the metadata in
 * this {@code AotMergedContextConfiguration}. Rather the metadata from the
 * {@linkplain #getOriginal() original} {@code MergedContextConfiguration} must
 * be used.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class AotMergedContextConfiguration extends MergedContextConfiguration {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Class<? extends ApplicationContextInitializer> contextInitializerClass;

  private final MergedContextConfiguration original;

  AotMergedContextConfiguration(Class<?> testClass, Class<? extends ApplicationContextInitializer> contextInitializerClass,
          MergedContextConfiguration original, CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

    super(testClass, null, null, Collections.singleton(contextInitializerClass), null,
            original.getContextLoader(), cacheAwareContextLoaderDelegate, original.getParent());
    this.contextInitializerClass = contextInitializerClass;
    this.original = original;
  }

  /**
   * Get the original {@link MergedContextConfiguration} that this
   * {@code AotMergedContextConfiguration} was created for.
   */
  MergedContextConfiguration getOriginal() {
    return this.original;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    return ((other instanceof AotMergedContextConfiguration that) &&
            this.contextInitializerClass.equals(that.contextInitializerClass));
  }

  @Override
  public int hashCode() {
    return this.contextInitializerClass.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, new DefaultToStringStyler(new DefaultValueStyler()))
            .append("testClass", getTestClass())
            .append("contextInitializerClass", this.contextInitializerClass)
            .append("original", this.original)
            .toString();
  }

}

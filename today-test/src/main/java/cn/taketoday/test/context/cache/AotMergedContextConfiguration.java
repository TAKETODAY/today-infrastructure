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

package cn.taketoday.test.context.cache;

import java.io.Serial;
import java.util.Collections;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.core.style.DefaultToStringStyler;
import cn.taketoday.core.style.DefaultValueStyler;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.MergedContextConfiguration;

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

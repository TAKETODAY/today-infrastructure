/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.weaving;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 16:30
 */
class DefaultContextLoadTimeWeaverTests {

  @Test
  void constructorWithClassLoader() {
    ClassLoader mockLoader = mock(ClassLoader.class);
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver(mockLoader);
    assertThat(weaver).isNotNull();
  }

  @Test
  void addTransformerBeforeInitializationThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();
    ClassFileTransformer transformer = mock(ClassFileTransformer.class);

    assertThatIllegalStateException()
            .isThrownBy(() -> weaver.addTransformer(transformer))
            .withMessage("Not initialized");
  }

  @Test
  void getInstrumentableClassLoaderBeforeInitializationThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();

    assertThatIllegalStateException()
            .isThrownBy(weaver::getInstrumentableClassLoader)
            .withMessage("Not initialized");
  }

  @Test
  void getThrowawayClassLoaderBeforeInitializationThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();

    assertThatIllegalStateException()
            .isThrownBy(weaver::getThrowawayClassLoader)
            .withMessage("Not initialized");
  }

  @Test
  void nullClassLoaderThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();
    assertThatThrownBy(() -> weaver.setBeanClassLoader(null))
            .isInstanceOf(NullPointerException.class);
  }

}
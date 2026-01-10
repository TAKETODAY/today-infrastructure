/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.context.weaving;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 16:30
 */
class DefaultContextLoadTimeWeaverTests {

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
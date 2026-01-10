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

package infra.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;

import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/14 14:19
 */
class SimpleLoadTimeWeaverTests {

  @Test
  void simpleLoadTimeWeaver() {
    SimpleLoadTimeWeaver weaver = new SimpleLoadTimeWeaver();
    assertThat(weaver.getInstrumentableClassLoader()).isInstanceOf(SimpleInstrumentableClassLoader.class);

    SimpleInstrumentableClassLoader classLoader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
    weaver = new SimpleLoadTimeWeaver(classLoader);
    assertThat(weaver.getInstrumentableClassLoader()).isInstanceOf(SimpleInstrumentableClassLoader.class);

    ClassFileTransformer0 transformer = new ClassFileTransformer0();
    weaver.addTransformer(transformer);
    assertThat(classLoader).extracting("weavingTransformer.transformers")
            .asList().containsExactly(transformer);

    assertThat(weaver.getThrowawayClassLoader()).isInstanceOf(SimpleThrowawayClassLoader.class);
  }

  static class ClassFileTransformer0 implements ClassFileTransformer {

  }

}
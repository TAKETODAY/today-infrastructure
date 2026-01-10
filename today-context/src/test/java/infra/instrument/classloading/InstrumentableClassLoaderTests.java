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

package infra.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class InstrumentableClassLoaderTests {

  @Test
  public void testDefaultLoadTimeWeaver() {
    ClassLoader loader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
    ReflectiveLoadTimeWeaver handler = new ReflectiveLoadTimeWeaver(loader);
    handler.addTransformer(new ClassFileTransformer() {
      @Override
      public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return ClassFileTransformer.super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
      }
    });
    assertThat(handler.getInstrumentableClassLoader()).isSameAs(loader);
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    Thread.currentThread().setContextClassLoader(loader);
    handler = new ReflectiveLoadTimeWeaver();
    assertThat(handler.getInstrumentableClassLoader()).isSameAs(ClassUtils.getDefaultClassLoader());

    // reset
    Thread.currentThread().setContextClassLoader(contextClassLoader);
  }

}

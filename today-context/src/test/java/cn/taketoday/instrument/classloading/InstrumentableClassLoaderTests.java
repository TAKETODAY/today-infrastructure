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

package cn.taketoday.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import cn.taketoday.util.ClassUtils;

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

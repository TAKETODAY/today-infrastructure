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
import java.security.ProtectionDomain;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/13 18:20
 */
class ReflectiveLoadTimeWeaverTests {

  @Test
  public void testCtorWithNullClassLoader() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ReflectiveLoadTimeWeaver(null));
  }

  @Test
  public void testCtorWithClassLoaderThatDoesNotExposeAnAddTransformerMethod() {
    assertThatIllegalStateException().isThrownBy(() ->
            new ReflectiveLoadTimeWeaver(getClass().getClassLoader()));
  }

  @Test
  public void testCtorWithClassLoaderThatDoesNotExposeAGetThrowawayClassLoaderMethodIsOkay() {
    JustAddTransformerClassLoader classLoader = new JustAddTransformerClassLoader();
    ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(classLoader);
    weaver.addTransformer(new ClassFileTransformer() {
      @Override
      public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return "CAFEDEAD".getBytes();
      }
    });
    assertThat(classLoader.getNumTimesGetThrowawayClassLoaderCalled()).isEqualTo(1);
  }

  @Test
  public void testAddTransformerWithNullTransformer() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ReflectiveLoadTimeWeaver(new JustAddTransformerClassLoader()).addTransformer(null));
  }

  @Test
  public void testGetThrowawayClassLoaderWithClassLoaderThatDoesNotExposeAGetThrowawayClassLoaderMethodYieldsFallbackClassLoader() {
    ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(new JustAddTransformerClassLoader());
    ClassLoader throwawayClassLoader = weaver.getThrowawayClassLoader();
    assertThat(throwawayClassLoader).isNotNull();
  }

  @Test
  public void testGetThrowawayClassLoaderWithTotallyCompliantClassLoader() {
    TotallyCompliantClassLoader classLoader = new TotallyCompliantClassLoader();
    ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(classLoader);
    ClassLoader throwawayClassLoader = weaver.getThrowawayClassLoader();
    assertThat(throwawayClassLoader).isNotNull();
    assertThat(classLoader.getNumTimesGetThrowawayClassLoaderCalled()).isEqualTo(1);
  }


  public static class JustAddTransformerClassLoader extends ClassLoader {

    private int numTimesAddTransformerCalled = 0;

    public int getNumTimesGetThrowawayClassLoaderCalled() {
      return this.numTimesAddTransformerCalled;
    }

    public void addTransformer(ClassFileTransformer transformer) {
      ++this.numTimesAddTransformerCalled;
    }
  }


  public static final class TotallyCompliantClassLoader extends JustAddTransformerClassLoader {

    private int numTimesGetThrowawayClassLoaderCalled = 0;

    @Override
    public int getNumTimesGetThrowawayClassLoaderCalled() {
      return this.numTimesGetThrowawayClassLoaderCalled;
    }

    public ClassLoader getThrowawayClassLoader() {
      ++this.numTimesGetThrowawayClassLoaderCalled;
      return getClass().getClassLoader();
    }
  }

}
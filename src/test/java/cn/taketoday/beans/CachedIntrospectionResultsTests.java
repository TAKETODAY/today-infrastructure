/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.OverridingClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Arjen Poutsma
 */
public class CachedIntrospectionResultsTests {

  @Test
  public void acceptAndClearClassLoader() throws Exception {
    BeanWrapper bw = new BeanWrapperImpl(TestBean.class);
    assertThat(bw.isWritableProperty("name")).isTrue();
    assertThat(bw.isWritableProperty("age")).isTrue();
    CachedIntrospectionResults.forClass(TestBean.class);
    // BeanMetadata use CachedIntrospectionResults to collect bean properties
    assertThat(CachedIntrospectionResults.strongClassCache.containsKey(TestBean.class)).isTrue();

    ClassLoader child = new OverridingClassLoader(getClass().getClassLoader());
    Class<?> tbClass = child.loadClass("cn.taketoday.beans.testfixture.beans.TestBean");
    assertThat(CachedIntrospectionResults.strongClassCache.containsKey(tbClass)).isFalse();
    CachedIntrospectionResults.acceptClassLoader(child);

    CachedIntrospectionResults.forClass(tbClass);

    assertThat(CachedIntrospectionResults.strongClassCache.containsKey(tbClass)).isTrue();
    CachedIntrospectionResults.clearClassLoader(child);
    assertThat(CachedIntrospectionResults.strongClassCache.containsKey(tbClass)).isFalse();

    assertThat(CachedIntrospectionResults.strongClassCache.containsKey(TestBean.class)).isTrue();
  }

  @Test
  public void clearClassLoaderForSystemClassLoader() throws Exception {
    BeanUtils.getPropertyDescriptors(ArrayList.class);
    assertThat(CachedIntrospectionResults.strongClassCache.containsKey(ArrayList.class)).isTrue();
    CachedIntrospectionResults.clearClassLoader(ArrayList.class.getClassLoader());
    assertThat(CachedIntrospectionResults.strongClassCache.containsKey(ArrayList.class)).isFalse();
  }

  @Test
  public void shouldUseExtendedBeanInfoWhenApplicable() throws NoSuchMethodException, SecurityException {
    // given a class with a non-void returning setter method
    @SuppressWarnings("unused")
    class C {
      public Object setFoo(String s) { return this; }

      public String getFoo() { return null; }
    }

    // CachedIntrospectionResults should delegate to ExtendedBeanInfo
    CachedIntrospectionResults results = CachedIntrospectionResults.forClass(C.class);
    BeanInfo info = results.getBeanInfo();
    PropertyDescriptor pd = null;
    for (PropertyDescriptor candidate : info.getPropertyDescriptors()) {
      if (candidate.getName().equals("foo")) {
        pd = candidate;
      }
    }

    // resulting in a property descriptor including the non-standard setFoo method
    assertThat(pd).isNotNull();
    assertThat(pd.getReadMethod()).isEqualTo(C.class.getMethod("getFoo"));
    // No write method found for non-void returning 'setFoo' method.
    // Check to see if CachedIntrospectionResults is delegating to ExtendedBeanInfo as expected
    assertThat(pd.getWriteMethod()).isEqualTo(C.class.getMethod("setFoo", String.class));
  }

}

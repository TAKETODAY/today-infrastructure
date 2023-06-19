/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jndi;

import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.NamingException;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.DerivedTestBean;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.testfixture.jndi.ExpectedLookupTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class JndiObjectFactoryBeanTests {

  @Test
  public void testNoJndiName() throws NamingException {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    assertThatIllegalArgumentException().isThrownBy(jof::afterPropertiesSet);
  }

  @Test
  public void testLookupWithFullNameAndResourceRefTrue() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    Object o = new Object();
    jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
    jof.setJndiName("java:comp/env/foo");
    jof.setResourceRef(true);
    jof.afterPropertiesSet();
    assertThat(jof.getObject() == o).isTrue();
  }

  @Test
  public void testLookupWithFullNameAndResourceRefFalse() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    Object o = new Object();
    jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
    jof.setJndiName("java:comp/env/foo");
    jof.setResourceRef(false);
    jof.afterPropertiesSet();
    assertThat(jof.getObject() == o).isTrue();
  }

  @Test
  public void testLookupWithSchemeNameAndResourceRefTrue() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    Object o = new Object();
    jof.setJndiTemplate(new ExpectedLookupTemplate("java:foo", o));
    jof.setJndiName("java:foo");
    jof.setResourceRef(true);
    jof.afterPropertiesSet();
    assertThat(jof.getObject() == o).isTrue();
  }

  @Test
  public void testLookupWithSchemeNameAndResourceRefFalse() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    Object o = new Object();
    jof.setJndiTemplate(new ExpectedLookupTemplate("java:foo", o));
    jof.setJndiName("java:foo");
    jof.setResourceRef(false);
    jof.afterPropertiesSet();
    assertThat(jof.getObject() == o).isTrue();
  }

  @Test
  public void testLookupWithShortNameAndResourceRefTrue() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    Object o = new Object();
    jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
    jof.setJndiName("foo");
    jof.setResourceRef(true);
    jof.afterPropertiesSet();
    assertThat(jof.getObject() == o).isTrue();
  }

  @Test
  public void testLookupWithShortNameAndResourceRefFalse() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    Object o = new Object();
    jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
    jof.setJndiName("foo");
    jof.setResourceRef(false);
    assertThatExceptionOfType(NamingException.class).isThrownBy(jof::afterPropertiesSet);
  }

  @Test
  public void testLookupWithArbitraryNameAndResourceRefFalse() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    Object o = new Object();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", o));
    jof.setJndiName("foo");
    jof.setResourceRef(false);
    jof.afterPropertiesSet();
    assertThat(jof.getObject() == o).isTrue();
  }

  @Test
  public void testLookupWithExpectedTypeAndMatch() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    String s = "";
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", s));
    jof.setJndiName("foo");
    jof.setExpectedType(String.class);
    jof.afterPropertiesSet();
    assertThat(jof.getObject() == s).isTrue();
  }

  @Test
  public void testLookupWithExpectedTypeAndNoMatch() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", new Object()));
    jof.setJndiName("foo");
    jof.setExpectedType(String.class);
    assertThatExceptionOfType(NamingException.class).isThrownBy(
                    jof::afterPropertiesSet)
            .withMessageContaining("java.lang.String");
  }

  @Test
  public void testLookupWithDefaultObject() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", ""));
    jof.setJndiName("myFoo");
    jof.setExpectedType(String.class);
    jof.setDefaultObject("myString");
    jof.afterPropertiesSet();
    assertThat(jof.getObject()).isEqualTo("myString");
  }

  @Test
  public void testLookupWithDefaultObjectAndExpectedType() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", ""));
    jof.setJndiName("myFoo");
    jof.setExpectedType(String.class);
    jof.setDefaultObject("myString");
    jof.afterPropertiesSet();
    assertThat(jof.getObject()).isEqualTo("myString");
  }

  @Test
  public void testLookupWithDefaultObjectAndExpectedTypeConversion() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", ""));
    jof.setJndiName("myFoo");
    jof.setExpectedType(Integer.class);
    jof.setDefaultObject("5");
    jof.afterPropertiesSet();
    assertThat(jof.getObject()).isEqualTo(5);
  }

  @Test
  public void testLookupWithDefaultObjectAndExpectedTypeConversionViaBeanFactory() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", ""));
    jof.setJndiName("myFoo");
    jof.setExpectedType(Integer.class);
    jof.setDefaultObject("5");
    jof.setBeanFactory(new StandardBeanFactory());
    jof.afterPropertiesSet();
    assertThat(jof.getObject()).isEqualTo(5);
  }

  @Test
  public void testLookupWithDefaultObjectAndExpectedTypeNoMatch() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", ""));
    jof.setJndiName("myFoo");
    jof.setExpectedType(Boolean.class);
    jof.setDefaultObject("5");
    assertThatIllegalArgumentException().isThrownBy(jof::afterPropertiesSet);
  }

  @Test
  public void testLookupWithProxyInterface() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    TestBean tb = new TestBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
    jof.setJndiName("foo");
    jof.setProxyInterface(ITestBean.class);
    jof.afterPropertiesSet();
    boolean condition = jof.getObject() instanceof ITestBean;
    assertThat(condition).isTrue();
    ITestBean proxy = (ITestBean) jof.getObject();
    assertThat(tb.getAge()).isEqualTo(0);
    proxy.setAge(99);
    assertThat(tb.getAge()).isEqualTo(99);
  }

  @Test
  public void testLookupWithProxyInterfaceAndDefaultObject() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    TestBean tb = new TestBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
    jof.setJndiName("myFoo");
    jof.setProxyInterface(ITestBean.class);
    jof.setDefaultObject(Boolean.TRUE);
    assertThatIllegalArgumentException().isThrownBy(jof::afterPropertiesSet);
  }

  @Test
  public void testLookupWithProxyInterfaceAndLazyLookup() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    final TestBean tb = new TestBean();
    jof.setJndiTemplate(new JndiTemplate() {
      @Override
      public Object lookup(String name) {
        if ("foo".equals(name)) {
          tb.setName("tb");
          return tb;
        }
        return null;
      }
    });
    jof.setJndiName("foo");
    jof.setProxyInterface(ITestBean.class);
    jof.setLookupOnStartup(false);
    jof.afterPropertiesSet();
    boolean condition = jof.getObject() instanceof ITestBean;
    assertThat(condition).isTrue();
    ITestBean proxy = (ITestBean) jof.getObject();
    assertThat(tb.getName()).isNull();
    assertThat(tb.getAge()).isEqualTo(0);
    proxy.setAge(99);
    assertThat(tb.getName()).isEqualTo("tb");
    assertThat(tb.getAge()).isEqualTo(99);
  }

  @Test
  public void testLookupWithProxyInterfaceWithNotCache() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    final TestBean tb = new TestBean();
    jof.setJndiTemplate(new JndiTemplate() {
      @Override
      public Object lookup(String name) {
        if ("foo".equals(name)) {
          tb.setName("tb");
          tb.setAge(tb.getAge() + 1);
          return tb;
        }
        return null;
      }
    });
    jof.setJndiName("foo");
    jof.setProxyInterface(ITestBean.class);
    jof.setCache(false);
    jof.afterPropertiesSet();
    boolean condition = jof.getObject() instanceof ITestBean;
    assertThat(condition).isTrue();
    ITestBean proxy = (ITestBean) jof.getObject();
    assertThat(tb.getName()).isEqualTo("tb");
    assertThat(tb.getAge()).isEqualTo(1);
    proxy.returnsThis();
    assertThat(tb.getAge()).isEqualTo(2);
    proxy.haveBirthday();
    assertThat(tb.getAge()).isEqualTo(4);
  }

  @Test
  public void testLookupWithProxyInterfaceWithLazyLookupAndNotCache() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    final TestBean tb = new TestBean();
    jof.setJndiTemplate(new JndiTemplate() {
      @Override
      public Object lookup(String name) {
        if ("foo".equals(name)) {
          tb.setName("tb");
          tb.setAge(tb.getAge() + 1);
          return tb;
        }
        return null;
      }
    });
    jof.setJndiName("foo");
    jof.setProxyInterface(ITestBean.class);
    jof.setLookupOnStartup(false);
    jof.setCache(false);
    jof.afterPropertiesSet();
    boolean condition = jof.getObject() instanceof ITestBean;
    assertThat(condition).isTrue();
    ITestBean proxy = (ITestBean) jof.getObject();
    assertThat(tb.getName()).isNull();
    assertThat(tb.getAge()).isEqualTo(0);
    proxy.returnsThis();
    assertThat(tb.getName()).isEqualTo("tb");
    assertThat(tb.getAge()).isEqualTo(1);
    proxy.returnsThis();
    assertThat(tb.getAge()).isEqualTo(2);
    proxy.haveBirthday();
    assertThat(tb.getAge()).isEqualTo(4);
  }

  @Test
  public void testLazyLookupWithoutProxyInterface() throws NamingException {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiName("foo");
    jof.setLookupOnStartup(false);
    assertThatIllegalStateException().isThrownBy(jof::afterPropertiesSet);
  }

  @Test
  public void testNotCacheWithoutProxyInterface() throws NamingException {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    jof.setJndiName("foo");
    jof.setCache(false);
    jof.setLookupOnStartup(false);
    assertThatIllegalStateException().isThrownBy(jof::afterPropertiesSet);
  }

  @Test
  public void testLookupWithProxyInterfaceAndExpectedTypeAndMatch() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    TestBean tb = new TestBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
    jof.setJndiName("foo");
    jof.setExpectedType(TestBean.class);
    jof.setProxyInterface(ITestBean.class);
    jof.afterPropertiesSet();
    boolean condition = jof.getObject() instanceof ITestBean;
    assertThat(condition).isTrue();
    ITestBean proxy = (ITestBean) jof.getObject();
    assertThat(tb.getAge()).isEqualTo(0);
    proxy.setAge(99);
    assertThat(tb.getAge()).isEqualTo(99);
  }

  @Test
  public void testLookupWithProxyInterfaceAndExpectedTypeAndNoMatch() {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    TestBean tb = new TestBean();
    jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
    jof.setJndiName("foo");
    jof.setExpectedType(DerivedTestBean.class);
    jof.setProxyInterface(ITestBean.class);
    assertThatExceptionOfType(NamingException.class)
            .isThrownBy(jof::afterPropertiesSet)
            .withMessageContaining("cn.taketoday.beans.testfixture.beans.DerivedTestBean");
  }

  @Test
  public void testLookupWithExposeAccessContext() throws Exception {
    JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
    TestBean tb = new TestBean();
    final Context mockCtx = mock(Context.class);
    given(mockCtx.lookup("foo")).willReturn(tb);
    jof.setJndiTemplate(new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return mockCtx;
      }
    });
    jof.setJndiName("foo");
    jof.setProxyInterface(ITestBean.class);
    jof.setExposeAccessContext(true);
    jof.afterPropertiesSet();
    boolean condition = jof.getObject() instanceof ITestBean;
    assertThat(condition).isTrue();
    ITestBean proxy = (ITestBean) jof.getObject();
    assertThat(tb.getAge()).isEqualTo(0);
    proxy.setAge(99);
    assertThat(tb.getAge()).isEqualTo(99);
    proxy.equals(proxy);
    proxy.hashCode();
    proxy.toString();
    verify(mockCtx, times(2)).close();
  }

}

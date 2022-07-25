/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ComponentScanParserScopedProxyTests {
  /*
  {

  @Test
  public void testDefaultScopedProxy() {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "cn/taketoday/context/annotation/scopedProxyDefaultTests.xml");
    context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

    ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();
    context.close();
  }

  @Test
  public void testNoScopedProxy() {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "cn/taketoday/context/annotation/scopedProxyNoTests.xml");
    context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

    ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
    // should not be a proxy
    assertThat(AopUtils.isAopProxy(bean)).isFalse();
    context.close();
  }

  @Test
  public void testInterfacesScopedProxy() throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "cn/taketoday/context/annotation/scopedProxyInterfacesTests.xml");
    context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

    // should cast to the interface
    FooService bean = (FooService) context.getBean("scopedProxyTestBean");
    // should be dynamic proxy
    assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
    // test serializability
    assertThat(bean.foo(1)).isEqualTo("bar");
    FooService deserialized = SerializationTestUtils.serializeAndDeserialize(bean);
    assertThat(deserialized).isNotNull();
    assertThat(deserialized.foo(1)).isEqualTo("bar");
    context.close();
  }

  @Test
  public void testTargetClassScopedProxy() throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            "cn/taketoday/context/annotation/scopedProxyTargetClassTests.xml");
    context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

    ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
    // should be a class-based proxy
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();
    // test serializability
    assertThat(bean.foo(1)).isEqualTo("bar");
    ScopedProxyTestBean deserialized = SerializationTestUtils.serializeAndDeserialize(bean);
    assertThat(deserialized).isNotNull();
    assertThat(deserialized.foo(1)).isEqualTo("bar");
    context.close();
  }

  @Test
  @SuppressWarnings("resource")
  public void testInvalidConfigScopedProxy() throws Exception {
    assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
                    new ClassPathXmlApplicationContext("cn/taketoday/context/annotation/scopedProxyInvalidConfigTests.xml"))
            .withMessageContaining("Cannot define both 'scope-resolver' and 'scoped-proxy' on <component-scan> tag")
            .withMessageContaining("Offending resource: class path resource [cn/taketoday/context/annotation/scopedProxyInvalidConfigTests.xml]");
  }

}
  * */
}

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

package infra.beans.factory.xml.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.aop.Advisor;
import infra.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator;
import infra.aop.framework.Advised;
import infra.aop.interceptor.DebugInterceptor;
import infra.aop.support.AopUtils;
import infra.aop.testfixture.interceptor.NopInterceptor;
import infra.beans.BeanInstantiationException;
import infra.beans.PropertyValues;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import infra.beans.factory.xml.BeanDefinitionDecorator;
import infra.beans.factory.xml.BeanDefinitionParser;
import infra.beans.factory.xml.DefaultNamespaceHandlerResolver;
import infra.beans.factory.xml.NamespaceHandlerResolver;
import infra.beans.factory.xml.NamespaceHandlerSupport;
import infra.beans.factory.xml.ParserContext;
import infra.beans.factory.xml.PluggableSchemaResolver;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.ApplicationListener;
import infra.context.support.GenericApplicationContext;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for custom XML namespace handler implementations.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class CustomNamespaceHandlerTests {

  private static final Class<?> CLASS = CustomNamespaceHandlerTests.class;
  private static final String CLASSNAME = CLASS.getSimpleName();
  private static final String FQ_PATH = "infra/beans/factory/xml/support";

  private static final String NS_PROPS = format("%s/%s.properties", FQ_PATH, CLASSNAME);
  private static final String NS_XML = format("%s/%s-context.xml", FQ_PATH, CLASSNAME);
  private static final String TEST_XSD = format("%s/%s.xsd", FQ_PATH, CLASSNAME);

  private GenericApplicationContext beanFactory;

  @BeforeEach
  void setUp() {
    NamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver(CLASS.getClassLoader(), NS_PROPS);
    this.beanFactory = new GenericApplicationContext();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.setNamespaceHandlerResolver(resolver);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
    reader.setEntityResolver(new DummySchemaResolver());
    reader.loadBeanDefinitions(getResource());
    this.beanFactory.refresh();
  }

  @Test
  void testSimpleParser() {
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    assertTestBean(bean);
  }

  @Test
  void testSimpleDecorator() {
    TestBean bean = (TestBean) this.beanFactory.getBean("customisedTestBean");
    assertTestBean(bean);
  }

  @Test
  void testProxyingDecorator() {
    ITestBean bean = (ITestBean) this.beanFactory.getBean("debuggingTestBean");
    assertTestBean(bean);
    assertThat(AopUtils.isAopProxy(bean)).isTrue();
    Advisor[] advisors = ((Advised) bean).getAdvisors();
    assertThat(advisors).as("Incorrect number of advisors").hasSize(1);
    assertThat(advisors[0].getAdvice().getClass()).as("Incorrect advice class").isEqualTo(DebugInterceptor.class);
  }

  @Test
  void testProxyingDecoratorNoInstance() {
    String[] beanNames = this.beanFactory.getBeanNamesForType(ApplicationListener.class);
    assertThat(Arrays.asList(beanNames)).contains("debuggingTestBeanNoInstance");
    assertThat(this.beanFactory.getType("debuggingTestBeanNoInstance")).isEqualTo(ApplicationListener.class);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    this.beanFactory.getBean("debuggingTestBeanNoInstance"))
            .havingRootCause()
            .isInstanceOf(BeanInstantiationException.class);
  }

  @Test
  void testChainedDecorators() {
    ITestBean bean = (ITestBean) this.beanFactory.getBean("chainedTestBean");
    assertTestBean(bean);
    assertThat(AopUtils.isAopProxy(bean)).isTrue();
    Advisor[] advisors = ((Advised) bean).getAdvisors();
    assertThat(advisors).as("Incorrect number of advisors").hasSize(2);
    assertThat(advisors[0].getAdvice().getClass()).as("Incorrect advice class").isEqualTo(DebugInterceptor.class);
    assertThat(advisors[1].getAdvice().getClass()).as("Incorrect advice class").isEqualTo(NopInterceptor.class);
  }

  @Test
  void testDecorationViaAttribute() {
    BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("decorateWithAttribute");
    assertThat(beanDefinition.getAttribute("objectName")).isEqualTo("foo");
  }

  @Test  //
  public void testCustomElementNestedWithinUtilList() {
    List<?> things = (List<?>) this.beanFactory.getBean("list.of.things");
    assertThat(things).isNotNull();
    assertThat(things).hasSize(2);
  }

  @Test  //
  public void testCustomElementNestedWithinUtilSet() {
    Set<?> things = (Set<?>) this.beanFactory.getBean("set.of.things");
    assertThat(things).isNotNull();
    assertThat(things).hasSize(2);
  }

  @Test  //
  public void testCustomElementNestedWithinUtilMap() {
    Map<?, ?> things = (Map<?, ?>) this.beanFactory.getBean("map.of.things");
    assertThat(things).isNotNull();
    assertThat(things).hasSize(2);
  }

  private void assertTestBean(ITestBean bean) {
    assertThat(bean.getName()).as("Invalid name").isEqualTo("Rob Harrop");
    assertThat(bean.getAge()).as("Invalid age").isEqualTo(23);
  }

  private Resource getResource() {
    return new ClassPathResource(NS_XML);
  }

  private static final class DummySchemaResolver extends PluggableSchemaResolver {

    public DummySchemaResolver() {
      super(CLASS.getClassLoader());
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException {
      InputSource source = super.resolveEntity(publicId, systemId);
      if (source == null) {
        Resource resource = new ClassPathResource(TEST_XSD);
        source = new InputSource(resource.getInputStream());
        source.setPublicId(publicId);
        source.setSystemId(systemId);
      }
      return source;
    }
  }

}

/**
 * Custom namespace handler implementation.
 *
 * @author Rob Harrop
 */
final class TestNamespaceHandler extends NamespaceHandlerSupport {

  @Override
  public void init() {
    registerBeanDefinitionParser("testBean", new TestBeanDefinitionParser());
    registerBeanDefinitionParser("person", new PersonDefinitionParser());

    registerBeanDefinitionDecorator("set", new PropertyModifyingBeanDefinitionDecorator());
    registerBeanDefinitionDecorator("debug", new DebugBeanDefinitionDecorator());
    registerBeanDefinitionDecorator("nop", new NopInterceptorBeanDefinitionDecorator());
    registerBeanDefinitionDecoratorForAttribute("object-name", new ObjectNameBeanDefinitionDecorator());
  }

  private static class TestBeanDefinitionParser implements BeanDefinitionParser {

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
      RootBeanDefinition definition = new RootBeanDefinition();
      definition.setBeanClass(TestBean.class);

      PropertyValues mpvs = new PropertyValues();
      mpvs.add("name", element.getAttribute("name"));
      mpvs.add("age", element.getAttribute("age"));
      definition.setPropertyValues(mpvs);

      parserContext.getRegistry().registerBeanDefinition(element.getAttribute("id"), definition);
      return null;
    }
  }

  private static final class PersonDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class<?> getBeanClass(Element element) {
      return TestBean.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
      builder.addPropertyValue("name", element.getAttribute("name"));
      builder.addPropertyValue("age", element.getAttribute("age"));
    }
  }

  private static class PropertyModifyingBeanDefinitionDecorator implements BeanDefinitionDecorator {

    @Override
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
      Element element = (Element) node;
      BeanDefinition def = definition.getBeanDefinition();

      PropertyValues mpvs = (def.getPropertyValues() == null ? new PropertyValues() : def.getPropertyValues());
      mpvs.add("name", element.getAttribute("name"));
      mpvs.add("age", element.getAttribute("age"));

      ((AbstractBeanDefinition) def).setPropertyValues(mpvs);
      return definition;
    }
  }

  private static class DebugBeanDefinitionDecorator extends AbstractInterceptorDrivenBeanDefinitionDecorator {

    @Override
    protected BeanDefinition createInterceptorDefinition(Node node) {
      return new RootBeanDefinition(DebugInterceptor.class);
    }
  }

  private static class NopInterceptorBeanDefinitionDecorator extends AbstractInterceptorDrivenBeanDefinitionDecorator {

    @Override
    protected BeanDefinition createInterceptorDefinition(Node node) {
      return new RootBeanDefinition(NopInterceptor.class);
    }
  }

  private static class ObjectNameBeanDefinitionDecorator implements BeanDefinitionDecorator {

    @Override
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
      Attr objectNameAttribute = (Attr) node;
      definition.getBeanDefinition().setAttribute("objectName", objectNameAttribute.getValue());
      return definition;
    }
  }

}

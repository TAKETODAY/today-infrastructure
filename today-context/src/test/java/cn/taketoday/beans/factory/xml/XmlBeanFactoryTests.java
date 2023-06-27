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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanClassLoadFailedException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanIsAbstractException;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.MethodReplacer;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.DependenciesBean;
import cn.taketoday.beans.testfixture.beans.DerivedTestBean;
import cn.taketoday.beans.testfixture.beans.FactoryMethods;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.IndexedTestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.factory.DummyFactory;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.tests.sample.beans.ResourceTestBean;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Miscellaneous tests for XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/6 22:35
 */
@Order(Integer.MAX_VALUE - 10)
class XmlBeanFactoryTests {

  private static final Class<?> CLASS = XmlBeanFactoryTests.class;
  private static final String CLASSNAME = CLASS.getSimpleName();

  private static final ClassPathResource AUTOWIRE_CONTEXT = classPathResource("-autowire.xml");
  private static final ClassPathResource CHILD_CONTEXT = classPathResource("-child.xml");
  private static final ClassPathResource CLASS_NOT_FOUND_CONTEXT = classPathResource("-classNotFound.xml");
  private static final ClassPathResource COMPLEX_FACTORY_CIRCLE_CONTEXT = classPathResource("-complexFactoryCircle.xml");
  private static final ClassPathResource CONSTRUCTOR_ARG_CONTEXT = classPathResource("-constructorArg.xml");
  private static final ClassPathResource CONSTRUCTOR_OVERRIDES_CONTEXT = classPathResource("-constructorOverrides.xml");
  private static final ClassPathResource DELEGATION_OVERRIDES_CONTEXT = classPathResource("-delegationOverrides.xml");
  private static final ClassPathResource DEP_CARG_AUTOWIRE_CONTEXT = classPathResource("-depCargAutowire.xml");
  private static final ClassPathResource DEP_CARG_INNER_CONTEXT = classPathResource("-depCargInner.xml");
  private static final ClassPathResource DEP_CARG_CONTEXT = classPathResource("-depCarg.xml");
  private static final ClassPathResource DEP_DEPENDSON_INNER_CONTEXT = classPathResource("-depDependsOnInner.xml");
  private static final ClassPathResource DEP_DEPENDSON_CONTEXT = classPathResource("-depDependsOn.xml");
  private static final ClassPathResource DEP_PROP = classPathResource("-depProp.xml");
  private static final ClassPathResource DEP_PROP_ABN_CONTEXT = classPathResource("-depPropAutowireByName.xml");
  private static final ClassPathResource DEP_PROP_ABT_CONTEXT = classPathResource("-depPropAutowireByType.xml");
  private static final ClassPathResource DEP_PROP_MIDDLE_CONTEXT = classPathResource("-depPropInTheMiddle.xml");
  private static final ClassPathResource DEP_PROP_INNER_CONTEXT = classPathResource("-depPropInner.xml");
  private static final ClassPathResource DEP_MATERIALIZE_CONTEXT = classPathResource("-depMaterializeThis.xml");
  private static final ClassPathResource FACTORY_CIRCLE_CONTEXT = classPathResource("-factoryCircle.xml");
  private static final ClassPathResource INITIALIZERS_CONTEXT = classPathResource("-initializers.xml");
  private static final ClassPathResource INVALID_CONTEXT = classPathResource("-invalid.xml");
  private static final ClassPathResource INVALID_NO_SUCH_METHOD_CONTEXT = classPathResource("-invalidOverridesNoSuchMethod.xml");
  private static final ClassPathResource COLLECTIONS_XSD_CONTEXT = classPathResource("-localCollectionsUsingXsd.xml");
  private static final ClassPathResource MISSING_CONTEXT = classPathResource("-missing.xml");
  private static final ClassPathResource OVERRIDES_CONTEXT = classPathResource("-overrides.xml");
  private static final ClassPathResource PARENT_CONTEXT = classPathResource("-parent.xml");
  private static final ClassPathResource NO_SUCH_FACTORY_METHOD_CONTEXT = classPathResource("-noSuchFactoryMethod.xml");
  private static final ClassPathResource RECURSIVE_IMPORT_CONTEXT = classPathResource("-recursiveImport.xml");
  private static final ClassPathResource RESOURCE_CONTEXT = classPathResource("-resource.xml");
  private static final ClassPathResource TEST_WITH_DUP_NAMES_CONTEXT = classPathResource("-testWithDuplicateNames.xml");
  private static final ClassPathResource TEST_WITH_DUP_NAME_IN_ALIAS_CONTEXT = classPathResource("-testWithDuplicateNameInAlias.xml");
  private static final ClassPathResource REFTYPES_CONTEXT = classPathResource("-reftypes.xml");
  private static final ClassPathResource DEFAULT_LAZY_CONTEXT = classPathResource("-defaultLazyInit.xml");
  private static final ClassPathResource DEFAULT_AUTOWIRE_CONTEXT = classPathResource("-defaultAutowire.xml");

  private static ClassPathResource classPathResource(String suffix) {
    return new ClassPathResource(CLASSNAME + suffix, CLASS);
  }

  @Test
    // SPR-2368
  void collectionsReferredToAsRefLocals() {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(COLLECTIONS_XSD_CONTEXT);
    factory.preInstantiateSingletons();
  }

  @Test
  void refToSeparatePrototypeInstances() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);

    TestBean emma = (TestBean) xbf.getBean("emma");
    TestBean georgia = (TestBean) xbf.getBean("georgia");
    ITestBean emmasJenks = emma.getSpouse();
    ITestBean georgiasJenks = georgia.getSpouse();
    assertThat(emmasJenks != georgiasJenks).as("Emma and georgia think they have a different boyfriend").isTrue();
    assertThat(emmasJenks.getName().equals("Andrew")).as("Emmas jenks has right name").isTrue();
    assertThat(emmasJenks != xbf.getBean("jenks")).as("Emmas doesn't equal new ref").isTrue();
    assertThat(georgiasJenks.getName().equals("Andrew")).as("Georgias jenks has right name").isTrue();
    assertThat(emmasJenks.equals(georgiasJenks)).as("They are object equal").isTrue();
    assertThat(emmasJenks.equals(xbf.getBean("jenks"))).as("They object equal direct ref").isTrue();
  }

  @Test
  void refToSingleton() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(new EncodedResource(REFTYPES_CONTEXT, "ISO-8859-1"));

    TestBean jen = (TestBean) xbf.getBean("jenny");
    TestBean dave = (TestBean) xbf.getBean("david");
    TestBean jenks = (TestBean) xbf.getBean("jenks");
    ITestBean davesJen = dave.getSpouse();
    ITestBean jenksJen = jenks.getSpouse();
    assertThat(davesJen == jenksJen).as("1 jen instance").isTrue();
    assertThat(davesJen == jen).as("1 jen instance").isTrue();
  }

  @Test
  void innerBeans() throws IOException {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);

    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    try (InputStream inputStream = REFTYPES_CONTEXT.getInputStream()) {
      reader.loadBeanDefinitions(new InputSource(inputStream));
    }

    // Let's create the outer bean named "innerBean",
    // to check whether it doesn't create any conflicts
    // with the actual inner beans named "innerBean".
    xbf.getBean("innerBean");

    TestBean hasInnerBeans = (TestBean) xbf.getBean("hasInnerBeans");
    assertThat(hasInnerBeans.getAge()).isEqualTo(5);
    TestBean inner1 = (TestBean) hasInnerBeans.getSpouse();
    assertThat(inner1).isNotNull();
    assertThat(inner1.getBeanName()).isEqualTo("innerBean#1");
    assertThat(inner1.getName()).isEqualTo("inner1");
    assertThat(inner1.getAge()).isEqualTo(6);

    assertThat(hasInnerBeans.getFriends()).isNotNull();
    Object[] friends = hasInnerBeans.getFriends().toArray();
    assertThat(friends.length).isEqualTo(3);
    DerivedTestBean inner2 = (DerivedTestBean) friends[0];
    assertThat(inner2.getName()).isEqualTo("inner2");
    assertThat(inner2.getBeanName().startsWith(DerivedTestBean.class.getName())).isTrue();
    assertThat(xbf.containsBean("innerBean#1")).isFalse();
    assertThat(inner2).isNotNull();
    assertThat(inner2.getAge()).isEqualTo(7);
    TestBean innerFactory = (TestBean) friends[1];
    assertThat(innerFactory.getName()).isEqualTo(DummyFactory.SINGLETON_NAME);
    TestBean inner5 = (TestBean) friends[2];
    assertThat(inner5.getBeanName()).isEqualTo("innerBean#2");

    assertThat(hasInnerBeans.getSomeMap()).isNotNull();
    assertThat(hasInnerBeans.getSomeMap().size()).isEqualTo(2);
    TestBean inner3 = (TestBean) hasInnerBeans.getSomeMap().get("someKey");
    assertThat(inner3.getName()).isEqualTo("Jenny");
    assertThat(inner3.getAge()).isEqualTo(30);
    TestBean inner4 = (TestBean) hasInnerBeans.getSomeMap().get("someOtherKey");
    assertThat(inner4.getName()).isEqualTo("inner4");
    assertThat(inner4.getAge()).isEqualTo(9);

    TestBean hasInnerBeansForConstructor = (TestBean) xbf.getBean("hasInnerBeansForConstructor");
    TestBean innerForConstructor = (TestBean) hasInnerBeansForConstructor.getSpouse();
    assertThat(innerForConstructor).isNotNull();
    assertThat(innerForConstructor.getBeanName()).isEqualTo("innerBean#3");
    assertThat(innerForConstructor.getName()).isEqualTo("inner1");
    assertThat(innerForConstructor.getAge()).isEqualTo(6);

    hasInnerBeansForConstructor = (TestBean) xbf.getBean("hasInnerBeansAsPrototype");
    innerForConstructor = (TestBean) hasInnerBeansForConstructor.getSpouse();
    assertThat(innerForConstructor).isNotNull();
    assertThat(innerForConstructor.getBeanName()).isEqualTo("innerBean");
    assertThat(innerForConstructor.getName()).isEqualTo("inner1");
    assertThat(innerForConstructor.getAge()).isEqualTo(6);

    hasInnerBeansForConstructor = (TestBean) xbf.getBean("hasInnerBeansAsPrototype");
    innerForConstructor = (TestBean) hasInnerBeansForConstructor.getSpouse();
    assertThat(innerForConstructor).isNotNull();
    assertThat(innerForConstructor.getBeanName()).isEqualTo("innerBean");
    assertThat(innerForConstructor.getName()).isEqualTo("inner1");
    assertThat(innerForConstructor.getAge()).isEqualTo(6);

    xbf.destroySingletons();
    assertThat(inner1.wasDestroyed()).isTrue();
    assertThat(inner2.wasDestroyed()).isTrue();
    assertThat(innerFactory.getName() == null).isTrue();
    assertThat(inner5.wasDestroyed()).isTrue();
  }

  @Test
  void innerBeansWithoutDestroy() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);

    // Let's create the outer bean named "innerBean",
    // to check whether it doesn't create any conflicts
    // with the actual inner beans named "innerBean".
    xbf.getBean("innerBean");

    TestBean hasInnerBeans = (TestBean) xbf.getBean("hasInnerBeansWithoutDestroy");
    assertThat(hasInnerBeans.getAge()).isEqualTo(5);
    TestBean inner1 = (TestBean) hasInnerBeans.getSpouse();
    assertThat(inner1).isNotNull();
    assertThat(inner1.getBeanName().startsWith("innerBean")).isTrue();
    assertThat(inner1.getName()).isEqualTo("inner1");
    assertThat(inner1.getAge()).isEqualTo(6);

    assertThat(hasInnerBeans.getFriends()).isNotNull();
    Object[] friends = hasInnerBeans.getFriends().toArray();
    assertThat(friends.length).isEqualTo(3);
    DerivedTestBean inner2 = (DerivedTestBean) friends[0];
    assertThat(inner2.getName()).isEqualTo("inner2");
    assertThat(inner2.getBeanName().startsWith(DerivedTestBean.class.getName())).isTrue();
    assertThat(inner2).isNotNull();
    assertThat(inner2.getAge()).isEqualTo(7);
    TestBean innerFactory = (TestBean) friends[1];
    assertThat(innerFactory.getName()).isEqualTo(DummyFactory.SINGLETON_NAME);
    TestBean inner5 = (TestBean) friends[2];
    assertThat(inner5.getBeanName().startsWith("innerBean")).isTrue();
  }

  @Test
  void failsOnInnerBean() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);

    try {
      xbf.getBean("failsOnInnerBean");
    }
    catch (BeanCreationException ex) {
      // Check whether message contains outer bean name.
      assertThat(ex.getMessage().contains("failsOnInnerBean")).isTrue();
      assertThat(ex.getMessage().contains("someMap")).isTrue();
    }

    try {
      xbf.getBean("failsOnInnerBeanForConstructor");
    }
    catch (BeanCreationException ex) {
      // Check whether message contains outer bean name.
      assertThat(ex.getMessage().contains("failsOnInnerBeanForConstructor")).isTrue();
      assertThat(ex.getMessage().contains("constructor argument")).isTrue();
    }
  }

  @Test
  void inheritanceFromParentFactoryPrototype() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    assertThat(child.getType("inheritsFromParentFactory")).isEqualTo(TestBean.class);
    TestBean inherits = (TestBean) child.getBean("inheritsFromParentFactory");
    // Name property value is overridden
    assertThat(inherits.getName().equals("override")).isTrue();
    // Age property is inherited from bean in parent factory
    assertThat(inherits.getAge() == 1).isTrue();
    TestBean inherits2 = (TestBean) child.getBean("inheritsFromParentFactory");
    assertThat(inherits2 == inherits).isFalse();
  }

  @Test
  void inheritanceWithDifferentClass() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    assertThat(child.getType("inheritsWithClass")).isEqualTo(DerivedTestBean.class);
    DerivedTestBean inherits = (DerivedTestBean) child.getBean("inheritsWithDifferentClass");
    // Name property value is overridden
    assertThat(inherits.getName().equals("override")).isTrue();
    // Age property is inherited from bean in parent factory
    assertThat(inherits.getAge() == 1).isTrue();
    assertThat(inherits.wasInitialized()).isTrue();
  }

  @Test
  void inheritanceWithClass() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    assertThat(child.getType("inheritsWithClass")).isEqualTo(DerivedTestBean.class);
    DerivedTestBean inherits = (DerivedTestBean) child.getBean("inheritsWithClass");
    // Name property value is overridden
    assertThat(inherits.getName().equals("override")).isTrue();
    // Age property is inherited from bean in parent factory
    assertThat(inherits.getAge() == 1).isTrue();
    assertThat(inherits.wasInitialized()).isTrue();
  }

  @Test
  void prototypeInheritanceFromParentFactoryPrototype() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    assertThat(child.getType("prototypeInheritsFromParentFactoryPrototype")).isEqualTo(TestBean.class);
    TestBean inherits = (TestBean) child.getBean("prototypeInheritsFromParentFactoryPrototype");
    // Name property value is overridden
    assertThat(inherits.getName().equals("prototype-override")).isTrue();
    // Age property is inherited from bean in parent factory
    assertThat(inherits.getAge() == 2).isTrue();
    TestBean inherits2 = (TestBean) child.getBean("prototypeInheritsFromParentFactoryPrototype");
    assertThat(inherits2 == inherits).isFalse();
    inherits2.setAge(13);
    assertThat(inherits2.getAge() == 13).isTrue();
    // Shouldn't have changed first instance
    assertThat(inherits.getAge() == 2).isTrue();
  }

  @Test
  void prototypeInheritanceFromParentFactorySingleton() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    TestBean inherits = (TestBean) child.getBean("protoypeInheritsFromParentFactorySingleton");
    // Name property value is overridden
    assertThat(inherits.getName().equals("prototypeOverridesInheritedSingleton")).isTrue();
    // Age property is inherited from bean in parent factory
    assertThat(inherits.getAge() == 1).isTrue();
    TestBean inherits2 = (TestBean) child.getBean("protoypeInheritsFromParentFactorySingleton");
    assertThat(inherits2 == inherits).isFalse();
    inherits2.setAge(13);
    assertThat(inherits2.getAge() == 13).isTrue();
    // Shouldn't have changed first instance
    assertThat(inherits.getAge() == 1).isTrue();
  }

  @Test
  void autowireModeNotInherited() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

    TestBean david = (TestBean) xbf.getBean("magicDavid");
    // the parent bean is autowiring
    assertThat(david.getSpouse()).isNotNull();

    TestBean derivedDavid = (TestBean) xbf.getBean("magicDavidDerived");
    // this fails while it inherits from the child bean
    assertThat(derivedDavid.getSpouse()).as("autowiring not propagated along child relationships").isNull();
  }

  @Test
  void abstractParentBeans() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    parent.preInstantiateSingletons();
    assertThat(parent.isSingleton("inheritedTestBeanWithoutClass")).isTrue();

    // abstract beans should not match
    Map<?, ?> tbs = parent.getBeansOfType(TestBean.class);
    assertThat(tbs.size()).isEqualTo(2);
    assertThat(tbs.containsKey("inheritedTestBeanPrototype")).isTrue();
    assertThat(tbs.containsKey("inheritedTestBeanSingleton")).isTrue();

    // abstract bean should throw exception on creation attempt
    assertThatExceptionOfType(BeanIsAbstractException.class).isThrownBy(() ->
            parent.getBean("inheritedTestBeanWithoutClass"));

    // non-abstract bean should work, even if it serves as parent
    assertThat(parent.getBean("inheritedTestBeanPrototype") instanceof TestBean).isTrue();
  }

  @Test
  void dependenciesMaterializeThis() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(DEP_MATERIALIZE_CONTEXT);

    assertThat(xbf.getBeansOfType(DummyBo.class, true, false).size()).isEqualTo(2);
    assertThat(xbf.getBeansOfType(DummyBo.class, true, true).size()).isEqualTo(3);
    assertThat(xbf.getBeansOfType(DummyBo.class, true, false).size()).isEqualTo(3);
    assertThat(xbf.getBeansOfType(DummyBo.class).size()).isEqualTo(3);
    assertThat(xbf.getBeansOfType(DummyBoImpl.class, true, true).size()).isEqualTo(2);
    assertThat(xbf.getBeansOfType(DummyBoImpl.class, false, true).size()).isEqualTo(1);
    assertThat(xbf.getBeansOfType(DummyBoImpl.class).size()).isEqualTo(2);

    DummyBoImpl bos = (DummyBoImpl) xbf.getBean("boSingleton");
    DummyBoImpl bop = (DummyBoImpl) xbf.getBean("boPrototype");
    assertThat(bop).isNotSameAs(bos);
    assertThat(bos.dao == bop.dao).isTrue();
  }

  @Test
  void childOverridesParentBean() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    TestBean inherits = (TestBean) child.getBean("inheritedTestBean");
    // Name property value is overridden
    assertThat(inherits.getName().equals("overrideParentBean")).isTrue();
    // Age property is inherited from bean in parent factory
    assertThat(inherits.getAge() == 1).isTrue();
    TestBean inherits2 = (TestBean) child.getBean("inheritedTestBean");
    assertThat(inherits2 != inherits).isTrue();
  }

  /**
   * Check that a prototype can't inherit from a bogus parent.
   * If a singleton does this the factory will fail to load.
   */
  @Test
  void bogusParentageFromParentFactory() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
                    child.getBean("bogusParent", TestBean.class))
            .withMessageContaining("bogusParent")
            .withCauseInstanceOf(NoSuchBeanDefinitionException.class);
  }

  /**
   * Note that prototype/singleton distinction is <b>not</b> inherited.
   * It's possible for a subclass singleton not to return independent
   * instances even if derived from a prototype
   */
  @Test
  void singletonInheritsFromParentFactoryPrototype() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    TestBean inherits = (TestBean) child.getBean("singletonInheritsFromParentFactoryPrototype");
    // Name property value is overridden
    assertThat(inherits.getName().equals("prototype-override")).isTrue();
    // Age property is inherited from bean in parent factory
    assertThat(inherits.getAge() == 2).isTrue();
    TestBean inherits2 = (TestBean) child.getBean("singletonInheritsFromParentFactoryPrototype");
    assertThat(inherits2 == inherits).isTrue();
  }

  @Test
  void singletonFromParent() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    TestBean beanFromParent = (TestBean) parent.getBean("inheritedTestBeanSingleton");
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    TestBean beanFromChild = (TestBean) child.getBean("inheritedTestBeanSingleton");
    assertThat(beanFromParent == beanFromChild).as("singleton from parent and child is the same").isTrue();
  }

  @Test
  void nestedPropertyValue() {
    StandardBeanFactory parent = new StandardBeanFactory();
    new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
    StandardBeanFactory child = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
    IndexedTestBean bean = (IndexedTestBean) child.getBean("indexedTestBean");
    assertThat(bean.getArray()[0].getName()).as("name applied correctly").isEqualTo("myname");
  }

  @Test
  void circularReferences() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    TestBean jenny = (TestBean) xbf.getBean("jenny");
    TestBean david = (TestBean) xbf.getBean("david");
    TestBean ego = (TestBean) xbf.getBean("ego");
    TestBean complexInnerEgo = (TestBean) xbf.getBean("complexInnerEgo");
    TestBean complexEgo = (TestBean) xbf.getBean("complexEgo");
    assertThat(jenny.getSpouse() == david).as("Correct circular reference").isTrue();
    assertThat(david.getSpouse() == jenny).as("Correct circular reference").isTrue();
    assertThat(ego.getSpouse() == ego).as("Correct circular reference").isTrue();
    assertThat(complexInnerEgo.getSpouse().getSpouse() == complexInnerEgo).as("Correct circular reference").isTrue();
    assertThat(complexEgo.getSpouse().getSpouse() == complexEgo).as("Correct circular reference").isTrue();
  }

  @Test
  void circularReferencesWithConstructor() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> xbf.getBean("jenny_constructor"))
            .matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> xbf.getBean("david_constructor"))
            .matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
  }

  @Test
  void circularReferencesWithPrototype() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> xbf.getBean("jenny_prototype"))
            .matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> xbf.getBean("david_prototype"))
            .matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
  }

  @Test
  void circularReferencesWithDependOn() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> xbf.getBean("jenny_depends_on"));
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> xbf.getBean("david_depends_on"));
  }

  @Test
  void circularReferenceWithFactoryBeanFirst() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    xbf.getBean("egoBridge");
    TestBean complexEgo = (TestBean) xbf.getBean("complexEgo");
    assertThat(complexEgo.getSpouse().getSpouse() == complexEgo).as("Correct circular reference").isTrue();
  }

  @Test
  void circularReferenceWithTwoFactoryBeans() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    TestBean ego1 = (TestBean) xbf.getBean("ego1");
    assertThat(ego1.getSpouse().getSpouse() == ego1).as("Correct circular reference").isTrue();
    TestBean ego3 = (TestBean) xbf.getBean("ego3");
    assertThat(ego3.getSpouse().getSpouse() == ego3).as("Correct circular reference").isTrue();
  }

  @Test
  void circularReferencesWithNotAllowed() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    xbf.setAllowCircularReferences(false);
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    xbf.getBean("jenny"))
            .matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
  }

  @Test
  void circularReferencesWithWrapping() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    xbf.addBeanPostProcessor(new WrappingPostProcessor());
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    xbf.getBean("jenny"))
            .matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
  }

  @Test
  void circularReferencesWithWrappingAndRawInjectionAllowed() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    xbf.setAllowRawInjectionDespiteWrapping(true);
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(REFTYPES_CONTEXT);
    xbf.addBeanPostProcessor(new WrappingPostProcessor());

    ITestBean jenny = (ITestBean) xbf.getBean("jenny");
    ITestBean david = (ITestBean) xbf.getBean("david");
    assertThat(AopUtils.isAopProxy(jenny)).isTrue();
    assertThat(AopUtils.isAopProxy(david)).isTrue();
    assertThat(jenny.getSpouse()).isSameAs(david);
    assertThat(david.getSpouse()).isNotSameAs(jenny);
    assertThat(david.getSpouse().getName()).isEqualTo("Jenny");
    assertThat(david.getSpouse().getSpouse()).isSameAs(david);
    assertThat(AopUtils.isAopProxy(jenny.getSpouse())).isTrue();
    assertThat(!AopUtils.isAopProxy(david.getSpouse())).isTrue();
  }

  @Test
  void factoryReferenceCircle() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(FACTORY_CIRCLE_CONTEXT);
    TestBean tb = (TestBean) xbf.getBean("singletonFactory");
    DummyFactory db = (DummyFactory) xbf.getBean("&singletonFactory");
    assertThat(tb == db.getOtherTestBean()).isTrue();
  }

  @Test
  void factoryReferenceWithDoublePrefix() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(FACTORY_CIRCLE_CONTEXT);
    assertThat(xbf.getBean("&&singletonFactory")).isInstanceOf(DummyFactory.class);
  }

  @Test
  void complexFactoryReferenceCircle() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(COMPLEX_FACTORY_CIRCLE_CONTEXT);
    xbf.getBean("proxy1");
    // check that unused instances from autowiring got removed
    assertThat(xbf.getSingletonCount()).isEqualTo(4);
    // properly create the remaining two instances
    xbf.getBean("proxy2");
    assertThat(xbf.getSingletonCount()).isEqualTo(5);
  }

  @Test
  void noSuchFactoryBeanMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(NO_SUCH_FACTORY_METHOD_CONTEXT);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            xbf.getBean("defaultTestBean"));
  }

  @Test
  void initMethodIsInvoked() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
    DoubleInitializer in = (DoubleInitializer) xbf.getBean("init-method1");
    // Initializer should have doubled value
    assertThat(in.getNum()).isEqualTo(14);
  }

  /**
   * Test that if a custom initializer throws an exception, it's handled correctly
   */
  @Test
  void initMethodThrowsException() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    xbf.getBean("init-method2"))
            .withCauseInstanceOf(IOException.class)
            .satisfies(ex -> {
              assertThat(ex.getResourceDescription()).contains("initializers.xml");
              assertThat(ex.getBeanName()).isEqualTo("init-method2");
            });
  }

  @Test
  void noSuchInitMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
    assertThatExceptionOfType(FatalBeanException.class).isThrownBy(() ->
                    xbf.getBean("init-method3"))
            .withMessageContaining("initializers.xml")
            .withMessageContaining("init-method3")
            .withMessageContaining("init");
  }

  /**
   * Check that InitializingBean method is called first.
   */
  @Test
  void initializingBeanAndInitMethod() {
    InitAndIB.constructed = false;
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
    assertThat(InitAndIB.constructed).isFalse();
    xbf.preInstantiateSingletons();
    assertThat(InitAndIB.constructed).isFalse();
    InitAndIB iib = (InitAndIB) xbf.getBean("init-and-ib");
    assertThat(InitAndIB.constructed).isTrue();
    assertThat(iib.afterPropertiesSetInvoked && iib.initMethodInvoked).isTrue();
    assertThat(!iib.destroyed && !iib.customDestroyed).isTrue();
    xbf.destroySingletons();
    assertThat(iib.destroyed && iib.customDestroyed).isTrue();
    xbf.destroySingletons();
    assertThat(iib.destroyed && iib.customDestroyed).isTrue();
  }

  /**
   * Check that InitializingBean method is not called twice.
   */
  @Test
  void initializingBeanAndSameInitMethod() {
    InitAndIB.constructed = false;
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
    assertThat(InitAndIB.constructed).isFalse();
    xbf.preInstantiateSingletons();
    assertThat(InitAndIB.constructed).isFalse();
    InitAndIB iib = (InitAndIB) xbf.getBean("ib-same-init");
    assertThat(InitAndIB.constructed).isTrue();
    assertThat(iib.afterPropertiesSetInvoked && !iib.initMethodInvoked).isTrue();
    assertThat(!iib.destroyed && !iib.customDestroyed).isTrue();
    xbf.destroySingletons();
    assertThat(iib.destroyed && !iib.customDestroyed).isTrue();
    xbf.destroySingletons();
    assertThat(iib.destroyed && !iib.customDestroyed).isTrue();
  }

  @Test
  void defaultLazyInit() {
    InitAndIB.constructed = false;
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(DEFAULT_LAZY_CONTEXT);
    assertThat(InitAndIB.constructed).isFalse();
    xbf.preInstantiateSingletons();
    assertThat(InitAndIB.constructed).isTrue();
    try {
      xbf.getBean("lazy-and-bad");
    }
    catch (BeanCreationException ex) {
      assertThat(ex.getCause() instanceof IOException).isTrue();
    }
  }

  @Test
  void noSuchXmlFile() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
            new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(MISSING_CONTEXT));
  }

  @Test
  void invalidXmlFile() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
            new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INVALID_CONTEXT));
  }

  @Test
  void autowire() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(AUTOWIRE_CONTEXT);
    TestBean spouse = new TestBean("kerry", 0);
    xbf.registerSingleton("spouse", spouse);
    doTestAutowire(xbf);
  }

  @Test
  void autowireWithParent() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(AUTOWIRE_CONTEXT);
    StandardBeanFactory lbf = new StandardBeanFactory();
    PropertyValues pvs = new PropertyValues();
    pvs.add("name", "kerry");
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setPropertyValues(pvs);
    lbf.registerBeanDefinition("spouse", bd);
    xbf.setParentBeanFactory(lbf);
    doTestAutowire(xbf);
  }

  private void doTestAutowire(StandardBeanFactory xbf) {
    DependenciesBean rod1 = (DependenciesBean) xbf.getBean("rod1");
    TestBean kerry = (TestBean) xbf.getBean("spouse");
    // should have been autowired
    assertThat(rod1.getSpouse()).isEqualTo(kerry);

    DependenciesBean rod1a = (DependenciesBean) xbf.getBean("rod1a");
    // should have been autowired
    assertThat(rod1a.getSpouse()).isEqualTo(kerry);

    DependenciesBean rod2 = (DependenciesBean) xbf.getBean("rod2");
    // should have been autowired
    assertThat(rod2.getSpouse()).isEqualTo(kerry);

    DependenciesBean rod2a = (DependenciesBean) xbf.getBean("rod2a");
    // should have been set explicitly
    assertThat(rod2a.getSpouse()).isEqualTo(kerry);

    ConstructorDependenciesBean rod3 = (ConstructorDependenciesBean) xbf.getBean("rod3");
    IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
    // should have been autowired
    assertThat(rod3.getSpouse1()).isEqualTo(kerry);
    assertThat(rod3.getSpouse2()).isEqualTo(kerry);
    assertThat(rod3.getOther()).isEqualTo(other);

    ConstructorDependenciesBean rod3a = (ConstructorDependenciesBean) xbf.getBean("rod3a");
    // should have been autowired
    assertThat(rod3a.getSpouse1()).isEqualTo(kerry);
    assertThat(rod3a.getSpouse2()).isEqualTo(kerry);
    assertThat(rod3a.getOther()).isEqualTo(other);

    assertThatExceptionOfType(FatalBeanException.class).isThrownBy(() ->
            xbf.getBean("rod4", ConstructorDependenciesBean.class));

    DependenciesBean rod5 = (DependenciesBean) xbf.getBean("rod5");
    // Should not have been autowired
    assertThat((Object) rod5.getSpouse()).isNull();

    BeanFactory appCtx = (BeanFactory) xbf.getBean("childAppCtx");
    assertThat(appCtx.containsBean("rod1")).isTrue();
    assertThat(appCtx.containsBean("jenny")).isTrue();
  }

  @Test
  void autowireWithDefault() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(DEFAULT_AUTOWIRE_CONTEXT);

    DependenciesBean rod1 = (DependenciesBean) xbf.getBean("rod1");
    // should have been autowired
    assertThat(rod1.getSpouse()).isNotNull();
    assertThat(rod1.getSpouse().getName().equals("Kerry")).isTrue();

    DependenciesBean rod2 = (DependenciesBean) xbf.getBean("rod2");
    // should have been autowired
    assertThat(rod2.getSpouse()).isNotNull();
    assertThat(rod2.getSpouse().getName().equals("Kerry")).isTrue();
  }

  @Test
  void autowireByConstructor() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    ConstructorDependenciesBean rod1 = (ConstructorDependenciesBean) xbf.getBean("rod1");
    TestBean kerry = (TestBean) xbf.getBean("kerry2");
    // should have been autowired
    assertThat(rod1.getSpouse1()).isEqualTo(kerry);
    assertThat(rod1.getAge()).isEqualTo(0);
    assertThat(rod1.getName()).isNull();

    ConstructorDependenciesBean rod2 = (ConstructorDependenciesBean) xbf.getBean("rod2");
    TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
    TestBean kerry2 = (TestBean) xbf.getBean("kerry2");
    // should have been autowired
    assertThat(rod2.getSpouse1()).isEqualTo(kerry2);
    assertThat(rod2.getSpouse2()).isEqualTo(kerry1);
    assertThat(rod2.getAge()).isEqualTo(0);
    assertThat(rod2.getName()).isNull();

    ConstructorDependenciesBean rod = (ConstructorDependenciesBean) xbf.getBean("rod3");
    IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
    // should have been autowired
    assertThat(rod.getSpouse1()).isEqualTo(kerry);
    assertThat(rod.getSpouse2()).isEqualTo(kerry);
    assertThat(rod.getOther()).isEqualTo(other);
    assertThat(rod.getAge()).isEqualTo(0);
    assertThat(rod.getName()).isNull();

    xbf.getBean("rod4", ConstructorDependenciesBean.class);
    // should have been autowired
    assertThat(rod.getSpouse1()).isEqualTo(kerry);
    assertThat(rod.getSpouse2()).isEqualTo(kerry);
    assertThat(rod.getOther()).isEqualTo(other);
    assertThat(rod.getAge()).isEqualTo(0);
    assertThat(rod.getName()).isNull();
  }

  @Test
  void autowireByConstructorWithSimpleValues() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

    ConstructorDependenciesBean rod5 = (ConstructorDependenciesBean) xbf.getBean("rod5");
    TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
    TestBean kerry2 = (TestBean) xbf.getBean("kerry2");
    IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
    // should have been autowired
    assertThat(rod5.getSpouse1()).isEqualTo(kerry2);
    assertThat(rod5.getSpouse2()).isEqualTo(kerry1);
    assertThat(rod5.getOther()).isEqualTo(other);
    assertThat(rod5.getAge()).isEqualTo(99);
    assertThat(rod5.getName()).isEqualTo("myname");

    DerivedConstructorDependenciesBean rod6 = (DerivedConstructorDependenciesBean) xbf.getBean("rod6");
    // should have been autowired
    assertThat(rod6.initialized).isTrue();
    assertThat(!rod6.destroyed).isTrue();
    assertThat(rod6.getSpouse1()).isEqualTo(kerry2);
    assertThat(rod6.getSpouse2()).isEqualTo(kerry1);
    assertThat(rod6.getOther()).isEqualTo(other);
    assertThat(rod6.getAge()).isEqualTo(0);
    assertThat(rod6.getName()).isNull();

    xbf.destroySingletons();
    assertThat(rod6.destroyed).isTrue();
  }

  @Test
  void relatedCausesFromConstructorResolution() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

    try {
      xbf.getBean("rod2Accessor");
    }
    catch (BeanCreationException ex) {
      ex.printStackTrace();
      assertThat(ex.getNestedMessage().contains("touchy")).isTrue();
      assertThat((Object) ex.getRelatedCauses()).isNull();
    }
  }

  @Test
  void constructorArgResolution() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
    TestBean kerry2 = (TestBean) xbf.getBean("kerry2");

    ConstructorDependenciesBean rod9 = (ConstructorDependenciesBean) xbf.getBean("rod9");
    assertThat(rod9.getAge()).isEqualTo(99);
    ConstructorDependenciesBean rod9a = (ConstructorDependenciesBean) xbf.getBean("rod9", 98);
    assertThat(rod9a.getAge()).isEqualTo(98);
    ConstructorDependenciesBean rod9b = (ConstructorDependenciesBean) xbf.getBean("rod9", "myName");
    assertThat(rod9b.getName()).isEqualTo("myName");
    ConstructorDependenciesBean rod9c = (ConstructorDependenciesBean) xbf.getBean("rod9", 97);
    assertThat(rod9c.getAge()).isEqualTo(97);

    ConstructorDependenciesBean rod10 = (ConstructorDependenciesBean) xbf.getBean("rod10");
    assertThat(rod10.getName()).isNull();

    ConstructorDependenciesBean rod11 = (ConstructorDependenciesBean) xbf.getBean("rod11");
    assertThat(rod11.getSpouse1()).isEqualTo(kerry2);

    ConstructorDependenciesBean rod12 = (ConstructorDependenciesBean) xbf.getBean("rod12");
    assertThat(rod12.getSpouse1()).isEqualTo(kerry1);
    assertThat(rod12.getSpouse2()).isNull();

    ConstructorDependenciesBean rod13 = (ConstructorDependenciesBean) xbf.getBean("rod13");
    assertThat(rod13.getSpouse1()).isEqualTo(kerry1);
    assertThat(rod13.getSpouse2()).isEqualTo(kerry2);

    ConstructorDependenciesBean rod14 = (ConstructorDependenciesBean) xbf.getBean("rod14");
    assertThat(rod14.getSpouse1()).isEqualTo(kerry1);
    assertThat(rod14.getSpouse2()).isEqualTo(kerry2);

    ConstructorDependenciesBean rod15 = (ConstructorDependenciesBean) xbf.getBean("rod15");
    assertThat(rod15.getSpouse1()).isEqualTo(kerry2);
    assertThat(rod15.getSpouse2()).isEqualTo(kerry1);

    ConstructorDependenciesBean rod16 = (ConstructorDependenciesBean) xbf.getBean("rod16");
    assertThat(rod16.getSpouse1()).isEqualTo(kerry2);
    assertThat(rod16.getSpouse2()).isEqualTo(kerry1);
    assertThat(rod16.getAge()).isEqualTo(29);

    ConstructorDependenciesBean rod17 = (ConstructorDependenciesBean) xbf.getBean("rod17");
    assertThat(rod17.getSpouse1()).isEqualTo(kerry1);
    assertThat(rod17.getSpouse2()).isEqualTo(kerry2);
    assertThat(rod17.getAge()).isEqualTo(29);
  }

  @Test
  void prototypeWithExplicitArguments() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    SimpleConstructorArgBean cd1 = (SimpleConstructorArgBean) xbf.getBean("rod18");
    assertThat(cd1.getAge()).isEqualTo(0);
    SimpleConstructorArgBean cd2 = (SimpleConstructorArgBean) xbf.getBean("rod18", 98);
    assertThat(cd2.getAge()).isEqualTo(98);
    SimpleConstructorArgBean cd3 = (SimpleConstructorArgBean) xbf.getBean("rod18", "myName");
    assertThat(cd3.getName()).isEqualTo("myName");
    SimpleConstructorArgBean cd4 = (SimpleConstructorArgBean) xbf.getBean("rod18");
    assertThat(cd4.getAge()).isEqualTo(0);
    SimpleConstructorArgBean cd5 = (SimpleConstructorArgBean) xbf.getBean("rod18", 97);
    assertThat(cd5.getAge()).isEqualTo(97);
  }

  @Test
  void constructorArgWithSingleMatch() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    File file = (File) xbf.getBean("file");
    assertThat(file.getPath()).isEqualTo((File.separator + "test"));
  }

  @Test
  void throwsExceptionOnTooManyArguments() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            xbf.getBean("rod7", ConstructorDependenciesBean.class));
  }

  @Test
  void throwsExceptionOnAmbiguousResolution() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
            xbf.getBean("rod8", ConstructorDependenciesBean.class));
  }

  @Test
  void dependsOn() {
    doTestDependencies(DEP_DEPENDSON_CONTEXT, 1);
  }

  @Test
  void dependsOnInInnerBean() {
    doTestDependencies(DEP_DEPENDSON_INNER_CONTEXT, 4);
  }

  @Test
  void dependenciesThroughConstructorArguments() {
    doTestDependencies(DEP_CARG_CONTEXT, 1);
  }

  @Test
  void dependenciesThroughConstructorArgumentAutowiring() {
    doTestDependencies(DEP_CARG_AUTOWIRE_CONTEXT, 1);
  }

  @Test
  void dependenciesThroughConstructorArgumentsInInnerBean() {
    doTestDependencies(DEP_CARG_INNER_CONTEXT, 1);
  }

  @Test
  void dependenciesThroughProperties() {
    doTestDependencies(DEP_PROP, 1);
  }

  @Test
  void dependenciesThroughPropertiesWithInTheMiddle() {
    doTestDependencies(DEP_PROP_MIDDLE_CONTEXT, 1);
  }

  @Test
  void dependenciesThroughPropertyAutowiringByName() {
    doTestDependencies(DEP_PROP_ABN_CONTEXT, 1);
  }

  @Test
  void dependenciesThroughPropertyAutowiringByType() {
    doTestDependencies(DEP_PROP_ABT_CONTEXT, 1);
  }

  @Test
  void dependenciesThroughPropertiesInInnerBean() {
    doTestDependencies(DEP_PROP_INNER_CONTEXT, 1);
  }

  private void doTestDependencies(ClassPathResource resource, int nrOfHoldingBeans) {
    PreparingBean1.prepared = false;
    PreparingBean1.destroyed = false;
    PreparingBean2.prepared = false;
    PreparingBean2.destroyed = false;
    DependingBean.destroyCount = 0;
    HoldingBean.destroyCount = 0;
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(resource);
    xbf.preInstantiateSingletons();
    xbf.destroySingletons();
    assertThat(PreparingBean1.prepared).isTrue();
    assertThat(PreparingBean1.destroyed).isTrue();
    assertThat(PreparingBean2.prepared).isTrue();
    assertThat(PreparingBean2.destroyed).isTrue();
    assertThat(DependingBean.destroyCount).isEqualTo(nrOfHoldingBeans);
    if (!xbf.getBeansOfType(HoldingBean.class, false, false).isEmpty()) {
      assertThat(HoldingBean.destroyCount).isEqualTo(nrOfHoldingBeans);
    }
  }

  /**
   * When using a BeanFactory. singletons are of course not pre-instantiated.
   * So rubbish class names in bean defs must now not be 'resolved' when the
   * bean def is being parsed, 'cos everything on a bean def is now lazy, but
   * must rather only be picked up when the bean is instantiated.
   */
  @Test
  void classNotFoundWithDefaultBeanClassLoader() {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(CLASS_NOT_FOUND_CONTEXT);
    // cool, no errors, so the rubbish class name in the bean def was not resolved
    // let's resolve the bean definition; must blow up
    assertThatExceptionOfType(BeanClassLoadFailedException.class).isThrownBy(() ->
                    factory.getBean("classNotFound"))
            .withCauseInstanceOf(ClassNotFoundException.class)
            .satisfies(ex -> assertThat(ex.getResourceDescription()).contains("classNotFound.xml"));
  }

  @Test
  void classNotFoundWithNoBeanClassLoader() {
    StandardBeanFactory bf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    reader.setBeanClassLoader(null);
    reader.loadBeanDefinitions(CLASS_NOT_FOUND_CONTEXT);
    assertThat(bf.getBeanDefinition("classNotFound").getBeanClassName()).isEqualTo("WhatALotOfRubbish");
  }

  @Test
  void resourceAndInputStream() throws IOException {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(RESOURCE_CONTEXT);
    // comes from "resourceImport.xml"
    ResourceTestBean resource1 = (ResourceTestBean) xbf.getBean("resource1");
    // comes from "resource.xml"
    ResourceTestBean resource2 = (ResourceTestBean) xbf.getBean("resource2");

    assertThat(resource1.getResource() instanceof ClassPathResource).isTrue();
    StringWriter writer = new StringWriter();
    FileCopyUtils.copy(new InputStreamReader(resource1.getResource().getInputStream()), writer);
    assertThat(writer.toString()).isEqualTo("test");
    writer = new StringWriter();
    FileCopyUtils.copy(new InputStreamReader(resource1.getInputStream()), writer);
    assertThat(writer.toString()).isEqualTo("test");
    writer = new StringWriter();
    FileCopyUtils.copy(new InputStreamReader(resource2.getResource().getInputStream()), writer);
    assertThat(writer.toString()).isEqualTo("test");
    writer = new StringWriter();
    FileCopyUtils.copy(new InputStreamReader(resource2.getInputStream()), writer);
    assertThat(writer.toString()).isEqualTo("test");
  }

  @Test
  void classPathResourceWithImport() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(RESOURCE_CONTEXT);
    // comes from "resourceImport.xml"
    xbf.getBean("resource1", ResourceTestBean.class);
    // comes from "resource.xml"
    xbf.getBean("resource2", ResourceTestBean.class);
  }

  @Test
  void urlResourceWithImport() throws Exception {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(new UrlResource(RESOURCE_CONTEXT.getURL()));
    // comes from "resourceImport.xml"
    xbf.getBean("resource1", ResourceTestBean.class);
    // comes from "resource.xml"
    xbf.getBean("resource2", ResourceTestBean.class);
  }

  @Test
  void fileSystemResourceWithImport() throws Exception {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(new FileSystemResource(RESOURCE_CONTEXT.getFile()));
    // comes from "resourceImport.xml"
    xbf.getBean("resource1", ResourceTestBean.class);
    // comes from "resource.xml"
    xbf.getBean("resource2", ResourceTestBean.class);
  }

  @Test
  void recursiveImport() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
            new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(RECURSIVE_IMPORT_CONTEXT));
  }

  /**
   * See <a href="https://jira.spring.io/browse/SPR-10785">SPR-10785</a> and <a
   * href="https://jira.spring.io/browse/SPR-11420">SPR-11420</a>
   */
  @Test
  @SuppressWarnings("deprecation")
  void methodInjectedBeanMustBeOfSameEnhancedCglibSubclassTypeAcrossBeanFactories() {
    Class<?> firstClass = null;

    for (int i = 0; i < 10; i++) {
      StandardBeanFactory bf = new StandardBeanFactory();
      new XmlBeanDefinitionReader(bf).loadBeanDefinitions(OVERRIDES_CONTEXT);

      final Class<?> currentClass = bf.getBean("overrideOneMethod").getClass();
      assertThat(isCglibProxyClass(currentClass)).as("Method injected bean class [" + currentClass + "] must be a CGLIB enhanced subclass.").isTrue();

      if (firstClass == null) {
        firstClass = currentClass;
      }
      else {
        assertThat(currentClass).isEqualTo(firstClass);
      }
    }
  }

  public static boolean isCglibProxyClass(@Nullable Class<?> clazz) {
    return (clazz != null && isCglibProxyClassName(clazz.getName()));
  }

  /** The CGLIB class separator: {@code "$$"}. */
  public static final String CGLIB_CLASS_SEPARATOR = "$$";

  /**
   * Check whether the specified class name is a CGLIB-generated class.
   *
   * @param className the class name to check
   */
  public static boolean isCglibProxyClassName(@Nullable String className) {
    return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
  }

  @Test
  void lookupOverrideMethodsWithSetterInjection() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

    lookupOverrideMethodsWithSetterInjection(xbf, "overrideOneMethod", true);
    // Should work identically on subclass definition, in which lookup
    // methods are inherited
    lookupOverrideMethodsWithSetterInjection(xbf, "overrideInheritedMethod", true);

    // Check cost of repeated construction of beans with method overrides
    // Will pick up misuse of CGLIB
    int howMany = 100;
    StopWatch sw = new StopWatch();
    sw.start("Look up " + howMany + " prototype bean instances with method overrides");
    for (int i = 0; i < howMany; i++) {
      lookupOverrideMethodsWithSetterInjection(xbf, "overrideOnPrototype", false);
    }
    sw.stop();
    // System.out.println(sw);
    if (!LoggerFactory.getLogger(StandardBeanFactory.class).isDebugEnabled()) {
      assertThat(sw.getTotalTimeMillis() < 2000).isTrue();
    }

    // Now test distinct bean with swapped value in factory, to ensure the two are independent
    OverrideOneMethod swappedOom = (OverrideOneMethod) xbf.getBean("overrideOneMethodSwappedReturnValues");

    TestBean tb = swappedOom.getPrototypeDependency();
    assertThat(tb.getName()).isEqualTo("David");
    tb = swappedOom.protectedOverrideSingleton();
    assertThat(tb.getName()).isEqualTo("Jenny");
  }

  private void lookupOverrideMethodsWithSetterInjection(BeanFactory xbf,
          String beanName, boolean singleton) {
    OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean(beanName);

    if (singleton) {
      assertThat(xbf.getBean(beanName)).isSameAs(oom);
    }
    else {
      assertThat(xbf.getBean(beanName)).isNotSameAs(oom);
    }

    TestBean jenny1 = oom.getPrototypeDependency();
    assertThat(jenny1.getName()).isEqualTo("Jenny");
    TestBean jenny2 = oom.getPrototypeDependency();
    assertThat(jenny2.getName()).isEqualTo("Jenny");
    assertThat(jenny2).isNotSameAs(jenny1);

    // Check that the bean can invoke the overridden method on itself
    // This differs from Framework's AOP support, which has a distinct notion
    // of a "target" object, meaning that the target needs explicit knowledge
    // of AOP proxying to invoke an advised method on itself.
    TestBean jenny3 = oom.invokesOverriddenMethodOnSelf();
    assertThat(jenny3.getName()).isEqualTo("Jenny");
    assertThat(jenny3).isNotSameAs(jenny1);

    // Now try protected method, and singleton
    TestBean dave1 = oom.protectedOverrideSingleton();
    assertThat(dave1.getName()).isEqualTo("David");
    TestBean dave2 = oom.protectedOverrideSingleton();
    assertThat(dave2.getName()).isEqualTo("David");
    assertThat(dave2).isSameAs(dave1);
  }

  @Test
  void replaceMethodOverrideWithSetterInjection() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);

    OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethod");

    // Same contract as for overrides.xml
    TestBean jenny1 = oom.getPrototypeDependency();
    assertThat(jenny1.getName()).isEqualTo("Jenny");
    TestBean jenny2 = oom.getPrototypeDependency();
    assertThat(jenny2.getName()).isEqualTo("Jenny");
    assertThat(jenny2).isNotSameAs(jenny1);

    TestBean notJenny = oom.getPrototypeDependency("someParam");
    assertThat(!"Jenny".equals(notJenny.getName())).isTrue();

    // Now try protected method, and singleton
    TestBean dave1 = oom.protectedOverrideSingleton();
    assertThat(dave1.getName()).isEqualTo("David");
    TestBean dave2 = oom.protectedOverrideSingleton();
    assertThat(dave2.getName()).isEqualTo("David");
    assertThat(dave2).isSameAs(dave1);

    // Check unadvised behaviour
    String str = "woierowijeiowiej";
    assertThat(oom.echo(str)).isEqualTo(str);

    // Now test replace
    String s = "this is not a palindrome";
    String reverse = new StringBuilder(s).reverse().toString();
    assertThat(oom.replaceMe(s)).as("Should have overridden to reverse, not echo").isEqualTo(reverse);

    assertThat(oom.replaceMe()).as("Should have overridden no-arg overloaded replaceMe method to return fixed value").isEqualTo(FixedMethodReplacer.VALUE);

    OverrideOneMethodSubclass ooms = (OverrideOneMethodSubclass) xbf.getBean("replaceVoidMethod");
    DoSomethingReplacer dos = (DoSomethingReplacer) xbf.getBean("doSomethingReplacer");
    assertThat(dos.lastArg).isNull();
    String s1 = "";
    String s2 = "foo bar black sheep";
    ooms.doSomething(s1);
    assertThat(dos.lastArg).isEqualTo(s1);
    ooms.doSomething(s2);
    assertThat(dos.lastArg).isEqualTo(s2);
  }

  @Test
  void lookupOverrideOneMethodWithConstructorInjection() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(CONSTRUCTOR_OVERRIDES_CONTEXT);

    ConstructorInjectedOverrides cio = (ConstructorInjectedOverrides) xbf.getBean("constructorOverrides");

    // Check that the setter was invoked...
    // We should be able to combine Constructor and
    // Setter Injection
    assertThat(cio.getSetterString()).as("Setter string was set").isEqualTo("from property element");

    // Jenny is a singleton
    TestBean jenny = (TestBean) xbf.getBean("jenny");
    assertThat(cio.getTestBean()).isSameAs(jenny);
    assertThat(cio.getTestBean()).isSameAs(jenny);
    FactoryMethods fm1 = cio.createFactoryMethods();
    FactoryMethods fm2 = cio.createFactoryMethods();
    assertThat(fm2).as("FactoryMethods reference is to a prototype").isNotSameAs(fm1);
    assertThat(fm2.getTestBean()).as("The two prototypes hold the same singleton reference").isSameAs(fm1.getTestBean());
  }

  @Test
  void rejectsOverrideOfBogusMethodName() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(INVALID_NO_SUCH_METHOD_CONTEXT);
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> xbf.getBean("constructorOverrides"))
            .havingCause()
            .isInstanceOf(BeanDefinitionValidationException.class)
            .withMessageContaining("bogusMethod");
  }

  @Test
  void serializableMethodReplacerAndSuperclass() throws IOException {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
    SerializableMethodReplacerCandidate s = (SerializableMethodReplacerCandidate) xbf.getBean("serializableReplacer");
    String forwards = "this is forwards";
    String backwards = new StringBuilder(forwards).reverse().toString();
    assertThat(s.replaceMe(forwards)).isEqualTo(backwards);
    // SPR-356: lookup methods & method replacers are not serializable.
    assertThat(SerializationTestUtils.isSerializable(s)).as("Lookup methods and method replacers are not meant to be serializable.").isFalse();
  }

  @Test
  void innerBeanInheritsScopeFromConcreteChildDefinition() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

    TestBean jenny1 = (TestBean) xbf.getBean("jennyChild");
    assertThat(jenny1.getFriends().size()).isEqualTo(1);
    Object friend1 = jenny1.getFriends().iterator().next();
    assertThat(friend1 instanceof TestBean).isTrue();

    TestBean jenny2 = (TestBean) xbf.getBean("jennyChild");
    assertThat(jenny2.getFriends().size()).isEqualTo(1);
    Object friend2 = jenny2.getFriends().iterator().next();
    assertThat(friend2 instanceof TestBean).isTrue();

    assertThat(jenny2).isNotSameAs(jenny1);
    assertThat(friend2).isNotSameAs(friend1);
  }

  @Test
  void constructorArgWithSingleSimpleTypeMatch() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

    SingleSimpleTypeConstructorBean bean = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBoolean");
    assertThat(bean.isSingleBoolean()).isTrue();

    SingleSimpleTypeConstructorBean bean2 = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBoolean2");
    assertThat(bean2.isSingleBoolean()).isTrue();
  }

  @Test
  void constructorArgWithDoubleSimpleTypeMatch() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

    SingleSimpleTypeConstructorBean bean = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBooleanAndString");
    assertThat(bean.isSecondBoolean()).isTrue();
    assertThat(bean.getTestString()).isEqualTo("A String");

    SingleSimpleTypeConstructorBean bean2 = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBooleanAndString2");
    assertThat(bean2.isSecondBoolean()).isTrue();
    assertThat(bean2.getTestString()).isEqualTo("A String");
  }

  @Test
  void doubleBooleanAutowire() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    DoubleBooleanConstructorBean bean = (DoubleBooleanConstructorBean) xbf.getBean("beanWithDoubleBoolean");
    assertThat(bean.boolean1).isEqualTo(Boolean.TRUE);
    assertThat(bean.boolean2).isEqualTo(Boolean.FALSE);
  }

  @Test
  void doubleBooleanAutowireWithIndex() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    DoubleBooleanConstructorBean bean = (DoubleBooleanConstructorBean) xbf.getBean("beanWithDoubleBooleanAndIndex");
    assertThat(bean.boolean1).isEqualTo(Boolean.FALSE);
    assertThat(bean.boolean2).isEqualTo(Boolean.TRUE);
  }

  @Test
  void lenientDependencyMatching() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    LenientDependencyTestBean bean = (LenientDependencyTestBean) xbf.getBean("lenientDependencyTestBean");
    assertThat(bean.tb instanceof DerivedTestBean).isTrue();
  }

  @Test
  void lenientDependencyMatchingFactoryMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    LenientDependencyTestBean bean = (LenientDependencyTestBean) xbf.getBean("lenientDependencyTestBeanFactoryMethod");
    assertThat(bean.tb instanceof DerivedTestBean).isTrue();
  }

  @Test
  void nonLenientDependencyMatching() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("lenientDependencyTestBean");
    bd.setLenientConstructorResolution(false);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    xbf.getBean("lenientDependencyTestBean"))
            .satisfies(ex -> assertThat(ex.getMostSpecificCause().getMessage()).contains("Ambiguous"));
  }

  @Test
  void nonLenientDependencyMatchingFactoryMethod() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("lenientDependencyTestBeanFactoryMethod");
    bd.setLenientConstructorResolution(false);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
                    xbf.getBean("lenientDependencyTestBeanFactoryMethod"))
            .satisfies(ex -> assertThat(ex.getMostSpecificCause().getMessage()).contains("Ambiguous"));
  }

  @Test
  void javaLangStringConstructor() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("string");
    bd.setLenientConstructorResolution(false);
    String str = (String) xbf.getBean("string");
    assertThat(str).isEqualTo("test");
  }

  @Test
  void customStringConstructor() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("stringConstructor");
    bd.setLenientConstructorResolution(false);
    StringConstructorTestBean tb = (StringConstructorTestBean) xbf.getBean("stringConstructor");
    assertThat(tb.name).isEqualTo("test");
  }

  @Test
  void primitiveConstructorArray() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArray");
    assertThat(bean.array instanceof int[]).isTrue();
    assertThat(((int[]) bean.array).length).isEqualTo(1);
    assertThat(((int[]) bean.array)[0]).isEqualTo(1);
  }

  @Test
  void indexedPrimitiveConstructorArray() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("indexedConstructorArray");
    assertThat(bean.array instanceof int[]).isTrue();
    assertThat(((int[]) bean.array).length).isEqualTo(1);
    assertThat(((int[]) bean.array)[0]).isEqualTo(1);
  }

  @Test
  void stringConstructorArrayNoType() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArrayNoType");
    assertThat(bean.array instanceof String[]).isTrue();
    assertThat(((String[]) bean.array).length).isEqualTo(0);
  }

  @Test
  void stringConstructorArrayNoTypeNonLenient() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("constructorArrayNoType");
    bd.setLenientConstructorResolution(false);
    ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArrayNoType");
    assertThat(bean.array instanceof String[]).isTrue();
    assertThat(((String[]) bean.array).length).isEqualTo(0);
  }

  @Test
  void constructorWithUnresolvableParameterName() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
    AtomicInteger bean = (AtomicInteger) xbf.getBean("constructorUnresolvableName");
    assertThat(bean.get()).isEqualTo(1);
    bean = (AtomicInteger) xbf.getBean("constructorUnresolvableNameWithIndex");
    assertThat(bean.get()).isEqualTo(1);
  }

  @Test
  void withDuplicateName() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
                    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(TEST_WITH_DUP_NAMES_CONTEXT))
            .withMessageContaining("Bean name 'foo'");
  }

  @Test
  void withDuplicateNameInAlias() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
                    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(TEST_WITH_DUP_NAME_IN_ALIAS_CONTEXT))
            .withMessageContaining("Bean name 'foo'");
  }

  @Test
  void overrideMethodByArgTypeAttribute() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
    OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethodByAttribute");
    assertThat(oom.replaceMe(1)).as("should not replace").isEqualTo("replaceMe:1");
    assertThat(oom.replaceMe("abc")).as("should replace").isEqualTo("cba");
  }

  @Test
  void overrideMethodByArgTypeElement() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
    reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
    OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethodByElement");
    assertThat(oom.replaceMe(1)).as("should not replace").isEqualTo("replaceMe:1");
    assertThat(oom.replaceMe("abc")).as("should replace").isEqualTo("cba");
  }

  static class DoSomethingReplacer implements MethodReplacer {

    public Object lastArg;

    @Override
    public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
      assertThat(args.length).isEqualTo(1);
      assertThat(method.getName()).isEqualTo("doSomething");
      lastArg = args[0];
      return null;
    }
  }

  static class BadInitializer {

    /** Init method */
    public void init2() throws IOException {
      throw new IOException();
    }
  }

  static class DoubleInitializer {

    private int num;

    public int getNum() {
      return num;
    }

    public void setNum(int i) {
      num = i;
    }

    /** Init method */
    public void init() {
      this.num *= 2;
    }
  }

  static class InitAndIB implements InitializingBean, DisposableBean {

    public static boolean constructed;

    public boolean afterPropertiesSetInvoked, initMethodInvoked, destroyed, customDestroyed;

    public InitAndIB() {
      constructed = true;
    }

    @Override
    public void afterPropertiesSet() {
      assertThat(this.initMethodInvoked).isFalse();
      if (this.afterPropertiesSetInvoked) {
        throw new IllegalStateException("Already initialized");
      }
      this.afterPropertiesSetInvoked = true;
    }

    /** Init method */
    public void customInit() throws IOException {
      assertThat(this.afterPropertiesSetInvoked).isTrue();
      if (this.initMethodInvoked) {
        throw new IllegalStateException("Already customInitialized");
      }
      this.initMethodInvoked = true;
    }

    @Override
    public void destroy() {
      assertThat(this.customDestroyed).isFalse();
      if (this.destroyed) {
        throw new IllegalStateException("Already destroyed");
      }
      this.destroyed = true;
    }

    public void customDestroy() {
      assertThat(this.destroyed).isTrue();
      if (this.customDestroyed) {
        throw new IllegalStateException("Already customDestroyed");
      }
      this.customDestroyed = true;
    }
  }

  static class PreparingBean1 implements DisposableBean {

    public static boolean prepared = false;

    public static boolean destroyed = false;

    public PreparingBean1() {
      prepared = true;
    }

    @Override
    public void destroy() {
      destroyed = true;
    }
  }

  static class PreparingBean2 implements DisposableBean {

    public static boolean prepared = false;

    public static boolean destroyed = false;

    public PreparingBean2() {
      prepared = true;
    }

    @Override
    public void destroy() {
      destroyed = true;
    }
  }

  static class DependingBean implements InitializingBean, DisposableBean {

    public static int destroyCount = 0;

    public boolean destroyed = false;

    public DependingBean() {
    }

    public DependingBean(PreparingBean1 bean1, PreparingBean2 bean2) {
    }

    public void setBean1(PreparingBean1 bean1) {
    }

    public void setBean2(PreparingBean2 bean2) {
    }

    public void setInTheMiddleBean(InTheMiddleBean bean) {
    }

    @Override
    public void afterPropertiesSet() {
      if (!(PreparingBean1.prepared && PreparingBean2.prepared)) {
        throw new IllegalStateException("Need prepared PreparingBeans!");
      }
    }

    @Override
    public void destroy() {
      if (PreparingBean1.destroyed || PreparingBean2.destroyed) {
        throw new IllegalStateException("Should not be destroyed after PreparingBeans");
      }
      destroyed = true;
      destroyCount++;
    }
  }

  static class InTheMiddleBean {

    public void setBean1(PreparingBean1 bean1) {
    }

    public void setBean2(PreparingBean2 bean2) {
    }
  }

  static class HoldingBean implements DisposableBean {

    public static int destroyCount = 0;

    private DependingBean dependingBean;

    public boolean destroyed = false;

    public void setDependingBean(DependingBean dependingBean) {
      this.dependingBean = dependingBean;
    }

    @Override
    public void destroy() {
      if (this.dependingBean.destroyed) {
        throw new IllegalStateException("Should not be destroyed after DependingBean");
      }
      this.destroyed = true;
      destroyCount++;
    }
  }

  static class DoubleBooleanConstructorBean {

    private Boolean boolean1;
    private Boolean boolean2;

    public DoubleBooleanConstructorBean(Boolean b1, Boolean b2) {
      this.boolean1 = b1;
      this.boolean2 = b2;
    }

    public DoubleBooleanConstructorBean(String s1, String s2) {
      throw new IllegalStateException("Don't pick this constructor");
    }

    public static DoubleBooleanConstructorBean create(Boolean b1, Boolean b2) {
      return new DoubleBooleanConstructorBean(b1, b2);
    }

    public static DoubleBooleanConstructorBean create(String s1, String s2) {
      return new DoubleBooleanConstructorBean(s1, s2);
    }
  }

  static class LenientDependencyTestBean {

    public final ITestBean tb;

    public LenientDependencyTestBean(ITestBean tb) {
      this.tb = tb;
    }

    public LenientDependencyTestBean(TestBean tb) {
      this.tb = tb;
    }

    public LenientDependencyTestBean(DerivedTestBean tb) {
      this.tb = tb;
    }

    @SuppressWarnings("rawtypes")
    public LenientDependencyTestBean(Map[] m) {
      throw new IllegalStateException("Don't pick this constructor");
    }

    public static LenientDependencyTestBean create(ITestBean tb) {
      return new LenientDependencyTestBean(tb);
    }

    public static LenientDependencyTestBean create(TestBean tb) {
      return new LenientDependencyTestBean(tb);
    }

    public static LenientDependencyTestBean create(DerivedTestBean tb) {
      return new LenientDependencyTestBean(tb);
    }
  }

  static class ConstructorArrayTestBean {

    public final Object array;

    public ConstructorArrayTestBean(int[] array) {
      this.array = array;
    }

    public ConstructorArrayTestBean(float[] array) {
      this.array = array;
    }

    public ConstructorArrayTestBean(short[] array) {
      this.array = array;
    }

    public ConstructorArrayTestBean(String[] array) {
      this.array = array;
    }
  }

  static class StringConstructorTestBean {

    public final String name;

    public StringConstructorTestBean(String name) {
      this.name = name;
    }
  }

  static class WrappingPostProcessor implements InitializationBeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      ProxyFactory pf = new ProxyFactory(bean);
      return pf.getProxy();
    }
  }

}

class MixedCollectionBean {

  private Collection<?> jumble;

  public void setJumble(Collection<?> jumble) {
    this.jumble = jumble;
  }

  public Collection<?> getJumble() {
    return jumble;
  }
}

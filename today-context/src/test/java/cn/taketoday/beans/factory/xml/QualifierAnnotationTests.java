/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import cn.taketoday.beans.factory.support.AutowireCandidateQualifier;
import cn.taketoday.beans.factory.support.BeanDefinitionReader;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.context.support.StaticApplicationContext;

import static cn.taketoday.util.ClassUtils.convertClassNameToResourcePath;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class QualifierAnnotationTests {

  private static final String CLASSNAME = QualifierAnnotationTests.class.getName();

  private static final String CONFIG_LOCATION =
          format("classpath:%s-context.xml", convertClassNameToResourcePath(CLASSNAME));

  @Test
  void testNonQualifiedFieldFails() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", NonQualifiedTestBean.class);

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(context::refresh)
            .withMessageContaining("found 6");
  }

  @Test
  void testQualifiedByValue() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByValueTestBean.class);
    context.refresh();
    QualifiedByValueTestBean testBean = (QualifiedByValueTestBean) context.getBean("testBean");
    Person person = testBean.getLarry();
    assertThat(person.getName()).isEqualTo("Larry");
  }

  @Test
  void testQualifiedByParentValue() {
    StaticApplicationContext parent = new StaticApplicationContext();
    GenericBeanDefinition parentLarry = new GenericBeanDefinition();
    parentLarry.setBeanClass(Person.class);
    parentLarry.getPropertyValues().add("name", "ParentLarry");
    parentLarry.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "parentLarry"));
    parent.registerBeanDefinition("someLarry", parentLarry);
    GenericBeanDefinition otherLarry = new GenericBeanDefinition();
    otherLarry.setBeanClass(Person.class);
    otherLarry.getPropertyValues().add("name", "OtherLarry");
    otherLarry.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "otherLarry"));
    parent.registerBeanDefinition("someOtherLarry", otherLarry);
    parent.refresh();

    StaticApplicationContext context = new StaticApplicationContext(parent);
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByParentValueTestBean.class);
    context.refresh();
    QualifiedByParentValueTestBean testBean = (QualifiedByParentValueTestBean) context.getBean("testBean");
    Person person = testBean.getLarry();
    assertThat(person.getName()).isEqualTo("ParentLarry");
  }

  @Test
  void testQualifiedByBeanName() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByBeanNameTestBean.class);
    context.refresh();
    QualifiedByBeanNameTestBean testBean = (QualifiedByBeanNameTestBean) context.getBean("testBean");
    Person person = testBean.getLarry();
    assertThat(person.getName()).isEqualTo("LarryBean");
    assertThat(testBean.myProps).isNotNull();
    assertThat(testBean.myProps).isEmpty();
  }

  @Test
  void testQualifiedByFieldName() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByFieldNameTestBean.class);
    context.refresh();
    QualifiedByFieldNameTestBean testBean = (QualifiedByFieldNameTestBean) context.getBean("testBean");
    Person person = testBean.getLarry();
    assertThat(person.getName()).isEqualTo("LarryBean");
  }

  @Test
  void testQualifiedByParameterName() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByParameterNameTestBean.class);
    context.refresh();
    QualifiedByParameterNameTestBean testBean = (QualifiedByParameterNameTestBean) context.getBean("testBean");
    Person person = testBean.getLarry();
    assertThat(person.getName()).isEqualTo("LarryBean");
  }

  @Test
  void testQualifiedByAlias() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByAliasTestBean.class);
    context.refresh();
    QualifiedByAliasTestBean testBean = (QualifiedByAliasTestBean) context.getBean("testBean");
    Person person = testBean.getStooge();
    assertThat(person.getName()).isEqualTo("LarryBean");
  }

  @Test
  void testQualifiedByAnnotation() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByAnnotationTestBean.class);
    context.refresh();
    QualifiedByAnnotationTestBean testBean = (QualifiedByAnnotationTestBean) context.getBean("testBean");
    Person person = testBean.getLarry();
    assertThat(person.getName()).isEqualTo("LarrySpecial");
  }

  @Test
  void testQualifiedByCustomValue() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByCustomValueTestBean.class);
    context.refresh();
    QualifiedByCustomValueTestBean testBean = (QualifiedByCustomValueTestBean) context.getBean("testBean");
    Person person = testBean.getCurly();
    assertThat(person.getName()).isEqualTo("Curly");
  }

  @Test
  void testQualifiedByAnnotationValue() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByAnnotationValueTestBean.class);
    context.refresh();
    QualifiedByAnnotationValueTestBean testBean = (QualifiedByAnnotationValueTestBean) context.getBean("testBean");
    Person person = testBean.getLarry();
    assertThat(person.getName()).isEqualTo("LarrySpecial");
  }

  @Test
  void testQualifiedByAttributesFailsWithoutCustomQualifierRegistered() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    context.registerSingleton("testBean", QualifiedByAttributesTestBean.class);

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(context::refresh)
            .withMessageContaining("found 6");
  }

  @Test
  void testQualifiedByAttributesWithCustomQualifierRegistered() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
    QualifierAnnotationAutowireCandidateResolver resolver = (QualifierAnnotationAutowireCandidateResolver)
            context.getBeanFactory().getAutowireCandidateResolver();
    resolver.addQualifierType(MultipleAttributeQualifier.class);
    context.registerSingleton("testBean", MultiQualifierClient.class);
    context.refresh();

    MultiQualifierClient testBean = (MultiQualifierClient) context.getBean("testBean");

    assertThat(testBean.factoryTheta).isNotNull();
    assertThat(testBean.implTheta).isNotNull();
  }

  @Test
  void testInterfaceWithOneQualifiedFactoryAndOneQualifiedBean() {
    StaticApplicationContext context = new StaticApplicationContext();
    BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
    reader.loadBeanDefinitions(CONFIG_LOCATION);
  }

  @SuppressWarnings("unused")
  private static class NonQualifiedTestBean {

    @Autowired
    private Person anonymous;

    public Person getAnonymous() {
      return anonymous;
    }
  }

  private static class QualifiedByValueTestBean {

    @Autowired
    @Qualifier("larry")
    private Person larry;

    public Person getLarry() {
      return larry;
    }
  }

  private static class QualifiedByParentValueTestBean {

    @Autowired
    @Qualifier("parentLarry")
    private Person larry;

    public Person getLarry() {
      return larry;
    }
  }

  private static class QualifiedByBeanNameTestBean {

    @Autowired
    @Qualifier("larryBean")
    private Person larry;

    @Autowired
    @Qualifier("testProperties")
    public Properties myProps;

    public Person getLarry() {
      return larry;
    }
  }

  private static class QualifiedByFieldNameTestBean {

    @Autowired
    private Person larryBean;

    public Person getLarry() {
      return larryBean;
    }
  }

  private static class QualifiedByParameterNameTestBean {

    private Person larryBean;

    @Autowired
    public void setLarryBean(Person larryBean) {
      this.larryBean = larryBean;
    }

    public Person getLarry() {
      return larryBean;
    }
  }

  private static class QualifiedByAliasTestBean {

    @Autowired
    @Qualifier("stooge")
    private Person stooge;

    public Person getStooge() {
      return stooge;
    }
  }

  private static class QualifiedByAnnotationTestBean {

    @Autowired
    @Qualifier("special")
    private Person larry;

    public Person getLarry() {
      return larry;
    }
  }

  private static class QualifiedByCustomValueTestBean {

    @Autowired
    @SimpleValueQualifier("curly")
    private Person curly;

    public Person getCurly() {
      return curly;
    }
  }

  private static class QualifiedByAnnotationValueTestBean {

    @Autowired
    @SimpleValueQualifier("special")
    private Person larry;

    public Person getLarry() {
      return larry;
    }
  }

  @SuppressWarnings("unused")
  private static class QualifiedByAttributesTestBean {

    @Autowired
    @MultipleAttributeQualifier(name = "moe", age = 42)
    private Person moeSenior;

    @Autowired
    @MultipleAttributeQualifier(name = "moe", age = 15)
    private Person moeJunior;

    public Person getMoeSenior() {
      return moeSenior;
    }

    public Person getMoeJunior() {
      return moeJunior;
    }
  }

  @SuppressWarnings("unused")
  private static class Person {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Qualifier("special")
  @SimpleValueQualifier("special")
  private static class SpecialPerson extends Person {
  }

  @Target({ ElementType.FIELD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface SimpleValueQualifier {

    String value() default "";
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MultipleAttributeQualifier {

    String name();

    int age();
  }

  private static final String FACTORY_QUALIFIER = "FACTORY";

  private static final String IMPL_QUALIFIER = "IMPL";

  public static class MultiQualifierClient {

    @Autowired
    @Qualifier(FACTORY_QUALIFIER)
    public Theta factoryTheta;

    @Autowired
    @Qualifier(IMPL_QUALIFIER)
    public Theta implTheta;
  }

  public interface Theta {
  }

  @Qualifier(IMPL_QUALIFIER)
  public static class ThetaImpl implements Theta {
  }

  @Qualifier(FACTORY_QUALIFIER)
  public static class QualifiedFactoryBean implements FactoryBean<Theta> {

    @Override
    public Theta getObject() {
      return new Theta() { };
    }

    @Override
    public Class<Theta> getObjectType() {
      return Theta.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

}

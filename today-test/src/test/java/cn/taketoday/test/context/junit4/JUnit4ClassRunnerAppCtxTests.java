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

package cn.taketoday.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.GenericXmlContextLoader;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpringJUnit4ClassRunnerAppCtxTests serves as a <em>proof of concept</em>
 * JUnit 4 based test class, which verifies the expected functionality of
 * {@link InfraRunner} in conjunction with the following:
 *
 * <ul>
 * <li>{@link ContextConfiguration @ContextConfiguration}</li>
 * <li>{@link Autowired @Autowired}</li>
 * <li>{@link Qualifier @Qualifier}</li>
 * <li>{@link Resource @Resource}</li>
 * <li>{@link Value @Value}</li>
 * <li>{@link Inject @Inject}</li>
 * <li>{@link Named @Named}</li>
 * <li>{@link ApplicationContextAware}</li>
 * <li>{@link BeanNameAware}</li>
 * <li>{@link InitializingBean}</li>
 * </ul>
 *
 * <p>Since no application context resource
 * {@link ContextConfiguration#locations() locations} are explicitly declared
 * and since the {@link ContextConfiguration#loader() ContextLoader} is left set
 * to the default value of {@link GenericXmlContextLoader}, this test class's
 * dependencies will be injected via {@link Autowired @Autowired},
 * {@link Inject @Inject}, and {@link Resource @Resource} from beans defined in
 * the {@link ApplicationContext} loaded from the default classpath resource:
 * {@value #DEFAULT_CONTEXT_RESOURCE_PATH}.
 *
 * @author Sam Brannen
 * @see AbsolutePathJUnit4ClassRunnerAppCtxTests
 * @see RelativePathJUnit4ClassRunnerAppCtxTests
 * @see InheritedConfigJUnit4ClassRunnerAppCtxTests
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class JUnit4ClassRunnerAppCtxTests implements ApplicationContextAware, BeanNameAware, InitializingBean {

  /**
   * Default resource path for the application context configuration for
   * {@link JUnit4ClassRunnerAppCtxTests}: {@value #DEFAULT_CONTEXT_RESOURCE_PATH}
   */
  public static final String DEFAULT_CONTEXT_RESOURCE_PATH =
          "/cn/taketoday/test/context/junit4/JUnit4ClassRunnerAppCtxTests-context.xml";

  private Employee employee;

  @Autowired
  private Pet autowiredPet;

  @Inject
  private Pet injectedPet;

  @Autowired(required = false)
  protected Long nonrequiredLong;

  @Resource
  protected String foo;

  protected String bar;

  @Value("enigma")
  private String literalFieldValue;

  @Value("#{2 == (1+1)}")
  private Boolean spelFieldValue;

  private String literalParameterValue;

  private Boolean spelParameterValue;

  @Autowired
  @Qualifier("quux")
  protected String quux;

  @Inject
  @Named("quux")
  protected String namedQuux;

  private String beanName;

  private ApplicationContext applicationContext;

  private boolean beanInitialized = false;

  @Autowired
  protected void setEmployee(Employee employee) {
    this.employee = employee;
  }

  @Resource
  protected void setBar(String bar) {
    this.bar = bar;
  }

  @Autowired
  public void setLiteralParameterValue(@Value("enigma") String literalParameterValue) {
    this.literalParameterValue = literalParameterValue;
  }

  @Autowired
  public void setSpelParameterValue(@Value("#{2 == (1+1)}") Boolean spelParameterValue) {
    this.spelParameterValue = spelParameterValue;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() {
    this.beanInitialized = true;
  }

  @Test
  public void verifyBeanNameSet() {
    assertThat(this.beanName.startsWith(getClass().getName())).as("The bean name of this test instance should have been set due to BeanNameAware semantics.").isTrue();
  }

  @Test
  public void verifyApplicationContextSet() {
    assertThat(this.applicationContext).as("The application context should have been set due to ApplicationContextAware semantics.").isNotNull();
  }

  @Test
  public void verifyBeanInitialized() {
    assertThat(this.beanInitialized).as("This test bean should have been initialized due to InitializingBean semantics.").isTrue();
  }

  @Test
  public void verifyAnnotationAutowiredAndInjectedFields() {
    assertThat(this.nonrequiredLong).as("The nonrequiredLong field should NOT have been autowired.").isNull();
    assertThat(this.quux).as("The quux field should have been autowired via @Autowired and @Qualifier.").isEqualTo("Quux");
    assertThat(this.namedQuux).as("The namedFoo field should have been injected via @Inject and @Named.").isEqualTo("Quux");
    assertThat(this.namedQuux).as("@Autowired/@Qualifier and @Inject/@Named quux should be the same object.").isSameAs(this.quux);

    assertThat(this.autowiredPet).as("The pet field should have been autowired.").isNotNull();
    assertThat(this.injectedPet).as("The pet field should have been injected.").isNotNull();
    assertThat(this.autowiredPet.getName()).isEqualTo("Fido");
    assertThat(this.injectedPet.getName()).isEqualTo("Fido");
    assertThat(this.injectedPet).as("@Autowired and @Inject pet should be the same object.").isSameAs(this.autowiredPet);
  }

  @Test
  public void verifyAnnotationAutowiredMethods() {
    assertThat(this.employee).as("The employee setter method should have been autowired.").isNotNull();
    assertThat(this.employee.getName()).isEqualTo("John Smith");
  }

  @Test
  public void verifyAutowiredAtValueFields() {
    assertThat(this.literalFieldValue).as("Literal @Value field should have been autowired").isNotNull();
    assertThat(this.spelFieldValue).as("SpEL @Value field should have been autowired.").isNotNull();
    assertThat(this.literalFieldValue).isEqualTo("enigma");
    assertThat(this.spelFieldValue).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void verifyAutowiredAtValueMethods() {
    assertThat(this.literalParameterValue).as("Literal @Value method parameter should have been autowired.").isNotNull();
    assertThat(this.spelParameterValue).as("SpEL @Value method parameter should have been autowired.").isNotNull();
    assertThat(this.literalParameterValue).isEqualTo("enigma");
    assertThat(this.spelParameterValue).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void verifyResourceAnnotationInjectedFields() {
    assertThat(this.foo).as("The foo field should have been injected via @Resource.").isEqualTo("Foo");
  }

  @Test
  public void verifyResourceAnnotationInjectedMethods() {
    assertThat(this.bar).as("The bar method should have been wired via @Resource.").isEqualTo("Bar");
  }

}

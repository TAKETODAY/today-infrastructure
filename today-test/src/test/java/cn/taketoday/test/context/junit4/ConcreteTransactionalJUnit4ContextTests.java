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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.annotation.Propagation;
import cn.taketoday.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

import static cn.taketoday.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Combined integration test for {@link AbstractJUnit4ContextTests} and
 * {@link AbstractTransactionalJUnit4ContextTests}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class ConcreteTransactionalJUnit4ContextTests extends AbstractTransactionalJUnit4ContextTests
        implements BeanNameAware, InitializingBean {

  private static final String JANE = "jane";
  private static final String SUE = "sue";
  private static final String YODA = "yoda";

  private Employee employee;

  @Autowired
  private Pet pet;

  @Autowired(required = false)
  private Long nonrequiredLong;

  @Resource
  private String foo;

  private String bar;

  private String beanName;

  private boolean beanInitialized = false;

  @Autowired
  private void setEmployee(Employee employee) {
    this.employee = employee;
  }

  @Resource
  private void setBar(String bar) {
    this.bar = bar;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  @Override
  public void afterPropertiesSet() {
    this.beanInitialized = true;
  }

  @Before
  public void setUp() {
    long expected = (isActualTransactionActive() ? 2 : 1);
    assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table before a test method.").isEqualTo(expected);
  }

  @After
  public void tearDown() {
    long expected = (isActualTransactionActive() ? 4 : 1);
    assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table after a test method.").isEqualTo(expected);
  }

  @BeforeTransaction
  public void beforeTransaction() {
    assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table before a transactional test method.").isEqualTo(1);
    assertThat(addPerson(YODA)).as("Adding yoda").isEqualTo(1);
  }

  @AfterTransaction
  public void afterTransaction() {
    assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);
    assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table after a transactional test method.").isEqualTo(1);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyBeanNameSet() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(this.beanName.startsWith(getClass().getName())).as("The bean name of this test instance should have been set to the fully qualified class name " +
            "due to BeanNameAware semantics.").isTrue();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyApplicationContext() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(super.applicationContext).as("The application context should have been set due to ApplicationContextAware semantics.").isNotNull();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyBeanInitialized() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(this.beanInitialized).as("This test bean should have been initialized due to InitializingBean semantics.").isTrue();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyAnnotationAutowiredFields() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(this.nonrequiredLong).as("The nonrequiredLong property should NOT have been autowired.").isNull();
    assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();
    assertThat(this.pet.getName()).isEqualTo("Fido");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyAnnotationAutowiredMethods() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(this.employee).as("The employee setter method should have been autowired.").isNotNull();
    assertThat(this.employee.getName()).isEqualTo("John Smith");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyResourceAnnotationWiredFields() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(this.foo).as("The foo field should have been wired via @Resource.").isEqualTo("Foo");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyResourceAnnotationWiredMethods() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(this.bar).as("The bar method should have been wired via @Resource.").isEqualTo("Bar");
  }

  @Test
  public void modifyTestDataWithinTransaction() {
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(addPerson(JANE)).as("Adding jane").isEqualTo(1);
    assertThat(addPerson(SUE)).as("Adding sue").isEqualTo(1);
    assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table in modifyTestDataWithinTransaction().").isEqualTo(4);
  }

  private int addPerson(String name) {
    return super.jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
  }

  private int deletePerson(String name) {
    return super.jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
  }

  private int countRowsInPersonTable() {
    return countRowsInTable("person");
  }

}

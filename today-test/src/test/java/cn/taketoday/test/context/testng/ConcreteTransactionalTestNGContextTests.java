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

package cn.taketoday.test.context.testng;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
 * Combined integration test for {@link AbstractTestNGContextTests} and
 * {@link AbstractTransactionalTestNGContextTests}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class ConcreteTransactionalTestNGContextTests extends AbstractTransactionalTestNGContextTests
        implements BeanNameAware, InitializingBean {

  private static final String JANE = "jane";
  private static final String SUE = "sue";
  private static final String YODA = "yoda";

  private static final int NUM_TESTS = 8;
  private static final int NUM_TX_TESTS = 1;

  private static int numSetUpCalls = 0;
  private static int numSetUpCallsInTransaction = 0;
  private static int numTearDownCalls = 0;
  private static int numTearDownCallsInTransaction = 0;

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

  @BeforeClass
  void beforeClass() {
    numSetUpCalls = 0;
    numSetUpCallsInTransaction = 0;
    numTearDownCalls = 0;
    numTearDownCallsInTransaction = 0;
  }

  @AfterClass
  void afterClass() {
    assertThat(numSetUpCalls).as("number of calls to setUp().").isEqualTo(NUM_TESTS);
    assertThat(numSetUpCallsInTransaction).as("number of calls to setUp() within a transaction.").isEqualTo(NUM_TX_TESTS);
    assertThat(numTearDownCalls).as("number of calls to tearDown().").isEqualTo(NUM_TESTS);
    assertThat(numTearDownCallsInTransaction).as("number of calls to tearDown() within a transaction.").isEqualTo(NUM_TX_TESTS);
  }

  @BeforeMethod
  void setUp() {
    numSetUpCalls++;
    if (isActualTransactionActive()) {
      numSetUpCallsInTransaction++;
    }
    assertNumRowsInPersonTable((isActualTransactionActive() ? 2 : 1), "before a test method");
  }

  @AfterMethod
  void tearDown() {
    numTearDownCalls++;
    if (isActualTransactionActive()) {
      numTearDownCallsInTransaction++;
    }
    assertNumRowsInPersonTable((isActualTransactionActive() ? 4 : 1), "after a test method");
  }

  @BeforeTransaction
  void beforeTransaction() {
    assertNumRowsInPersonTable(1, "before a transactional test method");
    assertAddPerson(YODA);
  }

  @AfterTransaction
  void afterTransaction() {
    assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);
    assertNumRowsInPersonTable(1, "after a transactional test method");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyBeanNameSet() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(this.beanName)
            .as("The bean name of this test instance should have been set to the fully qualified class name due to BeanNameAware semantics.")
            .startsWith(getClass().getName());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyApplicationContextSet() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(super.applicationContext)
            .as("The application context should have been set due to ApplicationContextAware semantics.")
            .isNotNull();
    Employee employeeBean = (Employee) super.applicationContext.getBean("employee");
    assertThat(employeeBean.getName()).as("employee's name.").isEqualTo("John Smith");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyBeanInitialized() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(beanInitialized)
            .as("This test instance should have been initialized due to InitializingBean semantics.")
            .isTrue();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyAnnotationAutowiredFields() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(nonrequiredLong).as("The nonrequiredLong field should NOT have been autowired.").isNull();
    assertThat(pet).as("The pet field should have been autowired.").isNotNull();
    assertThat(pet.getName()).as("pet's name.").isEqualTo("Fido");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyAnnotationAutowiredMethods() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(employee).as("The setEmployee() method should have been autowired.").isNotNull();
    assertThat(employee.getName()).as("employee's name.").isEqualTo("John Smith");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyResourceAnnotationInjectedFields() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(foo).as("The foo field should have been injected via @Resource.").isEqualTo("Foo");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void verifyResourceAnnotationInjectedMethods() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(bar).as("The setBar() method should have been injected via @Resource.").isEqualTo("Bar");
  }

  @Test
  public void modifyTestDataWithinTransaction() {
    TransactionAssert.assertThatTransaction().isActive();
    assertAddPerson(JANE);
    assertAddPerson(SUE);
    assertNumRowsInPersonTable(4, "in modifyTestDataWithinTransaction()");
  }

  private int createPerson(String name) {
    return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
  }

  private int deletePerson(String name) {
    return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
  }

  private void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
    assertThat(countRowsInTable("person"))
            .as("the number of rows in the person table (" + testState + ").")
            .isEqualTo(expectedNumRows);
  }

  private void assertAddPerson(String name) {
    assertThat(createPerson(name)).as("Adding '" + name + "'").isEqualTo(1);
  }

}

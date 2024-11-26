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

package infra.test.context.junit4.spr4868;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestExecutionListeners;
import infra.test.context.junit4.JUnit4ClassRunner;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that investigate the applicability of JSR-250 lifecycle
 * annotations in test classes.
 *
 * <p>This class does not really contain actual <em>tests</em> per se. Rather it
 * can be used to empirically verify the expected log output (see below). In
 * order to see the log output, one would naturally need to ensure that the
 * logger category for this class is enabled at {@code INFO} level.
 *
 * <h4>Expected Log Output</h4>
 * <pre>
 * INFO : infra.test.context.junit4.spr4868.LifecycleBean - initializing
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - beforeAllTests()
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - setUp()
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - test1()
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - tearDown()
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - beforeAllTests()
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - setUp()
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - test2()
 * INFO : infra.test.context.junit4.spr4868.ExampleTest - tearDown()
 * INFO : infra.test.context.junit4.spr4868.LifecycleBean - destroying
 * </pre>
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@ContextConfiguration
public class Jsr250LifecycleTests {

  private final Logger logger = LoggerFactory.getLogger(Jsr250LifecycleTests.class);

  @Configuration
  static class Config {

    @Bean
    public LifecycleBean lifecycleBean() {
      return new LifecycleBean();
    }
  }

  @Autowired
  private LifecycleBean lifecycleBean;

  @PostConstruct
  public void beforeAllTests() {
    logger.info("beforeAllTests()");
  }

  @PreDestroy
  public void afterTestSuite() {
    logger.info("afterTestSuite()");
  }

  @Before
  public void setUp() throws Exception {
    logger.info("setUp()");
  }

  @After
  public void tearDown() throws Exception {
    logger.info("tearDown()");
  }

  @Test
  public void test1() {
    logger.info("test1()");
    assertThat(lifecycleBean).isNotNull();
  }

  @Test
  public void test2() {
    logger.info("test2()");
    assertThat(lifecycleBean).isNotNull();
  }

}

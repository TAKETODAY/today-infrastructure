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

package cn.taketoday.jmx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import java.net.BindException;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.jmx.export.MBeanExporter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>If you run into the <em>"Unsupported protocol: jmxmp"</em> error, you will need to
 * download the <a href="https://www.oracle.com/technetwork/java/javase/tech/download-jsp-141676.html">JMX
 * Remote API 1.0.1_04 Reference Implementation</a> from Oracle and extract
 * {@code jmxremote_optional.jar} into your classpath, for example in the {@code lib/ext}
 * folder of your JVM.
 *
 * <p>See also:
 * <ul>
 * <li><a href="https://jira.spring.io/browse/SPR-8093">SPR-8093</a></li>
 * <li><a href="https://issuetracker.springsource.com/browse/EBR-349">EBR-349</a></li>
 * </ul>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Stephane Nicoll
 */
@ExtendWith(AbstractMBeanServerTests.BindExceptionHandler.class)
public abstract class AbstractMBeanServerTests {

  protected MBeanServer server;

  @BeforeEach
  public final void setUp() throws Exception {
    this.server = MBeanServerFactory.createMBeanServer();
    try {
      onSetUp();
    }
    catch (Exception ex) {
      releaseServer();
      throw ex;
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    releaseServer();
    onTearDown();
  }

  private void releaseServer() throws Exception {
    try {
      MBeanServerFactory.releaseMBeanServer(getServer());
    }
    catch (IllegalArgumentException ex) {
      if (!ex.getMessage().contains("not in list")) {
        throw ex;
      }
    }
    MBeanTestUtils.resetMBeanServers();
  }

  protected final ConfigurableApplicationContext loadContext(String configLocation) {
    GenericApplicationContext ctx = new GenericApplicationContext();
    new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(configLocation);
    ctx.getBeanFactory().registerSingleton("server", getServer());
    ctx.refresh();
    return ctx;
  }

  protected void onSetUp() throws Exception {
  }

  protected void onTearDown() throws Exception {
  }

  protected final MBeanServer getServer() {
    return this.server;
  }

  /**
   * Start the specified {@link MBeanExporter}.
   */
  protected void start(MBeanExporter exporter) {
    exporter.afterPropertiesSet();
    exporter.afterSingletonsInstantiated();
  }

  protected void assertIsRegistered(String message, ObjectName objectName) {
    assertThat(getServer().isRegistered(objectName)).as(message).isTrue();
  }

  protected void assertIsNotRegistered(String message, ObjectName objectName) {
    assertThat(getServer().isRegistered(objectName)).as(message).isFalse();
  }

  static class BindExceptionHandler implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
      handleBindException(throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
      handleBindException(throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
      handleBindException(throwable);
    }

    private void handleBindException(Throwable throwable) throws Throwable {
      // Abort test?
      if (throwable instanceof BindException) {
        throw new TestAbortedException("Failed to bind to MBeanServer", throwable);
      }
      // Else rethrow to conform to the contracts of TestExecutionExceptionHandler and LifecycleMethodExecutionExceptionHandler
      throw throwable;
    }

  }

}


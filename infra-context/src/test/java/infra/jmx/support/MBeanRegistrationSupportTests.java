/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jmx.support;

import org.junit.jupiter.api.Test;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import infra.logging.Logger;
import infra.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class MBeanRegistrationSupportTests {

  @Test
  void reportsOriginalConflictWhenExistingMBeanVanishesDuringReplacement() throws Exception {
    Logger log = mock(Logger.class);
    given(log.isDebugEnabled()).willReturn(true);
    given(log.isInfoEnabled()).willReturn(true);
    MBeanRegistrationSupport support = registrationSupport(log);
    MBeanServer server = mock(MBeanServer.class);
    support.setServer(server);
    assertThat(support.getServer()).isSameAs(server);
    ObjectName name = ObjectName.getInstance("infra:type=Race");
    Object resource = new Object();
    InstanceAlreadyExistsException conflict = new InstanceAlreadyExistsException(name.toString());
    given(server.registerMBean(resource, name)).willThrow(conflict);
    willThrow(new InstanceNotFoundException(name.toString())).given(server).unregisterMBean(name);
    support.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING);

    assertThatExceptionOfType(InstanceAlreadyExistsException.class)
            .isThrownBy(() -> support.doRegister(resource, name)).isSameAs(conflict);
    assertThat(support.getRegisteredObjectNames()).isEmpty();
    verify(log).debug("Replacing existing MBean at [{}]", name);

    support.setRegistrationPolicy(RegistrationPolicy.IGNORE_EXISTING);
    support.doRegister(resource, name);
    assertThat(support.getRegisteredObjectNames()).containsExactly(name);
    verify(log).debug("Ignoring existing MBean at [{}]", name);
  }

  @Test
  void continuesCleanupAfterRegistrationDisappearsOrUnregisterFails() throws Exception {
    Logger log = mock(Logger.class);
    given(log.isInfoEnabled()).willReturn(true);
    MBeanRegistrationSupport support = registrationSupport(log);
    MBeanServer server = mock(MBeanServer.class);
    support.setServer(server);
    Object resource = new Object();
    ObjectName missing = ObjectName.getInstance("infra:type=Missing");
    ObjectName failed = ObjectName.getInstance("infra:type=Failed");
    ObjectName healthy = ObjectName.getInstance("infra:type=Healthy");
    for (ObjectName name : new ObjectName[] { missing, failed, healthy }) {
      given(server.registerMBean(resource, name)).willReturn(new ObjectInstance(name, Object.class.getName()));
      support.doRegister(resource, name);
    }
    given(server.isRegistered(failed)).willReturn(true);
    given(server.isRegistered(healthy)).willReturn(true);
    MBeanRegistrationException failure = new MBeanRegistrationException(new IllegalStateException());
    willThrow(failure).given(server).unregisterMBean(failed);

    support.unregisterBeans();

    assertThat(support.getRegisteredObjectNames()).isEmpty();
    verify(server).unregisterMBean(healthy);
    verify(log).info("Could not unregister MBean [{}]", failed, failure);
  }

  @Test
  void requiresServerBeforeRegistrationOrRemoval() throws Exception {
    MBeanRegistrationSupport support = registrationSupport(mock(Logger.class));
    ObjectName name = ObjectName.getInstance("infra:type=MissingServer");

    assertThatIllegalStateException().isThrownBy(() -> support.doRegister(new Object(), name))
            .withMessage("No MBeanServer set");
    assertThatIllegalStateException().isThrownBy(() -> support.doUnregister(name)).withMessage("No MBeanServer set");
  }

  private static MBeanRegistrationSupport registrationSupport(Logger log) {
    try (var loggers = mockStatic(LoggerFactory.class)) {
      loggers.when(() -> LoggerFactory.getLogger(MBeanRegistrationSupport.class)).thenReturn(log);
      return new MBeanRegistrationSupport();
    }
  }
}

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

package infra.jmx.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardEmitterMBean;
import javax.management.modelmbean.RequiredModelMBean;

import infra.aop.framework.ProxyFactory;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.jmx.export.annotation.AnnotationMBeanExporter;
import infra.jmx.export.annotation.ManagedAttribute;
import infra.jmx.export.annotation.ManagedResource;
import infra.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class MBeanExporterLifecycleTests {

  private final MBeanServer server = MBeanServerFactory.newMBeanServer();

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void exportsManagementInterfaceFromJdkProxy(boolean mxBean) throws Exception {
    ObjectName name = ObjectName.getInstance("infra:type=Counter");
    Object target = mxBean ? new Sensor() : new Counter();
    Class<?> managementInterface = mxBean ? SensorMXBean.class : CounterMBean.class;
    Object proxy = proxy(target, managementInterface);
    MBeanExporter exporter = exporter();
    exporter.setBeans(Map.of(name.toString(), proxy));

    try {
      exporter.afterSingletonsInstantiated();
      assertThat(server.getAttribute(name, "Value")).isEqualTo(7);
      exporter.unregisterManagedResource(name);
      assertThat(server.isRegistered(name)).isFalse();
    }
    finally {
      exporter.destroy();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void rejectsJdkProxyThatHidesItsManagementInterface(boolean mxBean) {
    Object target = mxBean ? new Sensor() : new Counter();
    MBeanExporter exporter = exporter();
    exporter.setBeans(Map.of("infra:type=Hidden", proxy(target, Runnable.class)));

    assertThatExceptionOfType(UnableToRegisterMBeanException.class).isThrownBy(exporter::afterSingletonsInstantiated)
            .withCauseInstanceOf(NotCompliantMBeanException.class)
            .withStackTraceContaining("does not expose it in the proxy");
  }

  @Test
  void usesRequiredModelMBeanWhenResourceClassLoaderExposureIsDisabled() throws Exception {
    ObjectName name = ObjectName.getInstance("infra:type=Model");
    MBeanExporter exporter = exporter();
    exporter.setExposeManagedResourceClassLoader(false);

    try {
      assertThat(exporter.createModelMBean()).isExactlyInstanceOf(RequiredModelMBean.class);
      exporter.registerManagedResource(new SensorResource(), name);
      assertThat(server.getAttribute(name, "Value")).isEqualTo(7);
    }
    finally {
      exporter.destroy();
    }
  }

  @Test
  void rollsBackBeanRegistrationWhenListenerTargetDoesNotExist() throws Exception {
    ObjectName name = ObjectName.getInstance("infra:type=Counter");
    MBeanExporter exporter = exporter();
    exporter.setBeans(Map.of(name.toString(), new Counter()));
    exporter.setNotificationListenerMappings(Map.of("infra:type=Missing", (notification, handback) -> { }));

    assertThatExceptionOfType(MBeanExportException.class).isThrownBy(exporter::afterSingletonsInstantiated)
            .withMessage("Unable to register NotificationListener").withCauseInstanceOf(InstanceNotFoundException.class);
    assertThat(server.isRegistered(name)).isFalse();
  }

  @Test
  void removesTrackingAfterAnExternalUnregistration() throws Exception {
    ObjectName name = ObjectName.getInstance("infra:type=Counter");
    MBeanExporter exporter = exporter();
    NotificationListener listener = (notification, handback) -> { };
    exporter.setNotificationListenerMappings(Collections.singletonMap(null, listener));
    exporter.setBeans(Map.of(name.toString(), new StandardEmitterMBean(
            new Counter(), CounterMBean.class, new NotificationBroadcasterSupport())));
    exporter.afterSingletonsInstantiated();
    server.unregisterMBean(name);
    exporter.destroy();

    server.registerMBean(new Counter(), name);
    exporter.destroy();
    assertThat(server.isRegistered(name)).isTrue();
  }

  @Test
  void reportsNamingAndModelAssemblyFailures() throws Exception {
    MBeanExporter exporter = exporter();
    MalformedObjectNameException invalid = new MalformedObjectNameException("bad name");
    exporter.setNamingStrategy((bean, key) -> { throw invalid; });
    assertThatExceptionOfType(MBeanExportException.class).isThrownBy(() -> exporter.registerManagedResource(new Counter()))
            .withMessageContaining("Unable to generate ObjectName").withCause(invalid);

    IllegalArgumentException assemblyFailure = new IllegalArgumentException("bad metadata");
    exporter.setAssembler((bean, key) -> { throw assemblyFailure; });
    ObjectName name = ObjectName.getInstance("infra:type=Model");
    assertThatExceptionOfType(MBeanExportException.class)
            .isThrownBy(() -> exporter.registerManagedResource(new Object(), name))
            .withMessageContaining("Could not create ModelMBean").withCause(assemblyFailure);
    assertThat(server.isRegistered(name)).isFalse();
  }

  @Test
  void detectsLazyStandardMBeanWithoutInstantiatingItUntilInvoked() throws Exception {
    ObjectName name = ObjectName.getInstance("infra:type=Lazy");
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition definition = new RootBeanDefinition(Counter.class);
    definition.setLazyInit(true);
    beanFactory.registerBeanDefinition(name.toString(), definition);
    MBeanExporter exporter = exporter();
    exporter.setBeanFactory(beanFactory);
    try {
      exporter.afterSingletonsInstantiated();
      assertThat(server.isRegistered(name)).isTrue();
      assertThat(beanFactory.containsSingleton(name.toString())).isFalse();
      assertThat(server.getAttribute(name, "Value")).isEqualTo(7);
      assertThat(beanFactory.containsSingleton(name.toString())).isTrue();
    }
    finally {
      exporter.destroy();
    }
  }

  @Test
  void reportsAutodetectionDecisionsAndResourcesWithoutManagementOperations() throws Exception {
    Logger log = mock(Logger.class);
    given(log.isDebugEnabled()).willReturn(true);
    given(log.isInfoEnabled()).willReturn(true);
    given(log.isTraceEnabled()).willReturn(true);
    MBeanExporter exporter;
    try (var loggers = mockStatic(LoggerFactory.class, CALLS_REAL_METHODS)) {
      loggers.when(() -> LoggerFactory.getLogger(MBeanExporter.class)).thenReturn(log);
      exporter = exporter();
    }
    ObjectName explicit = ObjectName.getInstance("infra:type=Explicit");
    ObjectName detected = ObjectName.getInstance("infra:type=Detected");
    ObjectName empty = ObjectName.getInstance("infra:type=Empty");
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    Counter counter = new Counter();
    beanFactory.registerSingleton(explicit.toString(), counter);
    beanFactory.registerSingleton(detected.toString(), new Counter());
    exporter.setBeans(new LinkedHashMap<>(Map.of(explicit.toString(), counter, empty.toString(), new Object())));
    exporter.setBeanFactory(beanFactory);
    exporter.setAutodetect(true);

    try {
      exporter.afterSingletonsInstantiated();
      assertThat(server.getAttribute(detected, "Value")).isEqualTo(7);
      assertThat(server.getMBeanInfo(empty).getOperations()).isEmpty();
      verify(log).debug("Bean with name '{}' has been autodetected for JMX exposure", detected.toString());
      verify(log).trace("Bean with name '{}' is already registered for JMX exposure", explicit.toString());
      verify(log).info("Bean with key '{}' has been registered as an MBean but has no exposed attributes or operations",
              empty.toString());
    }
    finally {
      exporter.destroy();
    }
  }

  @Test
  void bulkAttributesUseTheManagedClassLoaderAndRestoreTheCallingLoader() throws Exception {
    LoaderAwareResource resource = new LoaderAwareResource();
    var assembler = new SimpleReflectiveMBeanInfoAssembler();
    InfraModelMBean mbean = new InfraModelMBean(assembler.getMBeanInfo(resource, "resource"));
    mbean.setManagedResource(resource, "ObjectReference");
    Thread thread = Thread.currentThread();
    ClassLoader original = thread.getContextClassLoader();
    ClassLoader caller = new ClassLoader(original) { };
    ClassLoader managed = resource.getClass().getClassLoader();
    try {
      thread.setContextClassLoader(caller);
      AttributeList updates = new AttributeList();
      updates.add(new Attribute("Value", 12));
      assertThat(mbean.setAttributes(updates).asList()).containsExactly(new Attribute("Value", 12));
      assertThat(resource.setterLoader).isSameAs(managed);
      assertThat(thread.getContextClassLoader()).isSameAs(caller);

      assertThat(mbean.getAttributes(new String[] { "Value" }).asList()).containsExactly(new Attribute("Value", 12));
      assertThat(resource.getterLoader).isSameAs(managed);
      assertThat(thread.getContextClassLoader()).isSameAs(caller);
    }
    finally {
      thread.setContextClassLoader(original);
    }
  }

  @Test
  void annotationExporterUsesConfiguredOrPackageDomainForImplicitNames() throws Exception {
    for (String domain : new String[] { null, "custom.jmx" }) {
      AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
      exporter.setServer(server);
      if (domain != null) {
        exporter.setDefaultDomain(domain);
      }
      StandardBeanFactory beanFactory = new StandardBeanFactory();
      beanFactory.registerSingleton("readings,primary", new SensorResource());
      exporter.setBeanFactory(beanFactory);

      try {
        exporter.afterSingletonsInstantiated();
        String expectedDomain = domain != null ? domain : SensorResource.class.getPackageName();
        ObjectName pattern = ObjectName.getInstance(expectedDomain + ":*");
        assertThat(server.queryNames(pattern, null)).singleElement().satisfies(name -> {
          assertThat(ObjectName.unquote(name.getKeyProperty("name"))).isEqualTo("readings,primary");
          assertThat(server.getAttribute(name, "Value")).isEqualTo(7);
        });
      }
      finally {
        exporter.destroy();
      }
    }
  }

  private MBeanExporter exporter() {
    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    return exporter;
  }

  private static Object proxy(Object target, Class<?> managementInterface) {
    ProxyFactory factory = new ProxyFactory();
    factory.setTarget(target);
    factory.setInterfaces(managementInterface);
    return factory.getProxy();
  }

  public interface CounterMBean {
    int getValue();
  }

  public static class Counter implements CounterMBean, Runnable {
    @Override
    public int getValue() {
      return 7;
    }

    @Override
    public void run() { }
  }

  public interface SensorMXBean {
    int getValue();
  }

  @ManagedResource
  public static class SensorResource {
    @ManagedAttribute
    public int getValue() {
      return 7;
    }
  }

  public static class LoaderAwareResource {
    private ClassLoader getterLoader;

    private ClassLoader setterLoader;

    private int value;

    public int getValue() {
      getterLoader = Thread.currentThread().getContextClassLoader();
      return value;
    }

    public void setValue(int value) {
      setterLoader = Thread.currentThread().getContextClassLoader();
      this.value = value;
    }
  }

  public static class Sensor implements SensorMXBean, Runnable {
    @Override
    public int getValue() {
      return 7;
    }

    @Override
    public void run() { }
  }
}

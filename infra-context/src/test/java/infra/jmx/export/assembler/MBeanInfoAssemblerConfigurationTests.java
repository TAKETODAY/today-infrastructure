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

package infra.jmx.export.assembler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanFeatureInfo;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import infra.aop.framework.ProxyFactory;
import infra.jmx.export.annotation.AnnotationJmxAttributeSource;
import infra.jmx.export.annotation.ManagedAttribute;
import infra.jmx.export.annotation.ManagedResource;
import infra.jmx.export.metadata.InvalidMetadataException;
import infra.jmx.export.metadata.ManagedNotification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class MBeanInfoAssemblerConfigurationTests {

  @Test
  void configuresAttributeCasingOperationDescriptorsAndParameterDiscovery() throws Exception {
    InterfaceBasedMBeanInfoAssembler assembler = new InterfaceBasedMBeanInfoAssembler();
    assembler.setManagedInterfaces((Class<?>[]) null);
    assembler.setUseStrictCasing(false);
    assembler.setExposeClassDescriptor(true);
    assembler.setDefaultCurrencyTimeLimit(15);
    assembler.setParameterNameDiscoverer(null);
    assembler.afterPropertiesSet();
    assembler.setManagedInterfaces(Operations.class);
    Object proxy = operationsProxy();

    ModelMBeanInfo info = assembler.getMBeanInfo(proxy, "bean");

    assertThat(info.getAttributes()).extracting(MBeanFeatureInfo::getName).containsExactly("value");
    assertThat(info.getAttribute("value").getDescriptor().getFieldValue("currencyTimeLimit")).isEqualTo("15");
    assertThat(info.getOperation("getValue").getDescriptor().getFieldValue("class")).isEqualTo(Operations.class.getName());
    MBeanOperationInfo operation = info.getOperation("calculate");
    assertThat(operation.getSignature()).hasSize(1);
    assertThat(operation.getDescriptor().getFieldValue("class")).isEqualTo(Operations.class.getName());
    assertThat(assembler.getMBeanInfo(new OperationsBean(), "bean").getOperation("calculate")
            .getDescriptor().getFieldValue("class")).isEqualTo(OperationsBean.class.getName());
  }

  @Test
  void ignoresBridgeMethodsAndOverloadsOutsideManagementInterface() throws Exception {
    InterfaceBasedMBeanInfoAssembler assembler = new InterfaceBasedMBeanInfoAssembler();
    assembler.setManagedInterfaces(Operations.class, StringReader.class);

    assertThat(assembler.getMBeanInfo(new OperationsBean(), "bean").getOperations())
            .extracting(MBeanFeatureInfo::getName).containsExactlyInAnyOrder("getValue", "calculate", "read");
  }

  @Test
  void rejectsConcreteClassesInInterfaceConfiguration() {
    InterfaceBasedMBeanInfoAssembler assembler = new InterfaceBasedMBeanInfoAssembler();
    assertThatIllegalArgumentException().isThrownBy(() -> assembler.setManagedInterfaces(String.class))
            .withMessageContaining("not an interface");
    Properties mappings = new Properties();
    mappings.setProperty("bean", String.class.getName());
    assembler.setInterfaceMappings(mappings);
    assertThatIllegalArgumentException().isThrownBy(assembler::afterPropertiesSet).withMessageContaining("bean", "no interface");
  }

  @Test
  void validatesNotificationMetadataAndUsesDefaultsForEmptyBeanKeys() throws Exception {
    InterfaceBasedMBeanInfoAssembler assembler = new InterfaceBasedMBeanInfoAssembler();
    ManagedNotification notification = new ManagedNotification();
    assertThatIllegalArgumentException().isThrownBy(() -> assembler.setNotificationInfos(new ManagedNotification[] { notification }))
            .withMessageContaining("notification type");
    notification.setNotificationType("started,finished");
    assertThatIllegalArgumentException().isThrownBy(() -> assembler.setNotificationInfos(new ManagedNotification[] { notification }))
            .withMessageContaining("notification name");
    notification.setName("lifecycle");
    assembler.setNotificationInfos(new ManagedNotification[] { notification });
    assertThat(assembler.getMBeanInfo(new OperationsBean(), "").getNotifications()).singleElement()
            .satisfies(info -> assertThat(info.getNotifTypes()).containsExactly("started", "finished"));
    for (Object invalid : List.of("invalid", List.of("invalid"))) {
      assertThatIllegalArgumentException().isThrownBy(() -> assembler.setNotificationInfoMappings(Map.of("bean", invalid)))
              .withMessageContaining("only accepts ManagedNotifications");
    }
  }

  @Test
  void requiresMetadataSourceAndRejectsUnannotatedBeansAndJdkProxies() {
    MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler();
    assertThatIllegalArgumentException().isThrownBy(assembler::afterPropertiesSet).withMessageContaining("attributeSource");
    assertThatIllegalStateException().isThrownBy(() -> assembler.includeBean(OperationsBean.class, "bean"))
            .withMessageContaining("JmxAttributeSource");
    assembler.setAttributeSource(new AnnotationJmxAttributeSource());
    assembler.afterPropertiesSet();
    assertThatExceptionOfType(InvalidMetadataException.class)
            .isThrownBy(() -> assembler.getMBeanInfo(new Object(), "bean")).withMessageContaining("No ManagedResource");
    Object proxy = operationsProxy();
    assertThatIllegalArgumentException().isThrownBy(() -> assembler.getMBeanInfo(proxy, "bean"))
            .withMessageContaining("does not support JDK dynamic proxies");
  }

  @Test
  void exposesWriteOnlyManagedAttributeWithPersistentMetadata() throws Exception {
    MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler(new AnnotationJmxAttributeSource());
    ModelMBeanInfo info = assembler.getMBeanInfo(new Sink(), "sink");

    assertThat(info.getAttribute("Value").isReadable()).isFalse();
    assertThat(info.getAttribute("Value").isWritable()).isTrue();
    assertThat(info.getAttribute("Value").getDescription()).isEqualTo("write only");
    assertThat(info.getAttribute("Value").getDescriptor().getFieldValue("currencyTimeLimit"))
            .isEqualTo(Integer.toString(Integer.MAX_VALUE));
    assertThat(info.getAttribute("Value").getDescriptor().getFieldValue("persistPolicy")).isEqualTo("OnUpdate");
  }

  public interface Operations {
    int getValue();

    int calculate(int operand);
  }

  private static Object operationsProxy() {
    ProxyFactory factory = new ProxyFactory();
    factory.setTarget(new OperationsBean());
    factory.setInterfaces(Operations.class);
    return factory.getProxy();
  }

  public interface Reader<T> {
    T read();
  }

  public interface StringReader extends Reader<String> {
  }

  public static class OperationsBean implements Operations, StringReader {
    @Override
    public int getValue() {
      return 1;
    }

    @Override
    public int calculate(int operand) {
      return operand;
    }

    public int calculate(String operand) {
      return Integer.parseInt(operand);
    }

    public int calculate(int left, int right) {
      return left + right;
    }

    @Override
    public String read() {
      return "value";
    }
  }

  @ManagedResource
  public static class Sink {
    @ManagedAttribute(description = "write only", currencyTimeLimit = 0, persistPolicy = "OnUpdate")
    public void setValue(int value) { }
  }
}

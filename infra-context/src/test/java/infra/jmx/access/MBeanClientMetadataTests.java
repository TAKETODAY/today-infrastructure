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

package infra.jmx.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import infra.aop.framework.ProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class MBeanClientMetadataTests {

  private final MBeanServerConnection server = mock(MBeanServerConnection.class);

  private final ObjectName name = ObjectName.getInstance("test:type=ManagedResource");

  private final MBeanClientInterceptor interceptor = new MBeanClientInterceptor();

  MBeanClientMetadataTests() throws Exception {
  }

  @BeforeEach
  void configureInterceptor() throws Exception {
    interceptor.setServer(server);
    interceptor.setObjectName(name);
    interceptor.setUseStrictCasing(false);
    interceptor.setBeanClassLoader(getClass().getClassLoader());
  }

  @Test
  void loadsMetadataLazilyAndRoutesAttributesAndOverloads() throws Exception {
    given(server.getMBeanInfo(name)).willReturn(metadata(
            new MBeanAttributeInfo[] { attribute("label", true, true) },
            operation("describe", String.class.getName()), operation("describe", "int")));
    given(server.getAttribute(name, "label")).willReturn("initial");
    given(server.invoke(name, "describe", new Object[] { "abc" }, new String[] { String.class.getName() }))
            .willReturn("string abc");
    given(server.invoke(name, "describe", new Object[] { 3 }, new String[] { "int" })).willReturn("int 3");
    interceptor.setConnectOnStartup(false);
    interceptor.afterPropertiesSet();
    var proxy = ProxyFactory.getProxy(ManagedResource.class, interceptor);

    then(server).shouldHaveNoInteractions();
    assertThat(proxy.getLabel()).isEqualTo("initial");
    proxy.setLabel("updated");
    assertThat(proxy.describe("abc")).isEqualTo("string abc");
    assertThat(proxy.describe(3)).isEqualTo("int 3");
    assertThat(proxy.describe(3)).isEqualTo("int 3");
    then(server).should().setAttribute(name, new Attribute("label", "updated"));
    then(server).should().getMBeanInfo(name);
    then(server).should(times(2)).invoke(name, "describe", new Object[] { 3 }, new String[] { "int" });
  }

  @Test
  void rejectsOperationsAndAttributesNotAllowedByMetadata() throws Exception {
    given(server.getMBeanInfo(name)).willReturn(metadata(new MBeanAttributeInfo[] {
        attribute("readOnly", true, false), attribute("writeOnly", false, true) }));
    var proxy = ProxyFactory.getProxy(ManagedResource.class, interceptor);

    assertThatThrownBy(proxy::getLabel).isInstanceOf(InvalidInvocationException.class).hasMessageContaining("not exposed");
    assertThatThrownBy(() -> proxy.setReadOnly("x")).isInstanceOf(InvalidInvocationException.class).hasMessageContaining("not writable");
    assertThatThrownBy(proxy::getWriteOnly).isInstanceOf(InvalidInvocationException.class).hasMessageContaining("not readable");
    assertThatThrownBy(() -> proxy.describe(3)).isInstanceOf(InvalidInvocationException.class).hasMessageContaining("not exposed");
    then(server).should().getMBeanInfo(name);
    then(server).shouldHaveNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("metadataFailures")
  void reportsMetadataFailuresWithTheOriginalCause(Exception failure) throws Exception {
    given(server.getMBeanInfo(name)).willThrow(failure);

    assertThatThrownBy(interceptor::afterPropertiesSet).isInstanceOf(MBeanInfoRetrievalException.class).hasCause(failure);
  }

  private static Stream<Exception> metadataFailures() {
    return Stream.of(new IntrospectionException(), new InstanceNotFoundException(),
            new ReflectionException(new IllegalStateException()), new IOException("offline"));
  }

  @Test
  void rejectsMetadataWithAnUnresolvableOperationParameter() throws Exception {
    given(server.getMBeanInfo(name)).willReturn(metadata(new MBeanAttributeInfo[0], operation("describe", "missing.Type")));

    assertThatThrownBy(interceptor::afterPropertiesSet).isInstanceOf(MBeanInfoRetrievalException.class)
            .hasCauseInstanceOf(ClassNotFoundException.class).hasMessageContaining("method signature");
  }

  @Test
  void unwrapsManagedResourceExceptions() throws Exception {
    given(server.getMBeanInfo(name)).willReturn(metadata(new MBeanAttributeInfo[] { attribute("label", true, true) }));
    var checked = new IOException("resource failure");
    var runtime = new IllegalStateException("resource failure");
    var error = new AssertionError("resource failure");
    given(server.getAttribute(name, "label")).willThrow(new MBeanException(checked),
            new RuntimeMBeanException(runtime), new RuntimeErrorException(error));
    var proxy = ProxyFactory.getProxy(ManagedResource.class, interceptor);

    assertThatThrownBy(proxy::getLabel).isSameAs(checked);
    assertThatThrownBy(proxy::getLabel).isSameAs(runtime);
    assertThatThrownBy(proxy::getLabel).isSameAs(error);
  }

  @Test
  void translatesUndeclaredServerFailuresButPreservesDeclaredIoFailure() throws Exception {
    given(server.getMBeanInfo(name)).willReturn(metadata(new MBeanAttributeInfo[] {
        attribute("label", true, true), attribute("readOnly", true, false) }));
    var ioFailure = new IOException("offline");
    given(server.getAttribute(name, "readOnly")).willThrow(new AttributeNotFoundException("removed"), ioFailure);
    given(server.getAttribute(name, "label")).willThrow(ioFailure);
    var proxy = ProxyFactory.getProxy(ManagedResource.class, interceptor);

    assertThatThrownBy(proxy::getReadOnly).isInstanceOf(InvalidInvocationException.class).hasMessageContaining("removed");
    assertThatThrownBy(proxy::getReadOnly).isInstanceOf(MBeanConnectFailureException.class).hasCause(ioFailure);
    assertThatThrownBy(proxy::getLabel).isSameAs(ioFailure);
  }

  @Test
  void cannotRefreshAnExplicitServerConnection() {
    interceptor.setRefreshOnConnectFailure(true);
    assertThatIllegalArgumentException().isThrownBy(interceptor::afterPropertiesSet).withMessageContaining("'server' reference");
  }

  @Test
  void convertsOpenDataToTheDeclaredTypeOrCollectionElementType() throws Exception {
    given(server.getMBeanInfo(name)).willReturn(metadata(new MBeanAttributeInfo[] {
        attribute("row", true, false), attribute("rows", true, false), attribute("rowList", true, false),
        attribute("table", true, false), attribute("tables", true, false), attribute("tableList", true, false) }));
    var row = mock(CompositeData.class);
    var table = mock(TabularData.class);
    given(server.getAttribute(name, "row")).willReturn(row);
    given(server.getAttribute(name, "rows")).willReturn(new CompositeData[] { row });
    given(server.getAttribute(name, "rowList")).willReturn(new CompositeData[] { row });
    given(server.getAttribute(name, "table")).willReturn(table);
    given(server.getAttribute(name, "tables")).willReturn(new TabularData[] { table });
    given(server.getAttribute(name, "tableList")).willReturn(new TabularData[] { table });
    var proxy = ProxyFactory.getProxy(ManagedResource.class, interceptor);

    assertThat(proxy.getRow()).isEqualTo(new Row(row));
    assertThat(proxy.getRows()).containsExactly(new Row(row));
    assertThat(proxy.getRowList()).containsExactly(new Row(row));
    assertThat(proxy.getTable()).isEqualTo(new Table(table));
    assertThat(proxy.getTables()).containsExactly(new Table(table));
    assertThat(proxy.getTableList()).containsExactly(new Table(table));
  }

  @Test
  void rejectsIncompatibleResultsAndMissingOpenDataFactories() throws Exception {
    given(server.getMBeanInfo(name)).willReturn(metadata(new MBeanAttributeInfo[] { attribute("label", true, true) }));
    given(server.getAttribute(name, "label")).willReturn(Map.of("label", "wrong type"), mock(CompositeData.class));
    var proxy = ProxyFactory.getProxy(ManagedResource.class, interceptor);

    assertThatThrownBy(proxy::getLabel).isInstanceOf(InvocationFailureException.class).hasMessageContaining("Incompatible result value");
    assertThatThrownBy(proxy::getLabel).isInstanceOf(InvocationFailureException.class).hasMessageContaining("Could not obtain 'from");
  }

  private static MBeanInfo metadata(MBeanAttributeInfo[] attributes, MBeanOperationInfo... operations) {
    return new MBeanInfo(ManagedResource.class.getName(), "Managed resource", attributes, null, operations, null);
  }

  private static MBeanAttributeInfo attribute(String name, boolean readable, boolean writable) {
    return new MBeanAttributeInfo(name, String.class.getName(), name, readable, writable, false);
  }

  private static MBeanOperationInfo operation(String name, String parameterType) {
    return new MBeanOperationInfo(name, name, new MBeanParameterInfo[] {
        new MBeanParameterInfo("argument", parameterType, "argument") }, String.class.getName(), MBeanOperationInfo.INFO);
  }

  interface ManagedResource {

    String getLabel() throws IOException;

    void setLabel(String label);

    String getReadOnly();

    void setReadOnly(String value);

    String getWriteOnly();

    String describe(int value);

    String describe(String value);

    Row getRow();

    Row[] getRows();

    List<Row> getRowList();

    Table getTable();

    Table[] getTables();

    List<Table> getTableList();
  }

  public record Row(CompositeData source) {

    public static Row from(CompositeData source) {
      return new Row(source);
    }
  }

  public record Table(TabularData source) {

    public static Table from(TabularData source) {
      return new Table(source);
    }
  }

}

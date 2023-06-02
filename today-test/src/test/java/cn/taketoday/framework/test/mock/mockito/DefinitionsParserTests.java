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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.framework.test.mock.mockito.example.ExampleExtraInterface;
import cn.taketoday.framework.test.mock.mockito.example.ExampleService;
import cn.taketoday.framework.test.mock.mockito.example.ExampleServiceCaller;
import cn.taketoday.framework.test.mock.mockito.example.RealExampleService;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefinitionsParser}.
 *
 * @author Phillip Webb
 */
class DefinitionsParserTests {

  private DefinitionsParser parser = new DefinitionsParser();

  @Test
  void parseSingleMockBean() {
    this.parser.parse(SingleMockBean.class);
    assertThat(getDefinitions()).hasSize(1);
    assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
  }

  @Test
  void parseRepeatMockBean() {
    this.parser.parse(RepeatMockBean.class);
    assertThat(getDefinitions()).hasSize(2);
    assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
    assertThat(getMockDefinition(1).getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
  }

  @Test
  void parseMockBeanAttributes() {
    this.parser.parse(MockBeanAttributes.class);
    assertThat(getDefinitions()).hasSize(1);
    MockDefinition definition = getMockDefinition(0);
    assertThat(definition.getName()).isEqualTo("Name");
    assertThat(definition.getTypeToMock().resolve()).isEqualTo(ExampleService.class);
    assertThat(definition.getExtraInterfaces()).containsExactly(ExampleExtraInterface.class);
    assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
    assertThat(definition.isSerializable()).isTrue();
    assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
    assertThat(definition.getQualifier()).isNull();
  }

  @Test
  void parseMockBeanOnClassAndField() {
    this.parser.parse(MockBeanOnClassAndField.class);
    assertThat(getDefinitions()).hasSize(2);
    MockDefinition classDefinition = getMockDefinition(0);
    assertThat(classDefinition.getTypeToMock().resolve()).isEqualTo(ExampleService.class);
    assertThat(classDefinition.getQualifier()).isNull();
    MockDefinition fieldDefinition = getMockDefinition(1);
    assertThat(fieldDefinition.getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
    QualifierDefinition qualifier = QualifierDefinition
            .forElement(ReflectionUtils.findField(MockBeanOnClassAndField.class, "caller"));
    assertThat(fieldDefinition.getQualifier()).isNotNull().isEqualTo(qualifier);
  }

  @Test
  void parseMockBeanInferClassToMock() {
    this.parser.parse(MockBeanInferClassToMock.class);
    assertThat(getDefinitions()).hasSize(1);
    assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
  }

  @Test
  void parseMockBeanMissingClassToMock() {
    assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(MockBeanMissingClassToMock.class))
            .withMessageContaining("Unable to deduce type to mock");
  }

  @Test
  void parseMockBeanMultipleClasses() {
    this.parser.parse(MockBeanMultipleClasses.class);
    assertThat(getDefinitions()).hasSize(2);
    assertThat(getMockDefinition(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
    assertThat(getMockDefinition(1).getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
  }

  @Test
  void parseMockBeanMultipleClassesWithName() {
    assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(MockBeanMultipleClassesWithName.class))
            .withMessageContaining("The name attribute can only be used when mocking a single class");
  }

  @Test
  void parseSingleSpyBean() {
    this.parser.parse(SingleSpyBean.class);
    assertThat(getDefinitions()).hasSize(1);
    assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
  }

  @Test
  void parseRepeatSpyBean() {
    this.parser.parse(RepeatSpyBean.class);
    assertThat(getDefinitions()).hasSize(2);
    assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
    assertThat(getSpyDefinition(1).getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
  }

  @Test
  void parseSpyBeanAttributes() {
    this.parser.parse(SpyBeanAttributes.class);
    assertThat(getDefinitions()).hasSize(1);
    SpyDefinition definition = getSpyDefinition(0);
    assertThat(definition.getName()).isEqualTo("Name");
    assertThat(definition.getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
    assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
    assertThat(definition.getQualifier()).isNull();
  }

  @Test
  void parseSpyBeanOnClassAndField() {
    this.parser.parse(SpyBeanOnClassAndField.class);
    assertThat(getDefinitions()).hasSize(2);
    SpyDefinition classDefinition = getSpyDefinition(0);
    assertThat(classDefinition.getQualifier()).isNull();
    assertThat(classDefinition.getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
    SpyDefinition fieldDefinition = getSpyDefinition(1);
    QualifierDefinition qualifier = QualifierDefinition
            .forElement(ReflectionUtils.findField(SpyBeanOnClassAndField.class, "caller"));
    assertThat(fieldDefinition.getQualifier()).isNotNull().isEqualTo(qualifier);
    assertThat(fieldDefinition.getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
  }

  @Test
  void parseSpyBeanInferClassToMock() {
    this.parser.parse(SpyBeanInferClassToMock.class);
    assertThat(getDefinitions()).hasSize(1);
    assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
  }

  @Test
  void parseSpyBeanMissingClassToMock() {
    assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(SpyBeanMissingClassToMock.class))
            .withMessageContaining("Unable to deduce type to spy");
  }

  @Test
  void parseSpyBeanMultipleClasses() {
    this.parser.parse(SpyBeanMultipleClasses.class);
    assertThat(getDefinitions()).hasSize(2);
    assertThat(getSpyDefinition(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
    assertThat(getSpyDefinition(1).getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
  }

  @Test
  void parseSpyBeanMultipleClassesWithName() {
    assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(SpyBeanMultipleClassesWithName.class))
            .withMessageContaining("The name attribute can only be used when spying a single class");
  }

  private MockDefinition getMockDefinition(int index) {
    return (MockDefinition) getDefinitions().get(index);
  }

  private SpyDefinition getSpyDefinition(int index) {
    return (SpyDefinition) getDefinitions().get(index);
  }

  private List<Definition> getDefinitions() {
    return new ArrayList<>(this.parser.getDefinitions());
  }

  @MockBean(ExampleService.class)
  static class SingleMockBean {

  }

  @MockBeans({ @MockBean(ExampleService.class), @MockBean(ExampleServiceCaller.class) })
  static class RepeatMockBean {

  }

  @MockBean(name = "Name", classes = ExampleService.class, extraInterfaces = ExampleExtraInterface.class,
            answer = Answers.RETURNS_SMART_NULLS, serializable = true, reset = MockReset.NONE)
  static class MockBeanAttributes {

  }

  @MockBean(ExampleService.class)
  static class MockBeanOnClassAndField {

    @MockBean(ExampleServiceCaller.class)
    @Qualifier("test")
    private Object caller;

  }

  @MockBean({ ExampleService.class, ExampleServiceCaller.class })
  static class MockBeanMultipleClasses {

  }

  @MockBean(name = "name", classes = { ExampleService.class, ExampleServiceCaller.class })
  static class MockBeanMultipleClassesWithName {

  }

  static class MockBeanInferClassToMock {

    @MockBean
    private ExampleService exampleService;

  }

  @MockBean
  static class MockBeanMissingClassToMock {

  }

  @SpyBean(RealExampleService.class)
  static class SingleSpyBean {

  }

  @SpyBeans({ @SpyBean(RealExampleService.class), @SpyBean(ExampleServiceCaller.class) })
  static class RepeatSpyBean {

  }

  @SpyBean(name = "Name", classes = RealExampleService.class, reset = MockReset.NONE)
  static class SpyBeanAttributes {

  }

  @SpyBean(RealExampleService.class)
  static class SpyBeanOnClassAndField {

    @SpyBean(ExampleServiceCaller.class)
    @Qualifier("test")
    private Object caller;

  }

  @SpyBean({ RealExampleService.class, ExampleServiceCaller.class })
  static class SpyBeanMultipleClasses {

  }

  @SpyBean(name = "name", classes = { RealExampleService.class, ExampleServiceCaller.class })
  static class SpyBeanMultipleClassesWithName {

  }

  static class SpyBeanInferClassToMock {

    @SpyBean
    private RealExampleService exampleService;

  }

  @SpyBean
  static class SpyBeanMissingClassToMock {

  }

}

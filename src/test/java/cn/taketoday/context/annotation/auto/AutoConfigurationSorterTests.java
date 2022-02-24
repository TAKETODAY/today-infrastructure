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

package cn.taketoday.context.annotation.auto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 12:16
 */
class AutoConfigurationSorterTests {

  private static final String DEFAULT = OrderUnspecified.class.getName();

  private static final String LOWEST = OrderLowest.class.getName();

  private static final String HIGHEST = OrderHighest.class.getName();

  private static final String A = AutoConfigureA.class.getName();

  private static final String B = AutoConfigureB.class.getName();

  private static final String C = AutoConfigureC.class.getName();

  private static final String D = AutoConfigureD.class.getName();

  private static final String E = AutoConfigureE.class.getName();

  private static final String W = AutoConfigureW.class.getName();

  private static final String X = AutoConfigureX.class.getName();

  private static final String Y = AutoConfigureY.class.getName();

  private static final String Z = AutoConfigureZ.class.getName();

  private static final String A2 = AutoConfigureA2.class.getName();

  private static final String W2 = AutoConfigureW2.class.getName();

  private AutoConfigurationSorter sorter;

  private AutoConfigurationMetadata autoConfigurationMetadata = mock(AutoConfigurationMetadata.class);

  @BeforeEach
  void setup() {
    this.sorter = new AutoConfigurationSorter(new SkipCycleMetadataReaderFactory(), this.autoConfigurationMetadata);
  }

  @Test
  void byOrderAnnotation() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(LOWEST, HIGHEST, DEFAULT));
    assertThat(actual).containsExactly(HIGHEST, DEFAULT, LOWEST);
  }

  @Test
  void byAutoConfigureAfter() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C));
    assertThat(actual).containsExactly(C, B, A);
  }

  @Test
  void byAutoConfigureBefore() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(X, Y, Z));
    assertThat(actual).containsExactly(Z, Y, X);
  }

  @Test
  void byAutoConfigureAfterDoubles() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, E));
    assertThat(actual).containsExactly(C, E, B, A);
  }

  @Test
  void byAutoConfigureMixedBeforeAndAfter() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, W, X));
    assertThat(actual).containsExactly(C, W, B, A, X);
  }

  @Test
  void byAutoConfigureMixedBeforeAndAfterWithClassNames() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A2, B, C, W2, X));
    assertThat(actual).containsExactly(C, W2, B, A2, X);
  }

  @Test
  void byAutoConfigureMixedBeforeAndAfterWithDifferentInputOrder() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(W, X, A, B, C));
    assertThat(actual).containsExactly(C, W, B, A, X);
  }

  @Test
  void byAutoConfigureAfterWithMissing() {
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, B));
    assertThat(actual).containsExactly(B, A);
  }

  @Test
  void byAutoConfigureAfterWithCycle() {
    this.sorter = new AutoConfigurationSorter(new CachingMetadataReaderFactory(), this.autoConfigurationMetadata);
    assertThatIllegalStateException().isThrownBy(() -> this.sorter.getInPriorityOrder(Arrays.asList(A, B, C, D)))
            .withMessageContaining("AutoConfigure cycle detected");
  }

  @Test
  void usesAnnotationPropertiesWhenPossible() throws Exception {
    MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
    this.autoConfigurationMetadata = getAutoConfigurationMetadata(A2, B, C, W2, X);
    this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A2, B, C, W2, X));
    assertThat(actual).containsExactly(C, W2, B, A2, X);
  }

  @Test
  void useAnnotationWithNoDirectLink() throws Exception {
    MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
    this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, E);
    this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
    List<String> actual = this.sorter.getInPriorityOrder(Arrays.asList(A, E));
    assertThat(actual).containsExactly(E, A);
  }

  @Test
  void useAnnotationWithNoDirectLinkAndCycle() throws Exception {
    MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
    this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, D);
    this.sorter = new AutoConfigurationSorter(readerFactory, this.autoConfigurationMetadata);
    assertThatIllegalStateException().isThrownBy(() -> this.sorter.getInPriorityOrder(Arrays.asList(D, B)))
            .withMessageContaining("AutoConfigure cycle detected");
  }

  private AutoConfigurationMetadata getAutoConfigurationMetadata(String... classNames) throws Exception {
    Properties properties = new Properties();
    for (String className : classNames) {
      Class<?> type = ClassUtils.forName(className, null);
      properties.put(type.getName(), "");
      AutoConfigureOrder order = type.getDeclaredAnnotation(AutoConfigureOrder.class);
      if (order != null) {
        properties.put(className + ".AutoConfigureOrder", String.valueOf(order.value()));
      }
      AutoConfigureBefore autoConfigureBefore = type.getDeclaredAnnotation(AutoConfigureBefore.class);
      if (autoConfigureBefore != null) {
        properties.put(className + ".AutoConfigureBefore",
                merge(autoConfigureBefore.value(), autoConfigureBefore.name()));
      }
      AutoConfigureAfter autoConfigureAfter = type.getDeclaredAnnotation(AutoConfigureAfter.class);
      if (autoConfigureAfter != null) {
        properties.put(className + ".AutoConfigureAfter",
                merge(autoConfigureAfter.value(), autoConfigureAfter.name()));
      }
    }
    return AutoConfigurationMetadata.valueOf(properties);
  }

  private String merge(Class<?>[] value, String[] name) {
    Set<String> items = new LinkedHashSet<>();
    for (Class<?> type : value) {
      items.add(type.getName());
    }
    Collections.addAll(items, name);
    return StringUtils.collectionToCommaDelimitedString(items);
  }

  @AutoConfigureOrder
  static class OrderUnspecified {

  }

  @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
  static class OrderLowest {

  }

  @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
  static class OrderHighest {

  }

  @AutoConfigureAfter(AutoConfigureB.class)
  static class AutoConfigureA {

  }

  @AutoConfigureAfter(name = "cn.taketoday.context.annotation.auto.AutoConfigurationSorterTests$AutoConfigureB")
  static class AutoConfigureA2 {

  }

  @AutoConfigureAfter({ AutoConfigureC.class, AutoConfigureD.class, AutoConfigureE.class })
  static class AutoConfigureB {

  }

  static class AutoConfigureC {

  }

  @AutoConfigureAfter(AutoConfigureA.class)
  static class AutoConfigureD {

  }

  static class AutoConfigureE {

  }

  @AutoConfigureBefore(AutoConfigureB.class)
  static class AutoConfigureW {

  }

  @AutoConfigureBefore(name = "cn.taketoday.context.annotation.auto.AutoConfigurationSorterTests$AutoConfigureB")
  static class AutoConfigureW2 {

  }

  static class AutoConfigureX {

  }

  @AutoConfigureBefore(AutoConfigureX.class)
  static class AutoConfigureY {

  }

  @AutoConfigureBefore(AutoConfigureY.class)
  static class AutoConfigureZ {

  }

  static class SkipCycleMetadataReaderFactory extends CachingMetadataReaderFactory {

    @Override
    public MetadataReader getMetadataReader(String className) throws IOException {
      if (className.equals(D)) {
        throw new IOException();
      }
      return super.getMetadataReader(className);
    }

  }

}

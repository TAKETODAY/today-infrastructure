/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ImportSelector} and {@link DeferredImportSelector}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@SuppressWarnings("resource")
public class ImportSelectorTests {

  static Map<Class<?>, String> importFrom = new HashMap<>();

  @BeforeEach
  public void cleanup() {
    ImportSelectorTests.importFrom.clear();
    SampleImportSelector.cleanup();
    TestImportGroup.cleanup();
  }

  @Test
  public void importSelectors() {
    StandardBeanFactory beanFactory = spy(new StandardBeanFactory());
    StandardApplicationContext context = new StandardApplicationContext(beanFactory);
    context.register(Config.class);
    context.refresh();
    context.getBean(Config.class);
    InOrder ordered = inOrder(beanFactory);
    ordered.verify(beanFactory).registerBeanDefinition(eq("a"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("b"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("d"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("c"), any());
  }

  @Test
  public void invokeAwareMethodsInImportSelector() {
    StandardApplicationContext context = new StandardApplicationContext(AwareConfig.class);
    assertThat(SampleImportSelector.beanFactory).isEqualTo(context.getBeanFactory());
    assertThat(SampleImportSelector.classLoader).isEqualTo(context.getBeanFactory().getBeanClassLoader());
    assertThat(SampleImportSelector.resourceLoader).isNotNull();
    assertThat(SampleImportSelector.environment).isEqualTo(context.getEnvironment());
  }

  @Test
  public void correctMetadataOnIndirectImports() {
    StandardApplicationContext context = new StandardApplicationContext(IndirectConfig.class);
    String indirectImport = IndirectImport.class.getName();
    assertThat(importFrom.get(ImportSelector1.class)).isEqualTo(indirectImport);
    assertThat(importFrom.get(ImportSelector2.class)).isEqualTo(indirectImport);
    assertThat(importFrom.get(DeferredImportSelector1.class)).isEqualTo(indirectImport);
    assertThat(importFrom.get(DeferredImportSelector2.class)).isEqualTo(indirectImport);
    assertThat(context.containsBean("a")).isFalse();  // since ImportedSelector1 got filtered
    assertThat(context.containsBean("b")).isTrue();
    assertThat(context.containsBean("c")).isTrue();
    assertThat(context.containsBean("d")).isTrue();
  }

  @Test
  public void importSelectorsWithGroup() {
    StandardBeanFactory beanFactory = spy(new StandardBeanFactory());
    StandardApplicationContext context = new StandardApplicationContext(beanFactory);
    context.register(GroupedConfig.class);
    context.refresh();
    InOrder ordered = inOrder(beanFactory);
    ordered.verify(beanFactory).registerBeanDefinition(eq("a"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("b"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("c"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("d"), any());
    assertThat(TestImportGroup.instancesCount.get()).isEqualTo(1);
    assertThat(TestImportGroup.imports.size()).isEqualTo(1);
    assertThat(TestImportGroup.imports.values().iterator().next().size()).isEqualTo(2);
  }

  @Test
  public void importSelectorsSeparateWithGroup() {
    StandardBeanFactory beanFactory = spy(new StandardBeanFactory());
    StandardApplicationContext context = new StandardApplicationContext(beanFactory);
    context.register(GroupedConfig1.class);
    context.register(GroupedConfig2.class);
    context.refresh();
    InOrder ordered = inOrder(beanFactory);
    ordered.verify(beanFactory).registerBeanDefinition(eq("c"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("d"), any());
    assertThat(TestImportGroup.instancesCount.get()).isEqualTo(1);
    assertThat(TestImportGroup.imports.size()).isEqualTo(2);
    Iterator<AnnotationMetadata> iterator = TestImportGroup.imports.keySet().iterator();
    assertThat(iterator.next().getClassName()).isEqualTo(GroupedConfig2.class.getName());
    assertThat(iterator.next().getClassName()).isEqualTo(GroupedConfig1.class.getName());
  }

  @Test
  public void importSelectorsWithNestedGroup() {
    StandardBeanFactory beanFactory = spy(new StandardBeanFactory());
    StandardApplicationContext context = new StandardApplicationContext(beanFactory);
    context.register(ParentConfiguration1.class);
    context.refresh();
    InOrder ordered = inOrder(beanFactory);
    ordered.verify(beanFactory).registerBeanDefinition(eq("a"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("e"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("c"), any());
    assertThat(TestImportGroup.instancesCount.get()).isEqualTo(2);
    assertThat(TestImportGroup.imports.size()).isEqualTo(2);
    assertThat(TestImportGroup.allImports())
            .containsOnlyKeys(ParentConfiguration1.class.getName(), ChildConfiguration1.class.getName());
    assertThat(TestImportGroup.allImports().get(ParentConfiguration1.class.getName()))
            .containsExactly(DeferredImportSelector1.class.getName(), ChildConfiguration1.class.getName());
    assertThat(TestImportGroup.allImports().get(ChildConfiguration1.class.getName()))
            .containsExactly(DeferredImportedSelector3.class.getName());
  }

  @Test
  public void importSelectorsWithNestedGroupSameDeferredImport() {
    StandardBeanFactory beanFactory = spy(new StandardBeanFactory());
    StandardApplicationContext context = new StandardApplicationContext(beanFactory);
    context.register(ParentConfiguration2.class);
    context.refresh();
    InOrder ordered = inOrder(beanFactory);
    ordered.verify(beanFactory).registerBeanDefinition(eq("b"), any());
    ordered.verify(beanFactory).registerBeanDefinition(eq("d"), any());
    assertThat(TestImportGroup.instancesCount.get()).isEqualTo(2);
    assertThat(TestImportGroup.allImports().size()).isEqualTo(2);
    assertThat(TestImportGroup.allImports())
            .containsOnlyKeys(ParentConfiguration2.class.getName(), ChildConfiguration2.class.getName());
    assertThat(TestImportGroup.allImports().get(ParentConfiguration2.class.getName()))
            .containsExactly(DeferredImportSelector2.class.getName(), ChildConfiguration2.class.getName());
    assertThat(TestImportGroup.allImports().get(ChildConfiguration2.class.getName()))
            .containsExactly(DeferredImportSelector2.class.getName());
  }

  @Test
  public void invokeAwareMethodsInImportGroup() {
    StandardApplicationContext context = new StandardApplicationContext(GroupedConfig1.class);
    assertThat(TestImportGroup.beanFactory).isEqualTo(context.getBeanFactory());
    assertThat(TestImportGroup.classLoader).isEqualTo(context.getBeanFactory().getBeanClassLoader());
    assertThat(TestImportGroup.resourceLoader).isNotNull();
    assertThat(TestImportGroup.environment).isEqualTo(context.getEnvironment());
  }

  @Configuration
  @Import(SampleImportSelector.class)
  static class AwareConfig {
  }

  private static class SampleImportSelector
          implements ImportSelector, BeanClassLoaderAware,
                     ResourceLoaderAware, BeanFactoryAware, EnvironmentAware {

    static ClassLoader classLoader;
    static ResourceLoader resourceLoader;
    static BeanFactory beanFactory;
    static Environment environment;

    static void cleanup() {
      SampleImportSelector.classLoader = null;
      SampleImportSelector.beanFactory = null;
      SampleImportSelector.resourceLoader = null;
      SampleImportSelector.environment = null;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
      SampleImportSelector.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      SampleImportSelector.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      SampleImportSelector.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
      SampleImportSelector.environment = environment;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      return new String[] {};
    }
  }

  @Sample
  @Configuration
  static class Config {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import({ DeferredImportSelector1.class, DeferredImportSelector2.class,
          ImportSelector1.class, ImportSelector2.class })
  public @interface Sample {
  }

  public static class ImportSelector1 implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { ImportedSelector1.class.getName() };
    }
  }

  public static class ImportSelector2 implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { ImportedSelector2.class.getName() };
    }

  }

  public static class DeferredImportSelector1 implements DeferredImportSelector, Ordered {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { DeferredImportedSelector1.class.getName() };
    }

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE;
    }

  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  public static class DeferredImportSelector2 implements DeferredImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { DeferredImportedSelector2.class.getName() };
    }

  }

  @Configuration
  public static class ImportedSelector1 {

    @Bean
    public String a() {
      return "a";
    }
  }

  @Configuration
  public static class ImportedSelector2 {

    @Bean
    public String b() {
      return "b";
    }
  }

  @Configuration
  public static class DeferredImportedSelector1 {

    @Bean
    public String c() {
      return "c";
    }
  }

  @Configuration
  public static class DeferredImportedSelector2 {

    @Bean
    public String d() {
      return "d";
    }
  }

  @Configuration
  public static class DeferredImportedSelector3 {

    @Bean
    public String e() {
      return "e";
    }
  }

  @Configuration
  @Import(IndirectImportSelector.class)
  public static class IndirectConfig { }

  public static class IndirectImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      return new String[] { IndirectImport.class.getName() };
    }

    @Override
    @Nullable
    public Predicate<String> getExclusionFilter() {
      return className -> className.endsWith("ImportedSelector1");
    }
  }

  @Sample
  public static class IndirectImport {
  }

  @GroupedSample
  @Configuration
  static class GroupedConfig {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import({ GroupedDeferredImportSelector1.class, GroupedDeferredImportSelector2.class, ImportSelector1.class, ImportSelector2.class })
  public @interface GroupedSample {
  }

  @Configuration
  @Import(GroupedDeferredImportSelector1.class)
  static class GroupedConfig1 {
  }

  @Configuration
  @Import(GroupedDeferredImportSelector2.class)
  static class GroupedConfig2 {
  }

  public static class GroupedDeferredImportSelector1 extends DeferredImportSelector1 {

    @Nullable
    @Override
    public Class<? extends Group> getImportGroup() {
      return TestImportGroup.class;
    }
  }

  public static class GroupedDeferredImportSelector2 extends DeferredImportSelector2 {

    @Nullable
    @Override
    public Class<? extends Group> getImportGroup() {
      return TestImportGroup.class;
    }
  }

  @Configuration
  @Import({ ImportSelector1.class, ParentDeferredImportSelector1.class })
  public static class ParentConfiguration1 {
  }

  public static class ParentDeferredImportSelector1 implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { DeferredImportSelector1.class.getName(), ChildConfiguration1.class.getName() };
    }

    @Nullable
    @Override
    public Class<? extends Group> getImportGroup() {
      return TestImportGroup.class;
    }

  }

  @Configuration
  @Import({ ImportSelector2.class, ParentDeferredImportSelector2.class })
  public static class ParentConfiguration2 {
  }

  public static class ParentDeferredImportSelector2 implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { DeferredImportSelector2.class.getName(), ChildConfiguration2.class.getName() };
    }

    @Nullable
    @Override
    public Class<? extends Group> getImportGroup() {
      return TestImportGroup.class;
    }

  }

  @Configuration
  @Import(ChildDeferredImportSelector1.class)
  public static class ChildConfiguration1 {

  }

  public static class ChildDeferredImportSelector1 implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { DeferredImportedSelector3.class.getName() };
    }

    @Nullable
    @Override
    public Class<? extends Group> getImportGroup() {
      return TestImportGroup.class;
    }

  }

  @Configuration
  @Import(ChildDeferredImportSelector2.class)
  public static class ChildConfiguration2 {

  }

  public static class ChildDeferredImportSelector2 implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      ImportSelectorTests.importFrom.put(getClass(), importingClassMetadata.getClassName());
      return new String[] { DeferredImportSelector2.class.getName() };
    }

    @Nullable
    @Override
    public Class<? extends Group> getImportGroup() {
      return TestImportGroup.class;
    }

  }

  public static class TestImportGroup
          implements DeferredImportSelector.Group,
                     BeanClassLoaderAware, ResourceLoaderAware, BeanFactoryAware, EnvironmentAware {

    static ClassLoader classLoader;
    static ResourceLoader resourceLoader;
    static BeanFactory beanFactory;
    static Environment environment;

    static AtomicInteger instancesCount = new AtomicInteger();
    static MultiValueMap<AnnotationMetadata, String> imports = MultiValueMap.fromLinkedHashMap();

    public TestImportGroup() {
      TestImportGroup.instancesCount.incrementAndGet();
    }

    static void cleanup() {
      TestImportGroup.classLoader = null;
      TestImportGroup.beanFactory = null;
      TestImportGroup.resourceLoader = null;
      TestImportGroup.environment = null;
      TestImportGroup.instancesCount = new AtomicInteger();
      TestImportGroup.imports.clear();
    }

    static Map<String, List<String>> allImports() {
      return TestImportGroup.imports.entrySet()
              .stream()
              .collect(Collectors.toMap(entry -> entry.getKey().getClassName(),
                      Map.Entry::getValue));
    }

    private final List<Entry> instanceImports = new ArrayList<>();

    @Override
    public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
      for (String importClassName : selector.selectImports(metadata)) {
        this.instanceImports.add(new Entry(metadata, importClassName));
      }
      TestImportGroup.imports.addAll(metadata,
              Arrays.asList(selector.selectImports(metadata)));
    }

    @Override
    public Iterable<Entry> selectImports() {
      ArrayList<Entry> content = new ArrayList<>(this.instanceImports);
      Collections.reverse(content);
      return content;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
      TestImportGroup.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      TestImportGroup.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      TestImportGroup.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
      TestImportGroup.environment = environment;
    }
  }

}

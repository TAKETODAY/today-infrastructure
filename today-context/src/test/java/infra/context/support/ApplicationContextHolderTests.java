/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ExpressionEvaluator;
import infra.beans.factory.support.DependencyInjector;
import infra.context.ApplicationContext;
import infra.context.ApplicationEvent;
import infra.context.MessageSourceResolvable;
import infra.context.NoSuchMessageException;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotation;
import infra.core.env.Environment;
import infra.core.io.Resource;
import infra.core.io.ResourceConsumer;
import infra.core.io.SmartResourceConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 16:55
 */
class ApplicationContextHolderTests {

  @BeforeEach
  void setUp() {
    ApplicationContextHolder.getAll().clear();
  }

  @Test
  void testRegisterAndGet() {
    ApplicationContext context = new MockApplicationContext("test");
    assertThat(ApplicationContextHolder.register(context)).isNull();
    assertThat(ApplicationContextHolder.get("test")).isSameAs(context);
  }

  @Test
  void testRegisterWithId() {
    ApplicationContext context = new MockApplicationContext("original-id");
    assertThat(ApplicationContextHolder.register("custom-id", context)).isNull();
    assertThat(ApplicationContextHolder.get("custom-id")).isSameAs(context);
  }

  @Test
  void testOptional() {
    ApplicationContext context = new MockApplicationContext("test");
    ApplicationContextHolder.register(context);

    assertThat(ApplicationContextHolder.optional("test")).hasValue(context);
    assertThat(ApplicationContextHolder.optional("non-existing")).isEmpty();
  }

  @Test
  void testObtain() {
    ApplicationContext context = new MockApplicationContext("test");
    ApplicationContextHolder.register(context);

    assertThat(ApplicationContextHolder.obtain("test")).isSameAs(context);
    assertThatThrownBy(() -> ApplicationContextHolder.obtain("non-existing"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No ApplicationContext: 'non-existing'");
  }

  @Test
  void testRemove() {
    ApplicationContext context = new MockApplicationContext("test");
    ApplicationContextHolder.register(context);

    assertThat(ApplicationContextHolder.remove("test")).isSameAs(context);
    assertThat(ApplicationContextHolder.get("test")).isNull();

    // 测试通过context对象移除
    ApplicationContextHolder.register(context);
    ApplicationContextHolder.remove(context);
    assertThat(ApplicationContextHolder.get("test")).isNull();
  }

  @Test
  void testGetLastStartupContext() {
    assertThat(ApplicationContextHolder.getLastStartupContext()).isNull();

    ApplicationContext context1 = new MockApplicationContext("test1");
    ApplicationContext context2 = new MockApplicationContext("test2");

    ApplicationContextHolder.register(context1);
    ApplicationContextHolder.register(context2);

    assertThat(ApplicationContextHolder.getLastStartupContext()).isSameAs(context1);
  }

  @Test
  void testGetAll() {
    ApplicationContext context1 = new MockApplicationContext("test1");
    ApplicationContext context2 = new MockApplicationContext("test2");

    ApplicationContextHolder.register(context1);
    ApplicationContextHolder.register(context2);

    assertThat(ApplicationContextHolder.getAll())
            .hasSize(2)
            .containsValues(context1, context2)
            .containsKeys("test1", "test2");
  }

  static class MockApplicationContext implements ApplicationContext {
    private final String id;

    MockApplicationContext(String id) {
      this.id = id;
    }

    @Override
    public Environment getEnvironment() {
      return null;
    }

    @Override
    public BeanFactory getBeanFactory() {
      return null;
    }

    @Override
    public <T> T unwrapFactory(Class<T> requiredType) {
      return null;
    }

    @Override
    public void close() {

    }

    @Override
    public Instant getStartupDate() {
      return null;
    }

    @Override
    public State getState() {
      return null;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getApplicationName() {
      return "";
    }

    @Override
    public String getDisplayName() {
      return "";
    }

    @Nullable
    @Override
    public ApplicationContext getParent() {
      return null;
    }

    @Override
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
      return null;
    }

    @Override
    public ExpressionEvaluator getExpressionEvaluator() {
      return null;
    }

    @Nullable
    @Override
    public BeanFactory getParentBeanFactory() {
      return null;
    }

    @Override
    public boolean containsLocalBean(String name) {
      return false;
    }

    @Nullable
    @Override
    public Object getBean(String name) throws BeansException {
      return null;
    }

    @Nullable
    @Override
    public Object getBean(String name, Object... args) throws BeansException {
      return null;
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
      return null;
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
      return false;
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
      return false;
    }

    @Nullable
    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
      return null;
    }

    @Nullable
    @Override
    public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
      return null;
    }

    @Nullable
    @Override
    public <A extends Annotation> A findSynthesizedAnnotation(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
      return null;
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
      return null;
    }

    @Override
    public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
      return null;
    }

    @Override
    public <A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
      return Set.of();
    }

    @Override
    public boolean containsBean(String name) {
      return false;
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
      return false;
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
      return false;
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
      return false;
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
      return null;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
      return null;
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
      return null;
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons) throws BeansException {
      return Map.of();
    }

    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
      return Map.of();
    }

    @Override
    public <T> Map<String, T> getBeansOfType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
      return Map.of();
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type) {
      return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
      return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
      return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
      return new String[0];
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
      return new String[0];
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
      return null;
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
      return null;
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
      return null;
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
      return null;
    }

    @Override
    public int getBeanDefinitionCount() {
      return 0;
    }

    @Override
    public String[] getBeanDefinitionNames() {
      return new String[0];
    }

    @Override
    public String[] getAliases(String name) {
      return new String[0];
    }

    @Override
    public DependencyInjector getInjector() {
      return null;
    }

    @Override
    public void publishEvent(ApplicationEvent event) {

    }

    @Override
    public void publishEvent(Object event) {

    }

    @Nullable
    @Override
    public String getMessage(String code, Object @Nullable [] args, @Nullable String defaultMessage, Locale locale) {
      return "";
    }

    @Override
    public String getMessage(String code, Object @Nullable [] args, Locale locale) throws NoSuchMessageException {
      return "";
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
      return "";
    }

    @Override
    public Set<Resource> getResources(String locationPattern) throws IOException {
      return Set.of();
    }

    @Override
    public void scan(String locationPattern, ResourceConsumer consumer) throws IOException {

    }

    @Override
    public void scan(String locationPattern, SmartResourceConsumer consumer) throws IOException {

    }

    @Override
    public Resource getResource(String location) {
      return null;
    }

    @Nullable
    @Override
    public ClassLoader getClassLoader() {
      return null;
    }
  }

}

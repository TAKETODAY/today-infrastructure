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

package cn.taketoday.beans.factory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * Use BeanReference to resolve value
 *
 * @author TODAY 2021/3/6 15:18
 * @since 3.0
 */
public class BeanReferencePropertySetter extends AbstractPropertySetter {
  private static final Logger log = LoggerFactory.getLogger(BeanReferencePropertySetter.class);

  /** reference name */
  private final String referenceName;
  /** property is required? **/
  private final boolean required;
  /** record reference type @since v2.1.2 */
  private final Class<?> referenceClass;
  /** record if property is prototype @since v2.1.6 */
  private boolean prototype = false;
  /** @since 3.0.2 */
  private BeanDefinition reference;

  /** @since 3.0.2 */
  public BeanReferencePropertySetter(String referenceName, boolean required, BeanProperty property) {
    super(property);
    Assert.notNull(referenceName, "Bean name can't be null");
    this.required = required;
    this.referenceName = referenceName;
    this.referenceClass = property.getType();
  }

  @Override
  protected Object resolveValue(AbstractBeanFactory beanFactory) {
    // fix: same name of bean
    Object value = resolveBeanReference(beanFactory);
    if (value == null) {
      if (required) {
        throw new NoSuchBeanDefinitionException(reference.getName(), referenceClass);
      }
      return DO_NOT_SET; // if reference bean is null, and it is not required ,do nothing,default value
    }
    return value;
  }

  /**
   * Resolve reference {@link PropertySetter}
   *
   * @return A {@link PropertySetter} bean or a proxy
   * @see ConfigurableBeanFactory#isFullLifecycle()
   * @see ConfigurableBeanFactory#isFullPrototype()
   */
  protected Object resolveBeanReference(AbstractBeanFactory beanFactory) {
    String name = referenceName;
    Class<?> type = getReferenceClass();

    if (beanFactory.isFullPrototype() && prototype && beanFactory.containsBeanDefinition(name)) {
      return Prototypes.newProxyInstance(type, beanFactory.getBeanDefinition(name), beanFactory);
    }
    BeanDefinition reference = getReference();
    if (reference == null) {
      handleDependency(beanFactory);
    }
    if (reference != null) {
      return beanFactory.getBean(reference);
    }
    Object bean = beanFactory.getBean(name, type);
    return bean != null ? bean : beanFactory.doGetBeanForType(type);
  }

  /**
   * Handle abstract dependencies
   */
  public void handleDependency(AbstractBeanFactory beanFactory) {
    String beanName = getReferenceName();
    // fix: #2 when handle dependency some bean definition has already exist
    if (beanFactory.containsBeanDefinition(beanName)) {
      setReference(beanFactory.getBeanDefinition(beanName), beanFactory);
      return;
    }
    // handle dependency which is special bean like List<?> or Set<?>...
    // ----------------------------------------------------------------
    BeanDefinition handleDef = resolveDependency(beanFactory);
    if (handleDef != null) {
      beanFactory.registerBeanDefinition(beanName, handleDef);
      setReference(handleDef, beanFactory);
    }
    else {
      // handle dependency which is interface and parent object
      // --------------------------------------------------------
      Class<?> propertyType = getReferenceClass();
      // find child beans
      List<BeanDefinition> childDefs = doGetChildDefinition(beanFactory, beanName, propertyType);
      if (CollectionUtils.isNotEmpty(childDefs)) {
        BeanDefinition childDef = BeanFactoryUtils.getPrimaryBeanDefinition(childDefs);
        if (log.isDebugEnabled()) {
          log.debug("Found The Implementation Of [{}] Bean: [{}].", beanName, childDef.getName());
        }
        DefaultBeanDefinition def = new DefaultBeanDefinition(beanName, childDef);
        beanFactory.registerBeanDefinition(beanName, def);
        setReference(def, beanFactory);
      }
      if (isRequired()) {
        throw new ConfigurationException("Context does not exist for this reference:[" + reference + "] of bean");
      }
    }
  }

  /**
   * Get child {@link BeanDefinition}s
   *
   * @param beanFactory Bean Factory
   * @param beanName Bean name
   * @param beanClass Bean class
   * @return A list of {@link BeanDefinition}s, Never be null
   */
  protected List<BeanDefinition> doGetChildDefinition(BeanFactory beanFactory, String beanName, Class<?> beanClass) {
    LinkedHashSet<BeanDefinition> ret = new LinkedHashSet<>();
    for (String name : beanFactory.getBeanDefinitionNames()) {
      if (Objects.equals(beanName, name)) {
        continue;
      }
      BeanDefinition childDef = beanFactory.getBeanDefinition(name);
      if (beanFactory.isTypeMatch(name, beanClass)) {
        ret.add(childDef); // is beanClass's Child Bean
      }
    }

    return ret.isEmpty() ? null : new ArrayList<>(ret);
  }

  /**
   * Handle dependency {@link BeanDefinition}
   *
   * @param factory BeanReference
   * @return Dependency {@link BeanDefinition}
   */
  protected BeanDefinition resolveDependency(AbstractBeanFactory factory) {
    // from objectFactories
    Map<Class<?>, Object> objectFactories = factory.getObjectFactories();
    if (CollectionUtils.isNotEmpty(objectFactories)) {
      Object objectFactory = objectFactories.get(getReferenceClass());
      if (objectFactory != null) {
        DefaultBeanDefinition def = new DefaultBeanDefinition(getName(), getReferenceClass());
        def.setSupplier(new Supplier<Object>() {
          @Override
          public Object get() {
            return createDependencyInstance(def.getBeanClass(), objectFactory);
          }
        });
        return def;
      }
    }

    return null;
  }

  /**
   * Create dependency object
   *
   * @param type dependency type
   * @param objectFactory Object factory
   * @return Dependency object
   */
  protected Object createDependencyInstance(Class<?> type, Object objectFactory) {
    if (type.isInstance(objectFactory)) {
      return objectFactory;
    }
    if (objectFactory instanceof Supplier) {
      return createObjectFactoryDependencyProxy(type, (Supplier<?>) objectFactory);
    }
    return null;
  }

  protected Object createObjectFactoryDependencyProxy(
          Class<?> type, Supplier<?> objectFactory) {
    // fixed @since 3.0.1
    ProxyFactory proxyFactory = createProxyFactory();
    proxyFactory.setTargetSource(new ObjectFactoryTargetSource(objectFactory, type));
    proxyFactory.setOpaque(true);
    return proxyFactory.getProxy(type.getClassLoader());
  }

  protected ProxyFactory createProxyFactory() {
    return new ProxyFactory();
  }

  static final class ObjectFactoryTargetSource implements TargetSource {
    private final Class<?> targetType;
    private final Supplier<?> objectFactory;

    ObjectFactoryTargetSource(Supplier<?> objectFactory, Class<?> targetType) {
      this.targetType = targetType;
      this.objectFactory = objectFactory;
    }

    @Override
    public Class<?> getTargetClass() {
      return targetType;
    }

    @Override
    public boolean isStatic() {
      return false;
    }

    @Override
    public Object getTarget() throws Exception {
      return objectFactory.get();
    }
  }

  //---------------------------------------------------------------------
  // Getter Setter
  //---------------------------------------------------------------------

  /** @since 3.0.2 */
  public boolean isRequired() {
    return required;
  }

  /** @since 3.0.2 */
  public Class<?> getReferenceClass() {
    return referenceClass;
  }

  public String getReferenceName() {
    return referenceName;
  }

  /** @since 3.0.2 */
  public boolean isPrototype() {
    return prototype;
  }

  /** @since 3.0.2 */
  public void setPrototype(boolean prototype) {
    this.prototype = prototype;
  }

  /** @since 3.0.2 */
  public BeanDefinition getReference() {
    return reference;
  }

  /** @since 3.0.2 */
  public void setReference(BeanDefinition reference, AbstractBeanFactory beanFactory) {
    this.reference = reference;
    if (beanFactory.isFullPrototype()) {
      setPrototype(reference.isPrototype());
    }
  }

  //

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanReferencePropertySetter))
      return false;
    if (!super.equals(o))
      return false;
    final BeanReferencePropertySetter that = (BeanReferencePropertySetter) o;
    return required == that.required
            && Objects.equals(referenceName, that.referenceName)
            && Objects.equals(referenceClass, that.referenceClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), referenceName, required, referenceClass);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n\t\"referenceName\":\"");
    builder.append(referenceName);
    builder.append("\",\n\t\"required\":\"");
    builder.append(required);
    builder.append("\",\n\t\"referenceClass\":\"");
    builder.append(referenceClass);
    builder.append("\",\n\t\"field\":\"");
    builder.append(getField());
    builder.append("\",\n\t\"prototype\":\"");
    builder.append(isPrototype());
    builder.append("\"\n}");
    return builder.toString();
  }

}

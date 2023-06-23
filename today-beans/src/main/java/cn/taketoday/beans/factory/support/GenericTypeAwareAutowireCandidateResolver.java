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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Basic {@link AutowireCandidateResolver} that performs a full generic type
 * match with the candidate's type if the dependency is declared as a generic type
 * (e.g. Repository&lt;Customer&gt;).
 *
 * <p>This is the base class for
 * {@link QualifierAnnotationAutowireCandidateResolver},
 * providing an implementation all non-annotation-based resolution steps at this level.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/22 22:12
 */
public class GenericTypeAwareAutowireCandidateResolver
        extends SimpleAutowireCandidateResolver implements BeanFactoryAware, Cloneable {

  @Nullable
  private BeanFactory beanFactory;

  @Override
  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Nullable
  protected final BeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  @Override
  public boolean isAutowireCandidate(BeanDefinitionHolder definition, DependencyDescriptor descriptor) {
    if (!super.isAutowireCandidate(definition, descriptor)) {
      // If explicitly false, do not proceed with any other checks...
      return false;
    }
    return checkGenericTypeMatch(definition, descriptor);
  }

  /**
   * Match the given dependency type with its generic type information against the given
   * candidate bean definition.
   */
  protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
    ResolvableType dependencyType = descriptor.getResolvableType();
    if (dependencyType.getType() instanceof Class) {
      // No generic type -> we know it's a Class type-match, so no need to check again.
      return true;
    }

    ResolvableType targetType = null;
    boolean cacheType = false;
    RootBeanDefinition rbd = null;
    if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition hrbd) {
      rbd = hrbd;
    }
    if (rbd != null) {
      targetType = rbd.targetType;
      if (targetType == null) {
        cacheType = true;
        // First, check factory method return type, if applicable
        targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
        if (targetType == null) {
          RootBeanDefinition dbd = getResolvedDecoratedDefinition(rbd);
          if (dbd != null) {
            targetType = dbd.targetType;
            if (targetType == null) {
              targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
            }
          }
        }
      }
      else {
        // Pre-existing target type: In case of a generic FactoryBean type,
        // unwrap nested generic type when matching a non-FactoryBean type.
        Class<?> resolvedClass = targetType.resolve();
        if (resolvedClass != null && FactoryBean.class.isAssignableFrom(resolvedClass)) {
          Class<?> typeToBeMatched = dependencyType.resolve();
          if (typeToBeMatched != null && !FactoryBean.class.isAssignableFrom(typeToBeMatched)) {
            targetType = targetType.getGeneric();
            if (descriptor.fallbackMatchAllowed()) {
              // Matching the Class-based type determination for FactoryBean
              // objects in the lazy-determination getType code path below.
              targetType = ResolvableType.forClass(targetType.resolve());
            }
          }
        }
      }
    }

    if (targetType == null) {
      // Regular case: straight bean instance, with BeanFactory available.
      if (beanFactory != null) {
        Class<?> beanType = beanFactory.getType(bdHolder.getBeanName());
        if (beanType != null) {
          targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
        }
      }
      // Fallback: no BeanFactory set, or no type resolvable through it
      // -> best-effort match against the target class if applicable.
      if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
        Class<?> beanClass = rbd.getBeanClass();
        if (!FactoryBean.class.isAssignableFrom(beanClass)) {
          targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
        }
      }
    }

    if (targetType == null) {
      return true;
    }
    if (cacheType) {
      rbd.targetType = targetType;
    }
    if (descriptor.fallbackMatchAllowed() &&
            (targetType.hasUnresolvableGenerics() || targetType.resolve() == Properties.class)) {
      // Fallback matches allow unresolvable generics, e.g. plain HashMap to Map<String,String>;
      // and pragmatically also java.util.Properties to any Map (since despite formally being a
      // Map<Object,Object>, java.util.Properties is usually perceived as a Map<String,String>).
      return true;
    }
    // Full check for complex generic type match...
    return dependencyType.isAssignableFrom(targetType);
  }

  @Nullable
  protected RootBeanDefinition getResolvedDecoratedDefinition(RootBeanDefinition rbd) {
    BeanDefinitionHolder decDef = rbd.getDecoratedDefinition();
    if (decDef != null && this.beanFactory instanceof ConfigurableBeanFactory clbf) {
      if (clbf.containsBeanDefinition(decDef.getBeanName())) {
        BeanDefinition dbd = clbf.getMergedBeanDefinition(decDef.getBeanName());
        if (dbd instanceof RootBeanDefinition) {
          return (RootBeanDefinition) dbd;
        }
      }
    }
    return null;
  }

  @Nullable
  protected ResolvableType getReturnTypeForFactoryMethod(RootBeanDefinition rbd, DependencyDescriptor descriptor) {
    // Should typically be set for any kind of factory method, since the BeanFactory
    // pre-resolves them before reaching out to the AutowireCandidateResolver...
    ResolvableType returnType = rbd.factoryMethodReturnType;
    if (returnType == null) {
      Method factoryMethod = rbd.getResolvedFactoryMethod();
      if (factoryMethod != null) {
        returnType = ResolvableType.forReturnType(factoryMethod);
      }
    }
    if (returnType != null) {
      Class<?> resolvedClass = returnType.resolve();
      if (resolvedClass != null && descriptor.getDependencyType().isAssignableFrom(resolvedClass)) {
        // Only use factory method metadata if the return type is actually expressive enough
        // for our dependency. Otherwise, the returned instance type may have matched instead
        // in case of a singleton instance having been registered with the container already.
        return returnType;
      }
    }
    return null;
  }

  /**
   * This implementation clones all instance fields through standard
   * {@link Cloneable} support, allowing for subsequent reconfiguration
   * of the cloned instance through a fresh {@link #setBeanFactory} call.
   *
   * @see #clone()
   */
  @Override
  public AutowireCandidateResolver cloneIfNecessary() {
    try {
      return (AutowireCandidateResolver) clone();
    }
    catch (CloneNotSupportedException ex) {
      throw new IllegalStateException(ex);
    }
  }

}

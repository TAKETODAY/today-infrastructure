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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.bytecode.core.ClassLoaderAwareGeneratorStrategy;
import cn.taketoday.bytecode.core.DefaultNamingPolicy;
import cn.taketoday.bytecode.proxy.Callback;
import cn.taketoday.bytecode.proxy.CallbackFilter;
import cn.taketoday.bytecode.proxy.Enhancer;
import cn.taketoday.bytecode.proxy.Factory;
import cn.taketoday.bytecode.proxy.MethodInterceptor;
import cn.taketoday.bytecode.proxy.MethodProxy;
import cn.taketoday.bytecode.proxy.NoOp;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Default object instantiation strategy for use in BeanFactories.
 *
 * <p>Uses CGLIB to generate subclasses dynamically if methods need to be
 * overridden by the container to implement <em>Method Injection</em>.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/8 12:21
 */
public class CglibSubclassingInstantiationStrategy extends InstantiationStrategy {

  /**
   * Index in the CGLIB callback array for passthrough behavior,
   * in which case the subclass won't override the original class.
   */
  private static final int PASSTHROUGH = 0;

  /**
   * Index in the CGLIB callback array for a method that should
   * be overridden to provide <em>method lookup</em>.
   */
  private static final int LOOKUP_OVERRIDE = 1;

  /**
   * Index in the CGLIB callback array for a method that should
   * be overridden using generic <em>method replacer</em> functionality.
   */
  private static final int METHOD_REPLACER = 2;

  @Override
  protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
    return instantiateWithMethodInjection(bd, beanName, owner, null);
  }

  @Override
  protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
          @Nullable Constructor<?> ctor, Object... args) {

    // Must generate CGLIB subclass...
    return new CglibSubclassCreator(bd, owner).instantiate(ctor, args);
  }

  /**
   * An inner class created for historical reasons to avoid external CGLIB dependency.
   */
  private record CglibSubclassCreator(RootBeanDefinition beanDefinition, BeanFactory owner) {

    private static final Class<?>[] CALLBACK_TYPES = new Class<?>[] {
            NoOp.class, LookupOverrideMethodInterceptor.class, ReplaceOverrideMethodInterceptor.class
    };

    /**
     * Create a new instance of a dynamically generated subclass implementing the
     * required lookups.
     *
     * @param ctor constructor to use. If this is {@code null}, use the
     * no-arg constructor (no parameterization, or Setter Injection)
     * @param args arguments to use for the constructor.
     * Ignored if the {@code ctor} parameter is {@code null}.
     * @return new instance of the dynamically generated subclass
     */
    public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
      Class<?> subclass = createEnhancedSubclass(this.beanDefinition);
      Object instance;
      if (ctor == null) {
        instance = BeanUtils.newInstance(subclass);
      }
      else {
        try {
          Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
          instance = enhancedSubclassConstructor.newInstance(args);
        }
        catch (Exception ex) {
          throw new BeanInstantiationException(this.beanDefinition.getBeanClass(),
                  "Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
        }
      }
      // SPR-10785: set callbacks directly on the instance instead of in the
      // enhanced class (via the Enhancer) in order to avoid memory leaks.
      Factory factory = (Factory) instance;
      factory.setCallbacks(new Callback[] { NoOp.INSTANCE,
              new LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
              new ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner) });
      return instance;
    }

    /**
     * Create an enhanced subclass of the bean class for the provided bean
     * definition, using CGLIB.
     */
    private Class<?> createEnhancedSubclass(RootBeanDefinition beanDefinition) {
      Enhancer enhancer = new Enhancer();
      enhancer.setSuperclass(beanDefinition.getBeanClass());
      enhancer.setNamingPolicy(DefaultNamingPolicy.INSTANCE);
      if (this.owner instanceof ConfigurableBeanFactory owner) {
        ClassLoader cl = owner.getBeanClassLoader();
        enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(cl));
      }
      enhancer.setCallbackFilter(new MethodOverrideCallbackFilter(beanDefinition));
      enhancer.setCallbackTypes(CALLBACK_TYPES);
      return enhancer.createClass();
    }
  }

  /**
   * Class providing hashCode and equals methods required by CGLIB to
   * ensure that CGLIB doesn't generate a distinct class per bean.
   * Identity is based on class and bean definition.
   */
  private static class CglibIdentitySupport {

    private final RootBeanDefinition beanDefinition;

    public CglibIdentitySupport(RootBeanDefinition beanDefinition) {
      this.beanDefinition = beanDefinition;
    }

    public RootBeanDefinition getBeanDefinition() {
      return this.beanDefinition;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return (other != null && getClass() == other.getClass() &&
              this.beanDefinition.equals(((CglibIdentitySupport) other).beanDefinition));
    }

    @Override
    public int hashCode() {
      return this.beanDefinition.hashCode();
    }
  }

  /**
   * CGLIB callback for filtering method interception behavior.
   */
  private static class MethodOverrideCallbackFilter extends CglibIdentitySupport implements CallbackFilter {

    private static final Logger logger = LoggerFactory.getLogger(MethodOverrideCallbackFilter.class);

    public MethodOverrideCallbackFilter(RootBeanDefinition beanDefinition) {
      super(beanDefinition);
    }

    @Override
    public int accept(Method method) {
      MethodOverride methodOverride = getBeanDefinition().getMethodOverrides().getOverride(method);
      if (logger.isTraceEnabled()) {
        logger.trace("MethodOverride for {}: {}", method, methodOverride);
      }
      if (methodOverride == null) {
        return PASSTHROUGH;
      }
      else if (methodOverride instanceof LookupOverride) {
        return LOOKUP_OVERRIDE;
      }
      else if (methodOverride instanceof ReplaceOverride) {
        return METHOD_REPLACER;
      }
      throw new UnsupportedOperationException("Unexpected MethodOverride subclass: " +
              methodOverride.getClass().getName());
    }
  }

  /**
   * CGLIB MethodInterceptor to override methods, replacing them with an
   * implementation that returns a bean looked up in the container.
   */
  private static class LookupOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

    private final BeanFactory owner;

    public LookupOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
      super(beanDefinition);
      this.owner = owner;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
      // Cast is safe, as CallbackFilter filters are used selectively.
      LookupOverride lo = (LookupOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
      Assert.state(lo != null, "LookupOverride not found");
      Object[] argsToUse = args.length > 0 ? args : null;  // if no-arg, don't insist on args at all
      if (StringUtils.hasText(lo.getBeanName())) {
        return argsToUse != null ?
               owner.getBean(lo.getBeanName(), argsToUse) :
               owner.getBean(lo.getBeanName());
      }
      else {
        // Find target bean matching the (potentially generic) method return type
        ResolvableType genericReturnType = ResolvableType.forReturnType(method);
        return argsToUse != null
               ? owner.getObjectSupplier(genericReturnType).get(argsToUse)
               : owner.getObjectSupplier(genericReturnType).get();
      }
    }
  }

  /**
   * CGLIB MethodInterceptor to override methods, replacing them with a call
   * to a generic MethodReplacer.
   */
  private static class ReplaceOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

    private final BeanFactory owner;

    public ReplaceOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
      super(beanDefinition);
      this.owner = owner;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
      ReplaceOverride ro = (ReplaceOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
      Assert.state(ro != null, "ReplaceOverride not found");
      // TODO could cache if a singleton for minor performance optimization
      MethodReplacer mr = this.owner.getBean(ro.getMethodReplacerBeanName(), MethodReplacer.class);
      return mr.reimplement(obj, method, args);
    }
  }

}

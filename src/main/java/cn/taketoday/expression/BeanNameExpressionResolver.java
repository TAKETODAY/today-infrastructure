/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package cn.taketoday.expression;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.lang.Nullable;

/**
 * <p>
 * An <code>ELResolver</code> for resolving user or container managed beans.
 * </p>
 * <p>
 * A {@link BeanNameResolver} is required for its proper operation. The
 * following example creates an <code>ELResolver</code> that resolves the name
 * "bean" to an instance of MyBean. <blockquote>
 *
 * <pre>
 * ELResovler elr = new BeanNameELResolver(new BeanNameResolver {
 *    public boolean isNameResolved(String beanName) {
 *       return "bean".equals(beanName);
 *    }
 *    public Object getBean(String beanName) {
 *       return "bean".equals(beanName)? new MyBean(): null;
 *    }
 * });
 * </pre>
 *
 * </blockquote>
 * </p>
 *
 * @since EL 3.0
 */
public class BeanNameExpressionResolver extends ExpressionResolver {

  private final BeanNameResolver beanNameResolver;

  /**
   * Constructor
   *
   * @param beanNameResolver The {@link BeanNameResolver} that resolves a bean name.
   */
  public BeanNameExpressionResolver(BeanNameResolver beanNameResolver) {
    this.beanNameResolver = beanNameResolver;
  }

  /**
   * Constructor
   *
   * @param beanFactory The {@link BeanFactory} that resolves a bean name.
   */
  public BeanNameExpressionResolver(BeanFactory beanFactory) {
    this.beanNameResolver = new BeanFactoryResolver(beanFactory);
  }

  /**
   * If the base object is <code>null</code> and the property is a name that is
   * resolvable by the BeanNameResolver, returns the value resolved by the
   * BeanNameResolver.
   *
   * <p>
   * If name is resolved by the BeanNameResolver, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by this resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller
   * should ignore the return value.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base <code>null</code>
   * @param property The name of the bean.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the value
   * of the bean with the given name. Otherwise, undefined.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public Object getValue(ExpressionContext context, Object base, Object property) {

    if (base == null && property instanceof String propertyName) {
      if (beanNameResolver.isNameResolved(propertyName)) {
        context.setPropertyResolved(base, property);
        return beanNameResolver.getBean(propertyName);
      }
    }
    return null;
  }

  /**
   * If the base is null and the property is a name that is resolvable by the
   * BeanNameResolver, the bean in the BeanNameResolver is set to the given value.
   *
   * <p>
   * If the name is resolvable by the BeanNameResolver, or if the BeanNameResolver
   * allows creating a new bean, the <code>propertyResolved</code> property of the
   * <code>ELContext</code> object must be set to <code>true</code> by the
   * resolver, before returning. If this property is not <code>true</code> after
   * this method is called, the caller can safely assume no value has been set.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base <code>null</code>
   * @param property The name of the bean
   * @param value The value to set the bean with the given name to.
   * @throws NullPointerException if context is <code>null</code>
   * @throws PropertyNotWritableException if the BeanNameResolver does not allow the bean to be modified.
   * @throws ExpressionException if an exception was thrown while attempting to set the bean with
   * the given name. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public void setValue(
          ExpressionContext context, @Nullable Object base, Object property, Object value) {
    if (base == null && property instanceof String beanName) {
      if (beanNameResolver.canCreateBean(beanName)
              || beanNameResolver.isNameResolved(beanName)) {
        beanNameResolver.setBeanValue(beanName, value);
        context.setPropertyResolved(base, property);
      }
    }
  }

  /**
   * If the base is null and the property is a name resolvable by the
   * BeanNameResolver, return the type of the bean.
   *
   * <p>
   * If the name is resolvable by the BeanNameResolver, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller can
   * safely assume no value has been set.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base <code>null</code>
   * @param property The name of the bean.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then the type of
   * the bean with the given name. Otherwise, undefined.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {

    if (base == null && property instanceof String propertyName
            && beanNameResolver.isNameResolved(propertyName)) {
      context.setPropertyResolved(true);
      Object bean = beanNameResolver.getBean(propertyName);
      if (bean != null) {
        return bean.getClass();
      }
    }
    return null;
  }

  /**
   * If the base is null and the property is a name resolvable by the
   * BeanNameResolver, attempts to determine if the bean is writable.
   *
   * <p>
   * If the name is resolvable by the BeanNameResolver, the
   * <code>propertyResolved</code> property of the <code>ELContext</code> object
   * must be set to <code>true</code> by the resolver, before returning. If this
   * property is not <code>true</code> after this method is called, the caller can
   * safely assume no value has been set.
   * </p>
   *
   * @param context The context of this evaluation.
   * @param base <code>null</code>
   * @param property The name of the bean.
   * @return If the <code>propertyResolved</code> property of
   * <code>ELContext</code> was set to <code>true</code>, then
   * <code>true</code> if the property is read-only or <code>false</code>
   * if not; otherwise undefined.
   * @throws NullPointerException if context is <code>null</code>.
   * @throws ExpressionException if an exception was thrown while performing the property or
   * variable resolution. The thrown exception must be included as the
   * cause property of this exception, if available.
   */
  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    if (base == null && property instanceof String propertyName
            && beanNameResolver.isNameResolved(propertyName)) {
      context.setPropertyResolved(true);
      return beanNameResolver.isReadOnly(propertyName);
    }
    return false;
  }

}

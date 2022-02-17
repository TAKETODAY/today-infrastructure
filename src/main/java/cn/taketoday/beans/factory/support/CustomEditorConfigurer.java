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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyEditor;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyEditorRegistrar;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link BeanFactoryPostProcessor} implementation that allows for convenient
 * registration of custom {@link PropertyEditor property editors}.
 *
 * <p>In case you want to register {@link PropertyEditor} instances,
 * the recommended usage as of Spring 2.0 is to use custom
 * {@link PropertyEditorRegistrar} implementations that in turn register any
 * desired editor instances on a given
 * {@link cn.taketoday.beans.PropertyEditorRegistry registry}. Each
 * PropertyEditorRegistrar can register any number of custom editors.
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="cn.taketoday.beans.factory.support.CustomEditorConfigurer"&gt;
 *   &lt;property name="propertyEditorRegistrars"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="mypackage.MyCustomDateEditorRegistrar"/&gt;
 *       &lt;bean class="mypackage.MyObjectEditorRegistrar"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>
 * It's perfectly fine to register {@link PropertyEditor} <em>classes</em> via
 * the {@code customEditors} property. Spring will create fresh instances of
 * them for each editing attempt then:
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="cn.taketoday.beans.factory.support.CustomEditorConfigurer"&gt;
 *   &lt;property name="customEditors"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="java.util.Date" value="mypackage.MyCustomDateEditor"/&gt;
 *       &lt;entry key="mypackage.MyObject" value="mypackage.MyObjectEditor"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>
 * Note, that you shouldn't register {@link PropertyEditor} bean instances via
 * the {@code customEditors} property as {@link PropertyEditor PropertyEditors} are stateful
 * and the instances will then have to be synchronized for every editing
 * attempt. In case you need control over the instantiation process of
 * {@link PropertyEditor PropertyEditors}, use a {@link PropertyEditorRegistrar} to register
 * them.
 *
 * <p>
 * Also supports "java.lang.String[]"-style array class names and primitive
 * class names (e.g. "boolean"). Delegates to {@link ClassUtils} for actual
 * class name resolution.
 *
 * <p><b>NOTE:</b> Custom property editors registered with this configurer do
 * <i>not</i> apply to data binding. Custom editors for data binding need to
 * be registered on the {@link cn.taketoday.validation.DataBinder}:
 * Use a common base class or delegate to common PropertyEditorRegistrar
 * implementations to reuse editor registration there.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.beans.PropertyEditor
 * @see cn.taketoday.beans.PropertyEditorRegistrar
 * @see ConfigurableBeanFactory#addPropertyEditorRegistrar
 * @see ConfigurableBeanFactory#registerCustomEditor
 * @see cn.taketoday.validation.DataBinder#registerCustomEditor
 * @since 4.0 2022/2/17 18:16
 */
public class CustomEditorConfigurer implements BeanFactoryPostProcessor, Ordered {

  protected final Log logger = LogFactory.getLog(getClass());

  private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

  @Nullable
  private PropertyEditorRegistrar[] propertyEditorRegistrars;

  @Nullable
  private Map<Class<?>, Class<? extends PropertyEditor>> customEditors;

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * Specify the {@link PropertyEditorRegistrar PropertyEditorRegistrars}
   * to apply to beans defined within the current application context.
   * <p>This allows for sharing {@code PropertyEditorRegistrars} with
   * {@link cn.taketoday.validation.DataBinder DataBinders}, etc.
   * Furthermore, it avoids the need for synchronization on custom editors:
   * A {@code PropertyEditorRegistrar} will always create fresh editor
   * instances for each bean creation attempt.
   *
   * @see ConfigurableBeanFactory#addPropertyEditorRegistrar
   */
  public void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
    this.propertyEditorRegistrars = propertyEditorRegistrars;
  }

  /**
   * Specify the custom editors to register via a {@link Map}, using the
   * class name of the required type as the key and the class name of the
   * associated {@link PropertyEditor} as value.
   *
   * @see ConfigurableBeanFactory#registerCustomEditor
   */
  public void setCustomEditors(Map<Class<?>, Class<? extends PropertyEditor>> customEditors) {
    this.customEditors = customEditors;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    if (this.propertyEditorRegistrars != null) {
      for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
        beanFactory.addPropertyEditorRegistrar(propertyEditorRegistrar);
      }
    }
    if (this.customEditors != null) {
      this.customEditors.forEach(beanFactory::registerCustomEditor);
    }
  }

}

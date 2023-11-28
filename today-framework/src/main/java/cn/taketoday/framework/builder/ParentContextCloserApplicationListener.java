/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.builder;

import java.lang.ref.WeakReference;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.framework.builder.ParentContextApplicationContextInitializer.ParentContextAvailableEvent;
import cn.taketoday.util.ObjectUtils;

/**
 * Listener that closes the application context if its parent is closed. It listens for
 * refresh events and grabs the current context from there, and then listens for closed
 * events and propagates it down the hierarchy.
 *
 * @author Dave Syer
 * @author Eric Bottard
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ParentContextCloserApplicationListener extends OrderedSupport
        implements ApplicationListener<ParentContextAvailableEvent>, ApplicationContextAware, Ordered {

  private ApplicationContext context;

  public ParentContextCloserApplicationListener() {
    super(Ordered.LOWEST_PRECEDENCE - 10);
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }

  @Override
  public void onApplicationEvent(ParentContextAvailableEvent event) {
    maybeInstallListenerInParent(event.getApplicationContext());
  }

  private void maybeInstallListenerInParent(ConfigurableApplicationContext child) {
    if (child == this.context && child.getParent() instanceof ConfigurableApplicationContext parent) {
      parent.addApplicationListener(createContextCloserListener(child));
    }
  }

  /**
   * Subclasses may override to create their own subclass of ContextCloserListener. This
   * still enforces the use of a weak reference.
   *
   * @param child the child context
   * @return the {@link ContextCloserListener} to use
   */
  protected ContextCloserListener createContextCloserListener(ConfigurableApplicationContext child) {
    return new ContextCloserListener(child);
  }

  /**
   * {@link ApplicationListener} to close the context.
   */
  protected static class ContextCloserListener implements ApplicationListener<ContextClosedEvent> {

    private final WeakReference<ConfigurableApplicationContext> childContext;

    public ContextCloserListener(ConfigurableApplicationContext childContext) {
      this.childContext = new WeakReference<>(childContext);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
      ConfigurableApplicationContext context = this.childContext.get();
      if ((context != null) && (event.getApplicationContext() == context.getParent()) && context.isActive()) {
        context.close();
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (obj instanceof ContextCloserListener other) {
        return ObjectUtils.nullSafeEquals(this.childContext.get(), other.childContext.get());
      }
      return super.equals(obj);
    }

    @Override
    public int hashCode() {
      return ObjectUtils.nullSafeHashCode(this.childContext.get());
    }

  }

}

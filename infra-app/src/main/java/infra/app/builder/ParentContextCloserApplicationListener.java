/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.builder;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

import infra.app.builder.ParentContextApplicationContextInitializer.ParentContextAvailableEvent;
import infra.beans.BeansException;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.event.ContextClosedEvent;
import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.util.ObjectUtils;

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

  @Nullable
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

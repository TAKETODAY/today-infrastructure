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

package infra.context.event;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.core.ResolvableType;

/**
 * {@link infra.context.ApplicationListener} decorator that filters
 * events from a specified event source, invoking its delegate listener for
 * matching {@link infra.context.ApplicationEvent} objects only.
 *
 * <p>Can also be used as base class, overriding the {@link #onApplicationEventInternal}
 * method instead of specifying a delegate listener.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 15:47
 */
public class SourceFilteringListener implements GenericApplicationListener {

  private final Object source;

  @Nullable
  private GenericApplicationListener delegate;

  /**
   * Create a SourceFilteringListener for the given event source.
   *
   * @param source the event source that this listener filters for,
   * only processing events from this source
   * @param delegate the delegate listener to invoke with event
   * from the specified source
   */
  public SourceFilteringListener(Object source, ApplicationListener<?> delegate) {
    this.source = source;
    this.delegate = (delegate instanceof GenericApplicationListener ?
            (GenericApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
  }

  /**
   * Create a SourceFilteringListener for the given event source,
   * expecting subclasses to override the {@link #onApplicationEventInternal}
   * method (instead of specifying a delegate listener).
   *
   * @param source the event source that this listener filters for,
   * only processing events from this source
   */
  protected SourceFilteringListener(Object source) {
    this.source = source;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event.getSource() == this.source) {
      onApplicationEventInternal(event);
    }
  }

  @Override
  public boolean supportsEventType(ResolvableType eventType) {
    return (this.delegate == null || this.delegate.supportsEventType(eventType));
  }

  @Override
  public boolean supportsSourceType(@Nullable Class<?> sourceType) {
    return (sourceType != null && sourceType.isInstance(this.source));
  }

  @Override
  public int getOrder() {
    return (this.delegate != null ? this.delegate.getOrder() : LOWEST_PRECEDENCE);
  }

  @Override
  public String getListenerId() {
    return (this.delegate != null ? this.delegate.getListenerId() : "");
  }

  /**
   * Actually process the event, after having filtered according to the
   * desired event source already.
   * <p>The default implementation invokes the specified delegate, if any.
   *
   * @param event the event to process (matching the specified source)
   */
  protected void onApplicationEventInternal(ApplicationEvent event) {
    if (this.delegate == null) {
      throw new IllegalStateException(
              "Must specify a delegate object or override the onApplicationEventInternal method");
    }
    this.delegate.onApplicationEvent(event);
  }

}

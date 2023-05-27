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

package cn.taketoday.framework.web.embedded.undertow;

import java.io.File;
import java.util.Collection;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.server.reactive.UndertowHttpHandlerAdapter;
import io.undertow.Undertow;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link UndertowWebServer}s.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class UndertowReactiveWebServerFactory extends AbstractReactiveWebServerFactory
        implements ConfigurableUndertowWebServerFactory {

  private final UndertowWebServerFactoryDelegate delegate = new UndertowWebServerFactoryDelegate();

  /**
   * Create a new {@link UndertowReactiveWebServerFactory} instance.
   */
  public UndertowReactiveWebServerFactory() { }

  /**
   * Create a new {@link UndertowReactiveWebServerFactory} that listens for requests
   * using the specified port.
   *
   * @param port the port to listen on
   */
  public UndertowReactiveWebServerFactory(int port) {
    super(port);
  }

  @Override
  public void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
    this.delegate.setBuilderCustomizers(customizers);
  }

  @Override
  public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
    this.delegate.addBuilderCustomizers(customizers);
  }

  /**
   * Returns a mutable collection of the {@link UndertowBuilderCustomizer}s that will be
   * applied to the Undertow {@link Undertow.Builder Builder}.
   *
   * @return the customizers that will be applied
   */
  public Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
    return this.delegate.getBuilderCustomizers();
  }

  @Override
  public void setBufferSize(Integer bufferSize) {
    this.delegate.setBufferSize(bufferSize);
  }

  @Override
  public void setIoThreads(Integer ioThreads) {
    this.delegate.setIoThreads(ioThreads);
  }

  @Override
  public void setWorkerThreads(Integer workerThreads) {
    this.delegate.setWorkerThreads(workerThreads);
  }

  @Override
  public void setUseDirectBuffers(Boolean directBuffers) {
    this.delegate.setUseDirectBuffers(directBuffers);
  }

  @Override
  public void setUseForwardHeaders(boolean useForwardHeaders) {
    this.delegate.setUseForwardHeaders(useForwardHeaders);
  }

  protected final boolean isUseForwardHeaders() {
    return this.delegate.isUseForwardHeaders();
  }

  @Override
  public void setAccessLogDirectory(File accessLogDirectory) {
    this.delegate.setAccessLogDirectory(accessLogDirectory);
  }

  @Override
  public void setAccessLogPattern(String accessLogPattern) {
    this.delegate.setAccessLogPattern(accessLogPattern);
  }

  @Override
  public void setAccessLogPrefix(String accessLogPrefix) {
    this.delegate.setAccessLogPrefix(accessLogPrefix);
  }

  @Override
  public void setAccessLogSuffix(String accessLogSuffix) {
    this.delegate.setAccessLogSuffix(accessLogSuffix);
  }

  public boolean isAccessLogEnabled() {
    return this.delegate.isAccessLogEnabled();
  }

  @Override
  public void setAccessLogEnabled(boolean accessLogEnabled) {
    this.delegate.setAccessLogEnabled(accessLogEnabled);
  }

  @Override
  public void setAccessLogRotate(boolean accessLogRotate) {
    this.delegate.setAccessLogRotate(accessLogRotate);
  }

  @Override
  public WebServer getWebServer(cn.taketoday.http.server.reactive.HttpHandler httpHandler) {
    Undertow.Builder builder = delegate.createBuilder(this, this::getSslBundle);
    var httpHandlerFactories = delegate.createHttpHandlerFactories(
            this, next -> new UndertowHttpHandlerAdapter(httpHandler));
    return new UndertowWebServer(builder, httpHandlerFactories, getPort() >= 0);
  }

}

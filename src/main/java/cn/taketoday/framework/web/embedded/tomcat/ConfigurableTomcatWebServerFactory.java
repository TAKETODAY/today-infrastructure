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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;

import java.io.File;
import java.nio.charset.Charset;

import cn.taketoday.framework.web.server.ConfigurableWebServerFactory;

/**
 * {@link ConfigurableWebServerFactory} for Tomcat-specific features.
 *
 * @author Brian Clozel
 * @see TomcatServletWebServerFactory
 * @see TomcatReactiveWebServerFactory
 * @since 4.0
 */
public interface ConfigurableTomcatWebServerFactory extends ConfigurableWebServerFactory {

  /**
   * Set the Tomcat base directory. If not specified a temporary directory will be used.
   *
   * @param baseDirectory the tomcat base directory
   */
  void setBaseDirectory(File baseDirectory);

  /**
   * Sets the background processor delay in seconds.
   *
   * @param delay the delay in seconds
   */
  void setBackgroundProcessorDelay(int delay);

  /**
   * Add {@link Valve}s that should be applied to the Tomcat {@link Engine}.
   *
   * @param engineValves the valves to add
   */
  void addEngineValves(Valve... engineValves);

  /**
   * Add {@link TomcatConnectorCustomizer}s that should be added to the Tomcat
   * {@link Connector}.
   *
   * @param tomcatConnectorCustomizers the customizers to add
   */
  void addConnectorCustomizers(TomcatConnectorCustomizer... tomcatConnectorCustomizers);

  /**
   * Add {@link TomcatContextCustomizer}s that should be added to the Tomcat
   * {@link Context}.
   *
   * @param tomcatContextCustomizers the customizers to add
   */
  void addContextCustomizers(TomcatContextCustomizer... tomcatContextCustomizers);

  /**
   * Add {@link TomcatProtocolHandlerCustomizer}s that should be added to the Tomcat
   * {@link Connector}.
   *
   * @param tomcatProtocolHandlerCustomizers the customizers to add
   * @since 4.0
   */
  void addProtocolHandlerCustomizers(TomcatProtocolHandlerCustomizer<?>... tomcatProtocolHandlerCustomizers);

  /**
   * Set the character encoding to use for URL decoding. If not specified 'UTF-8' will
   * be used.
   *
   * @param uriEncoding the uri encoding to set
   */
  void setUriEncoding(Charset uriEncoding);

}

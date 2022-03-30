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

package cn.taketoday.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import cn.taketoday.test.web.servlet.htmlunit.MockMvcWebConnection;
import cn.taketoday.util.Assert;

/**
 * {@code WebConnectionHtmlUnitDriver} enables configuration of the
 * {@link WebConnection} for an {@link HtmlUnitDriver} instance.
 *
 * <p>This is useful because it allows a
 * {@link MockMvcWebConnection
 * MockMvcWebConnection} to be injected.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see MockMvcHtmlUnitDriverBuilder
 * @since 4.2
 */
public class WebConnectionHtmlUnitDriver extends HtmlUnitDriver {

  public WebConnectionHtmlUnitDriver() {
  }

  public WebConnectionHtmlUnitDriver(BrowserVersion browserVersion) {
    super(browserVersion);
  }

  public WebConnectionHtmlUnitDriver(boolean enableJavascript) {
    super(enableJavascript);
  }

  public WebConnectionHtmlUnitDriver(Capabilities capabilities) {
    super(capabilities);
  }

  /**
   * Modify the supplied {@link WebClient} and retain a reference to it so that its
   * {@link WebConnection} is {@linkplain #getWebConnection accessible} for later use.
   * <p>Delegates to {@link HtmlUnitDriver#modifyWebClient} for default behavior
   * and to {@link #modifyWebClientInternal} for further customization.
   *
   * @param webClient the client to modify
   * @return the modified client
   * @see HtmlUnitDriver#modifyWebClient(WebClient)
   * @see #modifyWebClientInternal(WebClient)
   */
  @Override
  protected final WebClient modifyWebClient(WebClient webClient) {
    return modifyWebClientInternal(super.modifyWebClient(webClient));
  }

  /**
   * Modify the supplied {@link WebClient}.
   * <p>The default implementation simply returns the supplied client unmodified.
   * <p>Subclasses can override this method to customize the {@code WebClient}
   * that the {@link HtmlUnitDriver} uses.
   *
   * @param webClient the client to modify
   * @return the modified client
   */
  protected WebClient modifyWebClientInternal(WebClient webClient) {
    return webClient;
  }

  /**
   * Return the current {@link WebClient} in a public fashion.
   *
   * @since 4.3
   */
  @Override
  public WebClient getWebClient() {
    return super.getWebClient();
  }

  /**
   * Set the {@link WebConnection} to be used with the {@link WebClient}.
   *
   * @param webConnection the {@code WebConnection} to use
   */
  public void setWebConnection(WebConnection webConnection) {
    Assert.notNull(webConnection, "WebConnection must not be null");
    getWebClient().setWebConnection(webConnection);
  }

  /**
   * Access the current {@link WebConnection} for the {@link WebClient}.
   *
   * @return the current {@code WebConnection}
   */
  public WebConnection getWebConnection() {
    return getWebClient().getWebConnection();
  }

}

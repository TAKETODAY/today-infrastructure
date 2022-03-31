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

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.htmlunit.MockMvcWebConnectionBuilderSupport;
import cn.taketoday.test.web.servlet.setup.MockMvcConfigurer;
import cn.taketoday.test.web.servlet.htmlunit.WebRequestMatcher;
import cn.taketoday.util.Assert;
import cn.taketoday.web.context.WebApplicationContext;

/**
 * {@code MockMvcHtmlUnitDriverBuilder} simplifies the building of an
 * {@link HtmlUnitDriver} that delegates to {@link MockMvc} and optionally
 * delegates to an actual connection for specific requests.
 *
 * <p>By default, the driver will delegate to {@code MockMvc} to handle
 * requests to {@code localhost} and to a {@link WebClient} to handle any
 * other URL (i.e. to perform an actual HTTP request).
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @see #mockMvcSetup(MockMvc)
 * @see #webAppContextSetup(WebApplicationContext)
 * @see #webAppContextSetup(WebApplicationContext, MockMvcConfigurer)
 * @see #javascriptEnabled(boolean)
 * @see #withDelegate(WebConnectionHtmlUnitDriver)
 * @see #build()
 * @since 4.0
 */
public class MockMvcHtmlUnitDriverBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcHtmlUnitDriverBuilder> {

  @Nullable
  private HtmlUnitDriver driver;

  private boolean javascriptEnabled = true;

  protected MockMvcHtmlUnitDriverBuilder(MockMvc mockMvc) {
    super(mockMvc);
  }

  protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context) {
    super(context);
  }

  protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
    super(context, configurer);
  }

  /**
   * Create a new {@code MockMvcHtmlUnitDriverBuilder} based on the supplied
   * {@link MockMvc} instance.
   *
   * @param mockMvc the {@code MockMvc} instance to use (never {@code null})
   * @return the MockMvcHtmlUnitDriverBuilder to customize
   */
  public static MockMvcHtmlUnitDriverBuilder mockMvcSetup(MockMvc mockMvc) {
    Assert.notNull(mockMvc, "MockMvc must not be null");
    return new MockMvcHtmlUnitDriverBuilder(mockMvc);
  }

  /**
   * Create a new {@code MockMvcHtmlUnitDriverBuilder} based on the supplied
   * {@link WebApplicationContext}.
   *
   * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
   * instance from (never {@code null})
   * @return the MockMvcHtmlUnitDriverBuilder to customize
   */
  public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context) {
    Assert.notNull(context, "WebApplicationContext must not be null");
    return new MockMvcHtmlUnitDriverBuilder(context);
  }

  /**
   * Create a new {@code MockMvcHtmlUnitDriverBuilder} based on the supplied
   * {@link WebApplicationContext} and {@link MockMvcConfigurer}.
   *
   * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
   * instance from (never {@code null})
   * @param configurer the {@code MockMvcConfigurer} to apply (never {@code null})
   * @return the MockMvcHtmlUnitDriverBuilder to customize
   */
  public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context,
          MockMvcConfigurer configurer) {

    Assert.notNull(context, "WebApplicationContext must not be null");
    Assert.notNull(configurer, "MockMvcConfigurer must not be null");
    return new MockMvcHtmlUnitDriverBuilder(context, configurer);
  }

  /**
   * Specify whether JavaScript should be enabled.
   * <p>Default is {@code true}.
   *
   * @param javascriptEnabled {@code true} if JavaScript should be enabled
   * @return this builder for further customizations
   * @see #build()
   */
  public MockMvcHtmlUnitDriverBuilder javascriptEnabled(boolean javascriptEnabled) {
    this.javascriptEnabled = javascriptEnabled;
    return this;
  }

  /**
   * Supply the {@code WebConnectionHtmlUnitDriver} that the driver
   * {@linkplain #build built} by this builder should delegate to when
   * processing non-{@linkplain WebRequestMatcher matching} requests.
   *
   * @param driver the {@code WebConnectionHtmlUnitDriver} to delegate to
   * for requests that do not match (never {@code null})
   * @return this builder for further customizations
   * @see #build()
   */
  public MockMvcHtmlUnitDriverBuilder withDelegate(WebConnectionHtmlUnitDriver driver) {
    Assert.notNull(driver, "HtmlUnitDriver must not be null");
    driver.setJavascriptEnabled(this.javascriptEnabled);
    driver.setWebConnection(createConnection(driver.getWebClient()));
    this.driver = driver;
    return this;
  }

  /**
   * Build the {@link HtmlUnitDriver} configured via this builder.
   * <p>The returned driver will use the configured {@link MockMvc} instance
   * for processing any {@linkplain WebRequestMatcher matching} requests
   * and a delegate {@code HtmlUnitDriver} for all other requests.
   * <p>If a {@linkplain #withDelegate delegate} has been explicitly configured,
   * it will be used; otherwise, a default {@code WebConnectionHtmlUnitDriver}
   * with the {@link BrowserVersion} set to {@link BrowserVersion#CHROME CHROME}
   * will be configured as the delegate.
   *
   * @return the {@code HtmlUnitDriver} to use
   * @see #withDelegate(WebConnectionHtmlUnitDriver)
   */
  public HtmlUnitDriver build() {
    return (this.driver != null ? this.driver :
            withDelegate(new WebConnectionHtmlUnitDriver(BrowserVersion.CHROME)).build());
  }

}

/**
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet.initializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-02-03 12:28
 */
public class WebServletInitializer<T extends Servlet>
        extends WebComponentInitializer<ServletRegistration.Dynamic> {
  private static final Logger log = LoggerFactory.getLogger(WebServletInitializer.class);

  private T servlet;
  private int loadOnStartup = -1;
  private MultipartConfigElement multipartConfig;
  private ServletSecurityElement servletSecurity;

  public WebServletInitializer() { }

  public WebServletInitializer(T servlet) {
    this.servlet = servlet;
  }

  @Override
  protected Dynamic addRegistration(ServletContext servletContext) {
    final T servlet = getServlet();
    Assert.state(servlet != null, "servlet can't be null");
    return servletContext.addServlet(getName(), servlet);
  }

  /**
   * Configure registration settings. Subclasses can override this method to
   * perform additional configuration if required.
   *
   * @param registration
   *         the registration
   */
  @Override
  protected void configureRegistration(Dynamic registration) {
    log.debug("Configure servlet registration: [{}]", this);
    registration.setLoadOnStartup(this.loadOnStartup);

    super.configureRegistration(registration);
    configureMultipart(registration);
    configureUrlMappings(registration);
    configureServletSecurity(registration);
  }

  protected void configureUrlMappings(Dynamic registration) {

    final String[] urlMappings = getUrlMappings().isEmpty()
                                 ? DEFAULT_MAPPINGS
                                 : StringUtils.toStringArray(getUrlMappings());

    registration.addMapping(urlMappings);
  }

  protected void configureServletSecurity(Dynamic registration) {
    if (this.servletSecurity != null) {
      registration.setServletSecurity(servletSecurity);
    }
  }

  protected void configureMultipart(Dynamic registration) {
    if (this.multipartConfig != null) {
      registration.setMultipartConfig(this.multipartConfig);
    }
  }

  public void setServlet(T servlet) {
    this.servlet = servlet;
  }

  public T getServlet() {
    return servlet;
  }

  @Override
  protected String getDefaultName() {

    final T servlet = getServlet();
    if (servlet != null) {
      return servlet.getClass().getName();
    }
    return null;
  }

  // ---------------

  public int getLoadOnStartup() {
    return loadOnStartup;
  }

  public void setLoadOnStartup(int loadOnStartup) {
    this.loadOnStartup = loadOnStartup;
  }

  /**
   * Returns the {@link MultipartConfigElement multi-part configuration} to be
   * applied or {@code null}.
   *
   * @return the multipart config
   */
  public MultipartConfigElement getMultipartConfig() {
    return this.multipartConfig;
  }

  public void setMultipartConfig(MultipartConfigElement multipartConfig) {
    this.multipartConfig = multipartConfig;
  }

  public ServletSecurityElement getServletSecurity() {
    return servletSecurity;
  }

  public void setServletSecurity(ServletSecurityElement servletSecurity) {
    this.servletSecurity = servletSecurity;
  }

  @Override
  public String toString() {
    return new StringBuilder()//
            .append("{\n\t\"servlet\":\"").append(servlet)//
            .append("\",\n\t\"name\":\"").append(getName())//
            .append("\",\n\t\"loadOnStartup\":\"").append(loadOnStartup)//
            .append("\",\n\t\"multipartConfig\":\"").append(multipartConfig)//
            .append("\",\n\t\"servletSecurity\":\"").append(servletSecurity)//
            .append("\",\n\t\"initParameters\":\"").append(getInitParameters())//
            .append("\",\n\t\"order\":\"").append(getOrder())//
            .append("\",\n\t\"urlMappings\":\"").append(getUrlMappings())//
            .append("\",\n\t\"asyncSupported\":\"").append(isAsyncSupported())//
            .append("\"\n}").toString();
  }

}

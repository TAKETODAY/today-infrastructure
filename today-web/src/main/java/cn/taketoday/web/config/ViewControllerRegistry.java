/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.EmbeddedValueResolver;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ApplicationContextSupport;
import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Version;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.InfraConfigurationException;
import cn.taketoday.web.handler.SimpleUrlHandlerMapping;

/**
 * Assists with the registration of simple automated controllers pre-configured
 * with status code and/or a view.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 22:20
 */
public class ViewControllerRegistry extends ApplicationContextSupport {
  public static final String webMvcConfigLocation = "classpath:web-mvc.xml";

  // the dtd
  public static final String DTD_NAME = "web-configuration";

  // config
  public static final String ATTR_RESOURCE = "resource";
  public static final String ATTR_NAME = "name";
  /** resource location @since 2.3.7 */
  public static final String ATTR_PREFIX = "prefix";
  public static final String ATTR_SUFFIX = "suffix";

  /**
   * The resoure's content type
   *
   * @since 2.3.3
   */
  public static final String ATTR_CONTENT_TYPE = "content-type";
  public static final String ATTR_TYPE = "type";
  /** The response status @since 2.3.7 */
  public static final String ATTR_STATUS = "status";

  public static final String ELEMENT_ACTION = "action";
  public static final String ELEMENT_CONTROLLER = "controller";
  public static final String ROOT_ELEMENT = "Web-Configuration";

  private final List<ViewControllerRegistration> registrations = new ArrayList<>(4);

  private final List<RedirectViewControllerRegistration> redirectRegistrations = new ArrayList<>(10);

  private int order = 1;

  protected EmbeddedValueResolver embeddedValueResolver;

  /**
   * Class constructor with {@link ApplicationContext}.
   */
  public ViewControllerRegistry(ApplicationContext applicationContext) {
    setApplicationContext(applicationContext);
    this.embeddedValueResolver = new EmbeddedValueResolver(
            applicationContext.unwrapFactory(ConfigurableBeanFactory.class));
  }

  /**
   * Map a URL path or pattern to a view controller to render a response with
   * the configured status code and view.
   * <p>Patterns such as {@code "/admin/**"} or {@code "/articles/{articlename:\\w+}"}
   * are supported.
   *
   * <p><strong>Note:</strong> If an {@code @RequestMapping} method is mapped
   * to a URL for any HTTP method then a view controller cannot handle the
   * same URL. For this reason it is recommended to avoid splitting URL
   * handling across an annotated controller and a view controller.
   */
  public ViewControllerRegistration addViewController(String pathPattern) {
    ViewControllerRegistration registration = new ViewControllerRegistration(pathPattern);
    registration.setApplicationContext(this.applicationContext);
    this.registrations.add(registration);
    return registration;
  }

  /**
   * Map a view controller to the given URL path (or pattern) in order to render a
   * response with a pre-configured status code and view.
   * <p>
   * Patterns like {@code "/articles/**"} or {@code "/articles/{id:\\w+}"} are
   * allowed. See {@link AntPathMatcher} for more details on the syntax.
   *
   * @param pathPattern Patterns like {@code "/articles/**"} or
   * {@code "/articles/{id:\\w+}"} are allowed. See
   * {@link AntPathMatcher} for more details on the syntax.
   * @param resource resource location,such as redirect url or view template resource
   * <b>Or other type of resource</b>
   */
  public ViewControllerRegistration addViewController(String pathPattern, Object resource) {
    return addViewController(pathPattern).setReturnValue(resource);
  }

  /**
   * Map a view controller to the given URL path or pattern in order to redirect
   * to another URL.
   *
   * <p>By default the redirect URL is expected to be relative to the current
   * ServletContext, i.e. as relative to the web application root.
   */
  public RedirectViewControllerRegistration addRedirectViewController(String pathPattern, String redirectUrl) {
    RedirectViewControllerRegistration registration = new RedirectViewControllerRegistration(pathPattern, redirectUrl);
    registration.setApplicationContext(this.applicationContext);
    this.redirectRegistrations.add(registration);
    return registration;
  }

  /**
   * Map a simple controller to the given URL path (or pattern) in order to
   * set the response status to the given code without rendering a body.
   */
  public void addStatusController(String pathPattern, HttpStatusCode statusCode) {
    ViewControllerRegistration registration = new ViewControllerRegistration(pathPattern);
    registration.setApplicationContext(this.applicationContext);
    registration.setStatusCode(statusCode);
    registration.getViewController().setStatusOnly(true);
    this.registrations.add(registration);
  }

  /**
   * Map a simple controller to the given URL path (or pattern) in order to set
   * the response status to the given code without rendering a body.
   */
  public void addStatusController(String pathPattern, Integer status) {
    addStatusController(pathPattern, HttpStatusCode.valueOf(status));
  }

  /**
   * Map a URL path or pattern to a view controller to render a response with
   * the configured status code and view.
   * <p>Patterns such as {@code "/admin/**"} or {@code "/articles/{articlename:\\w+}"}
   * are supported.
   *
   * <p><strong>Note:</strong> If an {@code @RequestMapping} method is mapped
   * to a URL for any HTTP method then a view controller cannot handle the
   * same URL. For this reason it is recommended to avoid splitting URL
   * handling across an annotated controller and a view controller.
   */
  public void registerWebViewXml() {
    ClassPathResource resource = new ClassPathResource(webMvcConfigLocation);
    if (resource.exists()) {
      log.info("Using default web mvc configuration resource: '{}'", webMvcConfigLocation);
      registerWebViewXml(resource);
    }
    else {
      log.warn("Web mvc configuration resource not found.");
    }
  }

  public void registerWebViewXml(Resource resource) {
    if (!resource.exists()) {
      throw new InfraConfigurationException(
              "Your provided configuration location: [" + resource + "], does not exist");
    }
    try {
      configure(resource);
    }
    catch (Exception e) {
      throw new InfraConfigurationException("web-mvc xml parsing error", e);
    }
  }

  /**
   * Specify the order to use for the {@code HandlerMapping} used to map view
   * controllers relative to other handler mappings configured in Framework MVC.
   * <p>By default this is set to 1, i.e. right after annotated controllers,
   * which are ordered at 0.
   */
  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * Return the {@code HandlerMapping} that contains the registered view
   * controller mappings, or {@code null} for no registrations.
   */
  @Nullable
  protected SimpleUrlHandlerMapping buildHandlerMapping() {
    if (this.registrations.isEmpty() && this.redirectRegistrations.isEmpty()) {
      return null;
    }

    LinkedHashMap<String, Object> urlMap = new LinkedHashMap<>();
    for (ViewControllerRegistration registration : this.registrations) {
      urlMap.put(registration.getUrlPath(), registration.getViewController());
    }
    for (RedirectViewControllerRegistration registration : this.redirectRegistrations) {
      urlMap.put(registration.getUrlPath(), registration.getViewController());
    }

    return new SimpleUrlHandlerMapping(urlMap, this.order);
  }

  // ---------------------------------------------------------

  /**
   * configure from a xml file
   *
   * @see StringUtils#split(String)
   * @see StringUtils#isSplitable(char)
   */
  protected void configure(Resource resource) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver((publicId, systemId) -> {
      if (systemId.contains(DTD_NAME) || publicId.contains(DTD_NAME)) {
        return new InputSource(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
      }
      return null;
    });

    try (InputStream inputStream = resource.getInputStream()) {
      Element root = builder.parse(inputStream).getDocumentElement();
      if (ROOT_ELEMENT.equals(root.getNodeName())) { // root element
        log.debug("Found configuration file: [{}].", resource);

        NodeList nl = root.getChildNodes();
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
          Node node = nl.item(i);
          if (node instanceof Element ele) {
            String nodeName = ele.getNodeName();
            log.debug("Found Element: [{}]", nodeName);

            if (ELEMENT_CONTROLLER.equals(nodeName)) {
              configController(ele);
            } // ELEMENT_RESOURCES // TODO
            else {
              log.warn("This element: [{}] is not supported in this version: [{}].", nodeName, Version.instance);
            }
          }
        }
      }
    }
  }

  /**
   * Start configuration
   *
   * @param controller the controller element
   * @since 2.3.7
   */
  protected void configController(Element controller) {
    Assert.notNull(controller, "'controller' element can't be null");

    // <controller/> element
    String prefix = controller.getAttribute(ATTR_PREFIX); // prefix
    String suffix = controller.getAttribute(ATTR_SUFFIX); // suffix

    NodeList nl = controller.getChildNodes();
    int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      Node node = nl.item(i);
      if (node instanceof Element) {
        String nodeName = node.getNodeName();
        // @since 2.3.3
        if (nodeName.equals(ELEMENT_ACTION)) {// <action/>
          processAction(prefix, suffix, (Element) node);
        }
        else {
          log.warn("This element: [{}] is not supported.", nodeName);
        }
      }
    }
  }

  protected void processAction(String prefix, String suffix, Element action) {
    String path = action.getAttribute(ATTR_NAME); // action name
    String resource = action.getAttribute(ATTR_RESOURCE); // resource
    String contentType = action.getAttribute(ATTR_CONTENT_TYPE); // content type
    String status = action.getAttribute(ATTR_STATUS); // status
    String type = action.getAttribute(ATTR_TYPE); // type forward, redirect

    if (StringUtils.isEmpty(path)) {
      throw new InfraConfigurationException(
              "You must specify a 'name' attribute like this: [<action resource=\"https://taketoday.cn\" name=\"TODAY-BLOG\" type=\"redirect\"/>]");
    }

    path = resolveEmbeddedVariables(path);
    path = StringUtils.formatURL(path); // path

    if ("redirect".equals(type)) {
      var registration = addRedirectViewController(path, resource);
      if (StringUtils.isNotEmpty(status)) {
        registration.setStatusCode(HttpStatusCode.valueOf(Integer.parseInt(status)));
      }
    }
    else {
      ViewControllerRegistration registration = addViewController(path);
      if (StringUtils.isNotEmpty(status)) {
        registration.setStatusCode(HttpStatusCode.valueOf(Integer.parseInt(status)));
      }

      if (StringUtils.isNotEmpty(resource)) {
        String resourceAppender = prefix + resource + suffix;
        resource = resolveEmbeddedVariables(resourceAppender);
      }

      registration.setViewName(resource);

      // @since 2.3.3

      if (StringUtils.isNotEmpty(contentType)) {
        registration.setContentType(contentType);
      }
    }
  }

  /** @since 3.0.3 */
  protected String resolveEmbeddedVariables(String expression) {
    return embeddedValueResolver.resolveStringValue(expression);
  }

}

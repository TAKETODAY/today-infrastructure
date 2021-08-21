/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.registry;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebConstant;
import cn.taketoday.web.handler.ViewController;
import cn.taketoday.web.view.ResultHandler;

import static cn.taketoday.core.ConfigurationException.nonNull;

/**
 * @author TODAY <br>
 * 2019-12-23 22:10
 */
public class ViewControllerHandlerRegistry extends AbstractUrlHandlerRegistry {
  // the dtd
  public static final String DTD_NAME = "web-configuration";

  // config
  public static final String ATTR_CLASS = "class";
  public static final String ATTR_RESOURCE = "resource";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_ORDER = "order";
  public static final String ATTR_METHOD = "method";
  /** resource location @since 2.3.7 */
  public static final String ATTR_PREFIX = "prefix";
  public static final String ATTR_SUFFIX = "suffix";

  /**
   * The resoure's content type
   *
   * @since 2.3.3
   */
  public static final String ATTR_CONTENT_TYPE = "content-type";
  /** The response status @since 2.3.7 */
  public static final String ATTR_STATUS = "status";

  public static final String ELEMENT_ACTION = "action";
  public static final String ELEMENT_CONTROLLER = "controller";
  public static final String ROOT_ELEMENT = "Web-Configuration";

  public final ViewController getViewController(String key) {
    return getViewController(key, RequestContextHolder.currentContext());
  }

  public final ViewController getViewController(String key, RequestContext context) {
    final Object obj = lookupHandler(key, context);
    if (obj instanceof ViewController) {
      return (ViewController) obj;
    }
    return null;
  }

  public void register(String requestURI, ViewController viewController) {
    registerHandler(StringUtils.checkUrl(requestURI), viewController);
  }

  public void register(ViewController viewController, String... requestURI) {
    for (final String path : nonNull(requestURI, "request URIs must not be null")) {
      register(path, viewController);
    }
  }

  /**
   * Map a view controller to the given URL path (or pattern) in order to render a
   * response with a pre-configured status code and view.
   * <p>
   * Patterns like {@code "/articles/**"} or {@code "/articles/{id:\\w+}"} are
   * allowed. See {@link AntPathMatcher} for more details on the syntax.
   *
   * @param pathPattern
   *         Patterns like {@code "/articles/**"} or
   *         {@code "/articles/{id:\\w+}"} are allowed. See
   *         {@link AntPathMatcher} for more details on the syntax.
   *
   * @return {@link ViewController}
   */
  public ViewController addViewController(String pathPattern) {
    final ViewController viewController = new ViewController();
    register(pathPattern, viewController);
    return viewController;
  }

  /**
   * Map a view controller to the given URL path (or pattern) in order to render a
   * response with a pre-configured status code and view.
   * <p>
   * Patterns like {@code "/articles/**"} or {@code "/articles/{id:\\w+}"} are
   * allowed. See {@link AntPathMatcher} for more details on the syntax.
   *
   * @param pathPattern
   *         Patterns like {@code "/articles/**"} or
   *         {@code "/articles/{id:\\w+}"} are allowed. See
   *         {@link AntPathMatcher} for more details on the syntax.
   * @param resource
   *         resource location,such as redirect url or view template resource
   *         <b>Or other type of resource</b>
   *
   * @return {@link ViewController}
   */
  public ViewController addViewController(String pathPattern, Object resource) {
    return addViewController(pathPattern)
            .setResource(resource);
  }

  /**
   * Map a view controller to the given URL path (or pattern) in order to redirect
   * to another URL.
   */
  public ViewController addRedirectViewController(String pathPattern, String redirectUrl) {
    return addViewController(pathPattern)
            .setResource(ResultHandler.REDIRECT_URL_PREFIX.concat(redirectUrl));
  }

  /**
   * Map a simple controller to the given URL path (or pattern) in order to set
   * the response status to the given code without rendering a body.
   */
  public ViewController addStatusController(String pathPattern, Integer status) {
    return addViewController(pathPattern)
            .setStatus(status);
  }

  // ---------------------------------------------------------

  /**
   * configure {@link ViewController}s from a xml file
   *
   * @param webMvcConfigLocation
   *         Configuration File location , split-able
   *
   * @see StringUtils#split(String)
   * @see StringUtils#isSplitable(char)
   */
  public void configure(final String webMvcConfigLocation) throws Exception {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    final DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver((publicId, systemId) -> {
      if (systemId.contains(DTD_NAME) || publicId.contains(DTD_NAME)) {
        return new InputSource(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
      }
      return null;
    });

    for (final String file : StringUtils.split(webMvcConfigLocation)) {
      final Resource resource = ResourceUtils.getResource(file);
      if (!resource.exists()) {
        throw new ConfigurationException("Your Provided Configuration File: [" + file + "], Does Not Exist");
      }
      try (final InputStream inputStream = resource.getInputStream()) {
        final Element root = builder.parse(inputStream).getDocumentElement();
        if (ROOT_ELEMENT.equals(root.getNodeName())) { // root element

          log.info("Found Configuration File: [{}].", resource);
          registerFromXml(root);
        }
      }
    }
  }

  /**
   * configure with xml file
   */
  protected void registerFromXml(final Element root) {

    final NodeList nl = root.getChildNodes();
    final int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      final Node node = nl.item(i);
      if (node instanceof Element) {
        final Element ele = (Element) node;
        final String nodeName = ele.getNodeName();

        log.debug("Found Element: [{}]", nodeName);

        if (ELEMENT_CONTROLLER.equals(nodeName)) {
          configController(ele);
        } // ELEMENT_RESOURCES // TODO
        else {
          log.warn("This This element: [{}] is not supported in this version: [{}].", nodeName, WebConstant.VERSION);
        }
      }
    }
  }

  /**
   * Start configuration
   *
   * @param controller
   *         the controller element
   *
   * @since 2.3.7
   */
  protected void configController(final Element controller) {
    Assert.notNull(controller, "'controller' element can't be null");

    // <controller/> element
    final String name = controller.getAttribute(ATTR_NAME); // controller name
    final String prefix = controller.getAttribute(ATTR_PREFIX); // prefix
    final String suffix = controller.getAttribute(ATTR_SUFFIX); // suffix
    final String className = controller.getAttribute(ATTR_CLASS); // class

    // @since 2.3.3
    final Object controllerBean = getControllerBean(name, className);

    final NodeList nl = controller.getChildNodes();
    final int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      final Node node = nl.item(i);
      if (node instanceof Element) {
        String nodeName = node.getNodeName();
        // @since 2.3.3
        if (nodeName.equals(ELEMENT_ACTION)) {// <action/>
          processAction(prefix, suffix, (Element) node, controllerBean);
        }
        else {
          log.warn("This element: [{}] is not supported.", nodeName);
        }
      }
    }
  }

  protected Object getControllerBean(final String name, final String className) {
    Object controllerBean = null;
    final WebApplicationContext context = obtainApplicationContext();
    if (StringUtils.isNotEmpty(name)) {
      if (StringUtils.isEmpty(className)) {
        final Object bean = context.getBean(name);
        Assert.state(bean != null, () -> "You must provide a bean named: [" + name + "] or a 'class' attribute");
        return bean;
      }
      else {
        final Class<?> beanClass = ClassUtils.loadClass(className);
        if ((controllerBean = context.getBean(name, beanClass)) == null) {
          context.registerBean(name, beanClass);
          controllerBean = context.getBean(name, beanClass);
        }
      }
    }
    else if (StringUtils.isNotEmpty(className)) {
      final Class<?> beanClass = ClassUtils.loadClass(className);
      if ((controllerBean = context.getBean(beanClass)) == null) {
        context.registerBean(beanClass);
        controllerBean = context.getBean(beanClass);
      }
    }
    return controllerBean;
  }

  protected void processAction(final String prefix,
                               final String suffix,
                               final Element action,
                               final Object controller) //
  {

    String name = action.getAttribute(ATTR_NAME); // action name
    String order = action.getAttribute(ATTR_ORDER); // order
    String method = action.getAttribute(ATTR_METHOD); // handler method
    String resource = action.getAttribute(ATTR_RESOURCE); // resource
    String contentType = action.getAttribute(ATTR_CONTENT_TYPE); // content type
    final String status = action.getAttribute(ATTR_STATUS); // status

    if (StringUtils.isEmpty(name)) {
      throw new ConfigurationException(
              "You must specify a 'name' attribute like this: [<action resource=\"https://taketoday.cn\" name=\"TODAY-BLOG\" type=\"redirect\"/>]");
    }

    Method handlerMethod = null;

    if (StringUtils.isNotEmpty(method)) {
      if (controller == null) {
        throw new ConfigurationException(
                "You must specify a 'class' attribute like this: [<controller class=\"xxx.XMLController\" name=\"xmlController\" />]");
      }
      for (final Method targetMethod : ReflectionUtils.getDeclaredMethods(controller.getClass())) {
        if (!targetMethod.isBridge() && method.equals(targetMethod.getName())) {
          handlerMethod = targetMethod;
          break;
        }
      }
      if (handlerMethod == null) {
        throw new ConfigurationException("You must specify a method: [" + method + "] in class :[" + controller.getClass() + "]");
      }
    }

    final ViewController mapping = new ViewController(controller, handlerMethod);

    if (StringUtils.isNotEmpty(status)) {
      mapping.setStatus(Integer.valueOf(status));
    }

    if (StringUtils.isNotEmpty(order)) {
      mapping.setOrder(Integer.parseInt(order));
    }

    if (StringUtils.isNotEmpty(resource)) {
      final StringBuilder resourceAppender = //
              new StringBuilder(prefix.length() + resource.length() + suffix.length())
                      .append(prefix)
                      .append(resource)
                      .append(suffix);
      resource = resolveVariables(resourceAppender.toString());
    }
    mapping.setResource(resource);
    // @since 2.3.3
    if (StringUtils.isNotEmpty(contentType)) {
      mapping.setContentType(contentType);
    }

    name = resolveVariables(getContextPath().concat(StringUtils.checkUrl(name)));

    register(name, mapping);
  }

}


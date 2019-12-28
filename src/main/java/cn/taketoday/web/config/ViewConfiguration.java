/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.web.config;

import static cn.taketoday.context.utils.ContextUtils.resolveValue;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Properties;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.ViewController;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;

/**
 * @author TODAY <br>
 *         2018-06-23 16:19:53
 */
public class ViewConfiguration {

    private final Properties variables;
    private final BeanNameCreator beanNameCreator;
    private final WebApplicationContext applicationContext;

    private ViewControllerHandlerRegistry handlerRegistry;

    public ViewConfiguration(WebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        final Environment environment = applicationContext.getEnvironment();

        this.variables = environment.getProperties();
        this.beanNameCreator = environment.getBeanNameCreator();
        this.handlerRegistry = applicationContext.getBean(ViewControllerHandlerRegistry.class);
    }

    /**
     * 
     * Start configuration
     * 
     * @param controller
     *            the controller element
     * @throws Exception
     *             if any {@link Exception} occurred
     */
    public void configuration(final Element controller) throws Exception {

        Objects.requireNonNull(controller, "'controller' element can't be null");

        // <controller/> element
        String name = controller.getAttribute(Constant.ATTR_NAME); // controller name
        String prefix = controller.getAttribute(Constant.ATTR_PREFIX); // prefix
        String suffix = controller.getAttribute(Constant.ATTR_SUFFIX); // suffix
        String className = controller.getAttribute(Constant.ATTR_CLASS); // class

        // @since 2.3.3
        Class<?> beanClass = null;
        Object controllerBean = null;
        if (StringUtils.isNotEmpty(className)) {
            beanClass = ClassUtils.loadClass(className);
            if (StringUtils.isEmpty(name)) {
                name = beanNameCreator.create(beanClass);
            }
            final WebApplicationContext context = this.applicationContext;
            if (!context.containsBeanDefinition(name, beanClass)) {
                context.registerBean(name, beanClass); // fix
            }
            controllerBean = context.getBean(name); // fix
        }

        NodeList nl = controller.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                String nodeName = node.getNodeName();
                // @since 2.3.3
                if (nodeName.equals(Constant.ELEMENT_ACTION)) {// <action/>
                    processAction(prefix, suffix, (Element) node, beanClass, controllerBean);
                }
                else {
                    LoggerFactory.getLogger(ViewConfiguration.class)
                            .warn("This element: [{}] is not supported.", nodeName);
                }
            }
        }
    }

    protected ViewController processAction(final String prefix,
                                           final String suffix,
                                           final Element action,
                                           final Class<?> controller,
                                           final Object controllerBean) throws Exception //
    {

        String name = action.getAttribute(Constant.ATTR_NAME); // action name
        String method = action.getAttribute(Constant.ATTR_METHOD); // handler method
        String resource = action.getAttribute(Constant.ATTR_RESOURCE); // resource
        String contentType = action.getAttribute(Constant.ATTR_CONTENT_TYPE); // content type
        final String status = action.getAttribute(Constant.ATTR_STATUS); // status

        if (StringUtils.isEmpty(name)) {
            throw new ConfigurationException("You must specify a 'name' attribute like this: [<action resource=\"https://taketoday.cn\" name=\"TODAY-BLOG\" type=\"redirect\"/>]");
        }

        Method handlerMethod = null;

        if (StringUtils.isNotEmpty(method)) {
            for (final Method targetMethod : controller.getDeclaredMethods()) {
                if (!targetMethod.isBridge() && method.equals(targetMethod.getName())) {
                    handlerMethod = targetMethod;
                    break;
                }
            }
        }

        final ViewController mapping = new ViewController(controllerBean, handlerMethod);

        if (StringUtils.isNotEmpty(status)) {
            mapping.setStatus(Integer.parseInt(status));
        }

        if (StringUtils.isNotEmpty(resource)) {
            resource = resolveValue(
                                    new StringBuilder(prefix.length() + resource.length() + suffix.length())
                                            .append(prefix)
                                            .append(resource)
                                            .append(suffix).toString(), String.class, variables);
        }
        mapping.setAssetsPath(resource);
        { // @since 2.3.3
            if (StringUtils.isNotEmpty(contentType)) {
                mapping.setContentType(contentType);
            }
        }

        name = resolveValue(applicationContext.getContextPath().concat(StringUtils.checkUrl(name)), String.class, variables);

        handlerRegistry.register(name, mapping);
        return mapping;
    }

}

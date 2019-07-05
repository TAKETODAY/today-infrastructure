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

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Properties;

import javax.el.ELProcessor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.mapping.ViewMapping;
import cn.taketoday.web.servlet.ViewDispatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2018-06-23 16:19:53
 */
@Slf4j
@Singleton(Constant.VIEW_CONFIG)
public class ViewConfiguration implements WebApplicationContextAware {

    private String contextPath;
    private Properties variables;
    private ELProcessor elProcessor;
    private BeanNameCreator beanNameCreator;
    private WebApplicationContext applicationContext;

    /**
     * 
     * Start configuration
     * 
     * @param controller
     *            the controller element
     * @throws Exception
     *             if any {@link Exception} occurred
     */
    public void configuration(Element controller) throws Exception {

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
            beanClass = ClassUtils.forName(className);
            if (StringUtils.isEmpty(name)) {
                name = beanNameCreator.create(beanClass);
            }
            if (!applicationContext.containsBeanDefinition(beanClass)) {
                applicationContext.registerBean(name, new DefaultBeanDefinition(name, beanClass));
                applicationContext.refresh(name);
            }
            controllerBean = applicationContext.getBean(beanClass);
        }

        NodeList nl = controller.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                String nodeName = node.getNodeName();
                // @since 2.3.3
                if (nodeName.equals(Constant.ELEMENT_ACTION)) {// <action/>
                    processAction(prefix, suffix, (Element) node, beanClass)//
                            .setController(controllerBean);
                }
                else {
                    log.warn("This element: [{}] is not supported.", nodeName);
                }
            }
        }
    }

    /**
     * @param prefix
     * @param suffix
     * @param action
     * @param class_
     * @return
     * @throws Exception
     */
    private ViewMapping processAction(String prefix, String suffix, Element action, Class<?> class_) throws Exception {

        ViewMapping mapping = new ViewMapping();

        String name = action.getAttribute(Constant.ATTR_NAME); // action name
        String type = action.getAttribute(Constant.ATTR_TYPE); // action type
        String method = action.getAttribute(Constant.ATTR_METHOD); // handler method
        String resource = action.getAttribute(Constant.ATTR_RESOURCE); // resource
        String contentType = action.getAttribute(Constant.ATTR_CONTENT_TYPE); // content type
        final String status = action.getAttribute(Constant.ATTR_STATUS); // status

        if (StringUtils.isNotEmpty(status)) {
            mapping.setStatus(Integer.parseInt(status));
        }

        if (StringUtils.isEmpty(name)) {
            throw new ConfigurationException(//
                    "You must specify a 'name' attribute like this: [<action resource=\"https://taketoday.cn\" name=\"TODAY-BLOG\" type=\"redirect\"/>]"//
            );
        }

        if (StringUtils.isNotEmpty(method)) {
            Method handelrMethod = class_.getDeclaredMethod(method, HttpServletRequest.class, HttpServletResponse.class);
            mapping.setAction(handelrMethod);
            Class<?> returnType = handelrMethod.getReturnType();

            if (returnType == String.class) {
                mapping.setReturnType(Constant.RETURN_STRING);
            }
            else if (Image.class.isAssignableFrom(returnType) || RenderedImage.class.isAssignableFrom(returnType)) {
                mapping.setReturnType(Constant.RETURN_IMAGE);
            }
            else if (returnType == void.class) {
                mapping.setReturnType(Constant.RETURN_VOID);
            }
            else {
                mapping.setReturnType(Constant.RETURN_OBJECT);
            }
        }

        resource = ContextUtils.resolvePlaceholder(variables, prefix + resource + suffix, false);
        if (resource == null) {
            resource = elProcessor.getValue(prefix + resource + suffix, String.class);
        }
        if (Constant.VALUE_REDIRECT.equals(type)) { // redirect
            mapping.setReturnType(Constant.TYPE_REDIRECT);
            if (!resource.startsWith(Constant.HTTP)) {
                resource = contextPath + resource;
            }
        }
        else if (Constant.VALUE_FORWARD.equals(type)) { // forward
            mapping.setReturnType(Constant.TYPE_FORWARD);
        }

        mapping.setAssetsPath(resource);
        { // @since 2.3.3
            if (StringUtils.isNotEmpty(contentType)) {
                mapping.setContentType(contentType);
            }
        }

        name = ContextUtils.resolvePlaceholder(variables, //
                contextPath + (name.startsWith("/") ? name : "/" + name));

        ViewDispatcher.register(name, mapping);
        log.info("View Mapped [{} -> {}]", name, mapping);
        return mapping;
    }

    @Override
    public void setWebApplicationContext(WebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        final ConfigurableEnvironment environment = applicationContext.getEnvironment();

        this.variables = environment.getProperties();
        this.elProcessor = environment.getELProcessor();
        this.beanNameCreator = environment.getBeanNameCreator();
        this.contextPath = applicationContext.getServletContext().getContextPath();
    }

}

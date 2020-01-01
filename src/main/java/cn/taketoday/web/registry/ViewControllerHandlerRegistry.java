/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.AntPathMatcher;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.io.Resource;
import cn.taketoday.web.handler.ViewController;

/**
 * @author TODAY <br>
 *         2019-12-23 22:10
 */
@MissingBean
public class ViewControllerHandlerRegistry extends MappedHandlerRegistry {

    public ViewControllerHandlerRegistry() {
        this(new HashMap<>());
    }

    public ViewControllerHandlerRegistry(int initialCapacity) {
        this(new HashMap<>(initialCapacity));
    }

    public ViewControllerHandlerRegistry(Map<String, Object> viewControllers) {
        super(viewControllers);
    }

    public ViewControllerHandlerRegistry(Map<String, Object> viewControllers, int order) {
        super(viewControllers);
        setOrder(order);
    }

    public final ViewController getViewController(String key) {
        final Object obj = getHandlers().get(key);
        if (obj instanceof ViewController) {
            return (ViewController) obj;
        }
        return null;
    }

    public void register(String requestURI, ViewController viewController) {
        registerHandler(requestURI, viewController);
    }

    public void register(ViewController viewController, String... requestURI) {
        registerHandler(viewController, requestURI);
    }

    /**
     * Map a view controller to the given URL path (or pattern) in order to render a
     * response with a pre-configured status code and view.
     * <p>
     * Patterns like {@code "/articles/**"} or {@code "/articles/{id:\\w+}"} are
     * allowed. See {@link AntPathMatcher} for more details on the syntax.
     * 
     * @param pathPattern
     *            Patterns like {@code "/articles/**"} or
     *            {@code "/articles/{id:\\w+}"} are allowed. See
     *            {@link AntPathMatcher} for more details on the syntax.
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
     *            Patterns like {@code "/articles/**"} or
     *            {@code "/articles/{id:\\w+}"} are allowed. See
     *            {@link AntPathMatcher} for more details on the syntax.
     * @param assetsPath
     *            {@link Resource} location
     * @return {@link ViewController}
     */
    public ViewController addViewController(String pathPattern, String assetsPath) {
        return addViewController(pathPattern).setAssetsPath(assetsPath);
    }

}

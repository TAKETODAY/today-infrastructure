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

import static cn.taketoday.context.exception.ConfigurationException.nonNull;
import static cn.taketoday.web.RequestContextHolder.currentContext;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.EmptyObject;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.utils.ConcurrentCache;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.PatternHandler;
import cn.taketoday.web.handler.ResourceMapping;
import cn.taketoday.web.handler.ResourceMatchResult;
import cn.taketoday.web.handler.ResourceRequestHandler;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resource.DefaultResourceResolver;
import cn.taketoday.web.resource.WebResourceResolver;

/**
 * @author TODAY <br>
 *         2019-05-15 21:34
 * @since 2.3.7
 */
public class ResourceHandlerRegistry extends MappedHandlerRegistry implements WebApplicationInitializer {

    private int contextPathLength;
    private WebResourceResolver resourceResolver;
    private ConcurrentCache<String, Object> patternMatchingCache;
    private final List<ResourceMapping> resourceMappings = new ArrayList<>();

    public ResourceHandlerRegistry() {
        this(new DefaultResourceResolver());
    }

    public ResourceHandlerRegistry(@Autowired(required = false) WebResourceResolver resourceResolver) {
        setResourceResolver(nonNull(resourceResolver, "web resource resolver must not be null"));
        this.patternMatchingCache = new ConcurrentCache<>(64);
    }

    public ResourceMapping addResourceMapping(String... pathPatterns) {
        ResourceMapping resourceMapping = new ResourceMapping(null, pathPatterns);
        getResourceMappings().add(resourceMapping);
        return resourceMapping;
    }

    @SafeVarargs
    public final <T extends HandlerInterceptor> ResourceMapping addResourceMapping(Class<T>... handlerInterceptors) {
        final HandlerMethodRegistry registry = obtainApplicationContext().getBean(HandlerMethodRegistry.class);

        final HandlerInterceptor[] interceptors = registry.getInterceptors(handlerInterceptors);
        ResourceMapping resourceMapping = new ResourceMapping(ObjectUtils.isEmpty(interceptors) ? null : interceptors);
        getResourceMappings().add(resourceMapping);
        return resourceMapping;
    }

    public boolean isEmpty() {
        return resourceMappings.isEmpty();
    }

    public List<ResourceMapping> getResourceMappings() {
        return resourceMappings;
    }

    @Override
    protected String computeKey(RequestContext context) {
        return requestPath(context.requestURI(), contextPathLength);
    }

    @Override
    protected Object lookupHandler(final String handlerKey) {
        final Object handler = super.lookupHandler(handlerKey);
        if (handler instanceof ResourceMatchResult) {
            currentContext().attribute(Constant.RESOURCE_MATCH_RESULT, handler);
            return ((ResourceMatchResult) handler).getHandler();
        }
        else if (handler instanceof ResourceRequestHandler) {
            currentContext()
                    .attribute(Constant.RESOURCE_MATCH_RESULT,
                               new ResourceMatchResult(handlerKey,
                                                       handlerKey,
                                                       getPathMatcher(),
                                                       ((ResourceRequestHandler) handler)
                               )
                    );
        }
        return handler;
    }

    @Override
    protected Object lookupPatternHandler(final String handlerKey) {
        Object ret = patternMatchingCache.get(handlerKey);
        if (ret == null) {
            final PatternHandler matched = matchingPatternHandler(handlerKey);
            if (matched == null) {
                patternMatchingCache.put(handlerKey, EmptyObject.INSTANCE);
            }
            else {
                ret = new ResourceMatchResult(handlerKey,
                                              matched.getPattern(),
                                              getPathMatcher(),
                                              (ResourceRequestHandler) matched.getHandler()
                );
                patternMatchingCache.put(handlerKey, ret);
            }
        }
        else if (ret == EmptyObject.INSTANCE) {
            return null;
        }
        return ret;
    }

    /**
     * Get request path
     * 
     * @param requestURI
     *            Current requestURI
     * @param length
     *            context path length
     * @return Decoded request path
     */
    protected String requestPath(final String requestURI, final int length) {
        return StringUtils.decodeUrl(length == 0 ? requestURI : requestURI.substring(length));
    }

    public WebResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(WebResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void onStartup(WebApplicationContext context) throws Throwable {
        this.contextPathLength = context.getContextPath().length();
        final WebResourceResolver resourceResolver = getResourceResolver();

        for (final ResourceMapping resourceMapping : OrderUtils.reversedSort(getResourceMappings())) {
            final String[] pathPatterns = resourceMapping.getPathPatterns();
            registerHandler(new ResourceRequestHandler(resourceMapping, resourceResolver), pathPatterns);
        }
    }

}

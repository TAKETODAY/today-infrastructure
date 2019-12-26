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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.mapping;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.ActionConfiguration;
import cn.taketoday.web.handler.ResourceRequestHandler;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resolver.DefaultResourceResolver;
import cn.taketoday.web.resolver.WebResourceResolver;

/**
 * @author TODAY <br>
 *         2019-05-15 21:34
 * @since 2.3.7
 */
@MissingBean
public class ResourceHandlerRegistry extends MappedHandlerRegistry {

    private int contextPathLength;
    private WebResourceResolver resourceResolver;
    private final List<ResourceMapping> resourceMappings = new ArrayList<>();

    public ResourceHandlerRegistry() {
        this(new DefaultResourceResolver());
    }

    public ResourceHandlerRegistry(@Autowired(required = false) WebResourceResolver resourceResolver) {
        setResourceResolver(nonNull(resourceResolver, "web resource resolver must not be null"));
    }

    public ResourceMapping addResourceMapping(String... pathPatterns) {
        ResourceMapping resourceMapping = new ResourceMapping(null, pathPatterns);
        getResourceMappings().add(resourceMapping);
        return resourceMapping;
    }

    @SafeVarargs
    public final <T extends HandlerInterceptor> ResourceMapping addResourceMapping(Class<T>... handlerInterceptors) {

        final ActionConfiguration actionConfiguration = //
                ContextUtils.getApplicationContext().getBean(Constant.ACTION_CONFIG, ActionConfiguration.class);

        final HandlerInterceptor[] interceptors = actionConfiguration.addInterceptors(handlerInterceptors);

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

    protected ResourceHandlerRegistry sortResourceMappings() {
        OrderUtils.reversedSort(resourceMappings);
        return this;
    }

    @Override
    protected String computeKey(RequestContext context) {
        return requestPath(context.requestURI(), contextPathLength);
    }

    @Override
    protected Object lookupHandler(final String handlerKey, final RequestContext context) {
        final Object handler = super.lookupHandler(handlerKey, context);
        if (handler instanceof ResourceMappingMatchResult) {
            context.attribute(Constant.RESOURCE_MAPPING_MATCH_RESULT, handler);
        }
        else if (handler instanceof ResourceRequestHandler) {
            context.attribute(Constant.RESOURCE_MAPPING_MATCH_RESULT,
                              new ResourceMappingMatchResult(handlerKey,
                                                             handlerKey,
                                                             getPathMatcher(),
                                                             ((ResourceRequestHandler) handler).getMapping()));
        }
        return handler;
    }

    @Override
    protected Object matchingPatternHandler(String handlerKey, PatternMapping[] patternMappings) {

        final PathMatcher pathMatcher = getPathMatcher();
        for (PatternMapping patternMapping : patternMappings) {
            final String pattern = patternMapping.getPattern();
            if (pathMatcher.match(pattern, handlerKey)) {
                final ResourceRequestHandler handler = (ResourceRequestHandler) patternMapping.getHandler();
                return new ResourceMappingMatchResult(handlerKey, pattern, pathMatcher, handler.getMapping());
            }
        }
        return null;
    }

    public Object lookups(RequestContext context) {
        final String requestURI = context.requestURI();
        final String path = requestPath(requestURI, contextPathLength);

        final ResourceMappingMatchResult matchResult = lookupResourceMapping(path);
        if (matchResult == null) {
            return null;
        }

        context.attribute("matchResult", matchResult);

        return getResourceResolver().resolveResource(matchResult);
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

    /**
     * Looking for {@link ResourceMapping}
     * 
     * @param path
     *            Request path
     * @param pathMatcher
     *            {@link PathMatcher}
     * @return Mapped {@link ResourceMapping}
     */
    protected ResourceMappingMatchResult lookupResourceMapping(final String path) {

        final PathMatcher pathMatcher = getPathMatcher();

        for (final ResourceMapping resourceMapping : getResourceMappings()) {
            for (final String pathPattern : resourceMapping.getPathPatterns()) {
                if (pathMatcher.match(pathPattern, path)) {
                    return new ResourceMappingMatchResult(path, pathPattern, pathMatcher, resourceMapping);
                }
            }
        }
        return null;
    }

    @Override
    protected void initApplicationContext(ApplicationContext context) throws ContextException {
        super.initApplicationContext(context);
        
        sortResourceMappings();
        if (context instanceof WebApplicationContext) {
            this.contextPathLength = ((WebApplicationContext) context).getContextPath().length();
        }
        else {
            throw new ConfigurationException("context must be a WebApplicationContext");
        }

        final WebResourceResolver resourceResolver = getResourceResolver();

        for (final ResourceMapping resourceMapping : getResourceMappings()) {
            final String[] pathPatterns = resourceMapping.getPathPatterns();
            registerHandler(new ResourceRequestHandler(resourceMapping, resourceResolver), pathPatterns);
        }
    }

    public WebResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(WebResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

}

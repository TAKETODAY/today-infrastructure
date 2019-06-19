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
package cn.taketoday.web.config.initializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.Registration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-02-03 12:22
 */
@Getter
@Setter
public abstract class WebComponentInitializer<D extends Registration.Dynamic> implements OrderedInitializer {

    private String name;
    /** Startup order */
    private int order = LOWEST_PRECEDENCE;

    private boolean asyncSupported = false;

    private Set<String> urlMappings = new LinkedHashSet<>();

    private Map<String, String> initParameters = new LinkedHashMap<>();

    private ServletContext servletContext;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

        setServletContext(servletContext);

        D registration = addRegistration(servletContext);
        if (registration != null) {
            configureRegistration(registration);
        }
    }

    protected abstract D addRegistration(ServletContext servletContext);

    protected void configureRegistration(D registration) {
        registration.setAsyncSupported(this.asyncSupported);
        if (!this.initParameters.isEmpty()) {
            registration.setInitParameters(this.initParameters);
        }
    }

    public void setInitParameters(Map<String, String> initParameters) {
        this.initParameters = new LinkedHashMap<>(initParameters);
    }

    public void addInitParameter(String name, String value) {
        this.initParameters.put(name, value);
    }

    public Map<String, String> getInitParameters() {
        return this.initParameters;
    }

    public void setUrlMappings(Collection<String> urlMappings) {
        Objects.requireNonNull(urlMappings, "UrlMappings must not be null");
        this.urlMappings = new LinkedHashSet<>(urlMappings);
    }

    public Collection<String> getUrlMappings() {
        return this.urlMappings;
    }

    public void addUrlMappings(String... urlMappings) {
        Objects.requireNonNull(urlMappings, "UrlMappings must not be null");
        this.urlMappings.addAll(Arrays.asList(urlMappings));
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}

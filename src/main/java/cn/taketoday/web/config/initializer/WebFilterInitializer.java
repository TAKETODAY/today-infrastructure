/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2019 All Rights Reserved.
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

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;

import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-02-03 13:22
 */
@Setter
@Getter
public class WebFilterInitializer<T extends Filter> extends WebComponentInitializer<FilterRegistration.Dynamic> {

	private T filter;
	private boolean matchAfter;
	private DispatcherType[] dispatcherTypes;

	private Set<String> servletNames = new LinkedHashSet<>();

	@Override
	protected Dynamic addRegistration(ServletContext servletContext) {
		return servletContext.addFilter(getFilterName(), filter);
	}

	/**
	 * Get filter name
	 * 
	 * @return
	 */
	public String getFilterName() {
		return filter.getClass().getSimpleName();
	}

	@Override
	protected void configureRegistration(Dynamic registration) {
		super.configureRegistration(registration);

		EnumSet<DispatcherType> dispatcherTypes = null;

		if (this.dispatcherTypes == null) {
			dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
		}
		else {
			dispatcherTypes = EnumSet.noneOf(DispatcherType.class);
			dispatcherTypes.addAll(Arrays.asList(this.dispatcherTypes));
		}

		final Collection<String> urlMappings = getUrlMappings();

		if (servletNames.isEmpty() && urlMappings.isEmpty()) {
			registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, Constant.DEFAULT_MAPPINGS);
		}
		else {
			if (!servletNames.isEmpty()) {
				registration.addMappingForServletNames(dispatcherTypes, this.matchAfter, StringUtils.toStringArray(servletNames));
			}
			if (!urlMappings.isEmpty()) {
				registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, StringUtils.toStringArray(urlMappings));
			}
		}
	}

}

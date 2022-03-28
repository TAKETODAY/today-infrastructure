/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.env;

import cn.taketoday.framework.Application;
import cn.taketoday.framework.json.JsonParser;
import cn.taketoday.framework.json.JsonParserFactory;
import cn.taketoday.framework.origin.Origin;
import cn.taketoday.framework.origin.OriginLookup;
import cn.taketoday.framework.origin.PropertySourceOrigin;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.MutablePropertySources;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.context.support.StandardServletEnvironment;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An {@link EnvironmentPostProcessor} that parses JSON from
 * {@code spring.application.json} or equivalently {@code SPRING_APPLICATION_JSON} and
 * adds it as a map property source to the {@link Environment}. The new properties are
 * added with higher priority than the system properties.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @since 1.3.0
 */
public class ApplicationJsonEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * Name of the {@code spring.application.json} property.
	 */
	public static final String SPRING_APPLICATION_JSON_PROPERTY = "spring.application.json";

	/**
	 * Name of the {@code SPRING_APPLICATION_JSON} environment variable.
	 */
	public static final String SPRING_APPLICATION_JSON_ENVIRONMENT_VARIABLE = "SPRING_APPLICATION_JSON";

	private static final String SERVLET_ENVIRONMENT_CLASS = "cn.taketoday.web."
			+ "context.support.StandardServletEnvironment";

	private static final Set<String> SERVLET_ENVIRONMENT_PROPERTY_SOURCES = new LinkedHashSet<>(
			Arrays.asList(StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME));

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

	private int order = DEFAULT_ORDER;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.stream().map(JsonPropertyValue::get).filter(Objects::nonNull).findFirst()
				.ifPresent((v) -> processJson(environment, v));
	}

	private void processJson(ConfigurableEnvironment environment, JsonPropertyValue propertyValue) {
		JsonParser parser = JsonParserFactory.getJsonParser();
		Map<String, Object> map = parser.parseMap(propertyValue.getJson());
		if (!map.isEmpty()) {
			addJsonPropertySource(environment, new JsonPropertySource(propertyValue, flatten(map)));
		}
	}

	/**
	 * Flatten the map keys using period separator.
	 * @param map the map that should be flattened
	 * @return the flattened map
	 */
	private Map<String, Object> flatten(Map<String, Object> map) {
		Map<String, Object> result = new LinkedHashMap<>();
		flatten(null, result, map);
		return result;
	}

	private void flatten(String prefix, Map<String, Object> result, Map<String, Object> map) {
		String namePrefix = (prefix != null) ? prefix + "." : "";
		map.forEach((key, value) -> extract(namePrefix + key, result, value));
	}

	@SuppressWarnings("unchecked")
	private void extract(String name, Map<String, Object> result, Object value) {
		if (value instanceof Map) {
			if (CollectionUtils.isEmpty((Map<?, ?>) value)) {
				result.put(name, value);
				return;
			}
			flatten(name, result, (Map<String, Object>) value);
		}
		else if (value instanceof Collection) {
			if (CollectionUtils.isEmpty((Collection<?>) value)) {
				result.put(name, value);
				return;
			}
			int index = 0;
			for (Object object : (Collection<Object>) value) {
				extract(name + "[" + index + "]", result, object);
				index++;
			}
		}
		else {
			result.put(name, value);
		}
	}

	private void addJsonPropertySource(ConfigurableEnvironment environment, PropertySource<?> source) {
		MutablePropertySources sources = environment.getPropertySources();
		String name = findPropertySource(sources);
		if (sources.contains(name)) {
			sources.addBefore(name, source);
		}
		else {
			sources.addFirst(source);
		}
	}

	private String findPropertySource(MutablePropertySources sources) {
		if (ClassUtils.isPresent(SERVLET_ENVIRONMENT_CLASS, null)) {
			PropertySource<?> servletPropertySource = sources.stream()
					.filter((source) -> SERVLET_ENVIRONMENT_PROPERTY_SOURCES.contains(source.getName())).findFirst()
					.orElse(null);
			if (servletPropertySource != null) {
				return servletPropertySource.getName();
			}
		}
		return StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;
	}

	private static class JsonPropertySource extends MapPropertySource implements OriginLookup<String> {

		private final JsonPropertyValue propertyValue;

		JsonPropertySource(JsonPropertyValue propertyValue, Map<String, Object> source) {
			super(SPRING_APPLICATION_JSON_PROPERTY, source);
			this.propertyValue = propertyValue;
		}

		@Override
		public Origin getOrigin(String key) {
			return this.propertyValue.getOrigin();
		}

	}

	private static class JsonPropertyValue {

		private static final String[] CANDIDATES = { SPRING_APPLICATION_JSON_PROPERTY,
				SPRING_APPLICATION_JSON_ENVIRONMENT_VARIABLE };

		private final PropertySource<?> propertySource;

		private final String propertyName;

		private final String json;

		JsonPropertyValue(PropertySource<?> propertySource, String propertyName, String json) {
			this.propertySource = propertySource;
			this.propertyName = propertyName;
			this.json = json;
		}

		String getJson() {
			return this.json;
		}

		Origin getOrigin() {
			return PropertySourceOrigin.get(this.propertySource, this.propertyName);
		}

		static JsonPropertyValue get(PropertySource<?> propertySource) {
			for (String candidate : CANDIDATES) {
				Object value = propertySource.getProperty(candidate);
				if (value instanceof String && StringUtils.hasLength((String) value)) {
					return new JsonPropertyValue(propertySource, candidate, (String) value);
				}
			}
			return null;
		}

	}

}

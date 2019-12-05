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
package cn.taketoday.framework.env;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import cn.taketoday.context.env.StandardEnvironment;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.annotation.PropertiesSource;
import cn.taketoday.framework.utils.ApplicationUtils;

/**
 * @author TODAY <br>
 *         2019-06-17 22:34
 */
public class StandardWebEnvironment extends StandardEnvironment {

    private static final Logger log = LoggerFactory.getLogger(StandardWebEnvironment.class);

    private final String[] arguments;
    private final Class<?> applicationClass;

    public StandardWebEnvironment(Class<?> applicationClass, String... arguments) {
        this.arguments = arguments;
        this.applicationClass = applicationClass;
    }

    @Override
    public void loadProperties() throws IOException {

        // load default properties source : application.yaml or application.properties
        final Set<String> locations = new HashSet<>(8);
        final Resource propertiesResource = getPropertiesResource(locations);

        if (propertiesResource.exists()) { // load
            loadProperties(propertiesResource);
        }
        else {
            super.loadProperties(Constant.BLANK); // scan class path properties files
        }

        // load properties from starter class annotated @PropertiesSource
        final Class<?> applicationClass = this.applicationClass;
        if (applicationClass != null && applicationClass.isAnnotationPresent(PropertiesSource.class)) {
            for (final String propertiesLocation : //@off
                    StringUtils.split(applicationClass.getAnnotation(PropertiesSource.class).value())) {
                
                if(!locations.contains(propertiesLocation)) {
                    loadProperties(propertiesLocation);
                    locations.add(propertiesLocation);
                }
            }//@on
        }

        // arguments

        getProperties().putAll(ApplicationUtils.parseCommandArguments(arguments));

        refreshActiveProfiles();
        replaceProperties(locations);
    }

    protected Resource getPropertiesResource(final Set<String> locations) {
        Resource propertiesResource = ResourceUtils.getResource(Constant.DEFAULT_PROPERTIES_FILE);

        if (propertiesResource.exists()) {
            setPropertiesLocation(Constant.DEFAULT_PROPERTIES_FILE);
            locations.add(Constant.DEFAULT_PROPERTIES_FILE);
        }
        else {
            propertiesResource = ResourceUtils.getResource(Constant.DEFAULT_YAML_FILE);
            setPropertiesLocation(Constant.DEFAULT_YAML_FILE);
            locations.add(Constant.DEFAULT_YAML_FILE);
        }
        return propertiesResource;
    }

    /**
     * Is yaml?
     * 
     * @param propertiesLocation
     *            location
     */
    protected boolean isYamlProperties(String propertiesLocation) {
        return propertiesLocation.contains(".yaml") || propertiesLocation.contains(".yml");
    }

    /**
     * Replace the properties from current active profiles
     * 
     * @param locations
     *            loaded properties locations
     * @throws IOException
     */
    protected void replaceProperties(Set<String> locations) throws IOException {

        // replace
        final String[] activeProfiles = getActiveProfiles();
        for (final String profile : activeProfiles) {

            for (final String location : locations) {
                final StringBuilder builder = new StringBuilder(location);
                builder.insert(builder.indexOf("."), '-' + profile);

                try {
                    super.loadProperties(builder.toString());
                }
                catch (FileNotFoundException e) {}
            }
        }
    }

    @Override
    protected void loadProperties(Resource resource) throws IOException {

        if (isYamlProperties(resource.getName())) {
            loadFromYmal(getProperties(), resource);
        }
        else {
            super.loadProperties(resource);
        }
    }

    protected void loadFromYmal(final Properties properties, final Resource yamlResource) throws IOException {
        log.info("Found Yaml Properties Resource: [{}]", yamlResource.getLocation());
        doMapping(properties, new Yaml(new CompactConstructor()).load(yamlResource.getInputStream()), null);
    }

    @SuppressWarnings("unchecked")
    protected static void doMapping(final Properties properties, final Map<String, Object> base, final String prefix) {

        for (final Entry<String, Object> entry : base.entrySet()) {
            String key = entry.getKey();
            final Object value = entry.getValue();
            key = prefix == null ? key : (prefix + '.' + key);
            if (value instanceof Map) {
                doMapping(properties, (Map<String, Object>) value, key);
            }
            else {
                properties.put(key, value);
            }
        }
    }
}

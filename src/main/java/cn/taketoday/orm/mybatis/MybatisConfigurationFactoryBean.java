/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.session.Configuration;

import java.io.InputStream;
import java.util.Properties;

import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.lang.Env;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.util.ResourceUtils;

/**
 * @author TODAY <br>
 * 2018-10-09 20:32
 */
public class MybatisConfigurationFactoryBean implements FactoryBean<Configuration>, InitializingBean {

  @Env("mybatis.config")
  private String configLocation;

  @Props(prefix = "mybatis.", replace = true)
  private Properties properties;

  private Configuration configuration;

  @Override
  public void afterPropertiesSet() throws Exception {
    if (configuration == null) {
      final InputStream resourceAsStream = ResourceUtils.getResourceAsStream(getConfigLocation());
      final Configuration configuration
              = new XMLConfigBuilder(resourceAsStream, "TODAY-MYBATIS", getProperties()).parse();
      setConfiguration(configuration);
    }
  }

  @Override
  public Configuration getBean() {
    return getConfiguration();
  }

  @Override
  public Class<Configuration> getBeanClass() {
    return Configuration.class;
  }

  public String getConfigLocation() {
    if (configLocation == null) {
      throw new ConfigurationException("mybatis config file must not be null");
    }
    return configLocation;
  }

  public Properties getProperties() {
    return properties;
  }

  public Configuration getConfiguration() {
    if (configuration == null) {
      throw new ConfigurationException("org.apache.ibatis.session.Configuration must not be null");
    }
    return configuration;
  }

  public MybatisConfigurationFactoryBean setProperties(Properties properties) {
    this.properties = properties;
    return this;
  }

  public MybatisConfigurationFactoryBean setConfigLocation(String configLocation) {
    this.configLocation = configLocation;
    return this;
  }

  public MybatisConfigurationFactoryBean setConfiguration(Configuration configuration) {
    this.configuration = configuration;
    return this;
  }

}

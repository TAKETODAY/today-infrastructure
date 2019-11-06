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
package cn.taketoday.orm.mybatis;

import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.session.Configuration;

import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.utils.ContextUtils;

/**
 * @author TODAY <br>
 *         2018-10-09 20:32
 */
@Singleton
public class MybatisConfigurationFactoryBean implements FactoryBean<Configuration>, InitializingBean {

    @Env("mybatis.config")
    private String configLocation;

    @Props(prefix = "mybatis.", replace = true)
    private Properties properties;

    private Configuration configuration;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getConfiguration() == null) {
            setConfiguration(new XMLConfigBuilder(ContextUtils.getResourceAsStream(configLocation), "TODAY-MYBATIS", properties)//
                    .parse());
        }
    }

    @Override
    public Configuration getBean() {
        return getConfiguration();
    }

    @Override
    public String getBeanName() {
        return "mybatisConfiguration";
    }

    @Override
    public Class<Configuration> getBeanClass() {
        return Configuration.class;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public MybatisConfigurationFactoryBean setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

}

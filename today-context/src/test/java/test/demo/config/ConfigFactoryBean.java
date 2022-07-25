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
package test.demo.config;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.env.Environment;
import cn.taketoday.stereotype.Prototype;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

/**
 * @author TODAY <br>
 * 2018-08-08 15:06
 */
@Getter
@Prototype("FactoryBean-Config")
public class ConfigFactoryBean implements FactoryBean<Config>, InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(ConfigFactoryBean.class);

  @PostConstruct
  @Order(Ordered.LOWEST_PRECEDENCE)
  public void init1() {
    log.info("ConfigFactoryBean.init1()");
  }

  @PostConstruct
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public void init2() {
    log.info("ConfigFactoryBean.init2()");
  }

  @Autowired
  private Environment pro;

  @Override
  public Config getObject() {
    Config bean = new Config();

    bean.setCdn(pro.getProperty("site.cdn"));
    bean.setHost(pro.getProperty("site.host"));
    bean.setCopyright(pro.getProperty("site.copyright"));
    return bean;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

  }

  @Override
  public Class<Config> getObjectType() {
    return Config.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}

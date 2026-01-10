/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.demo.config;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.annotation.Autowired;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.env.Environment;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Prototype;
import jakarta.annotation.PostConstruct;

/**
 * @author TODAY <br>
 * 2018-08-08 15:06
 */
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

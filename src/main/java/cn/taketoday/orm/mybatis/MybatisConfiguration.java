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
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.loader.BeanDefinitionLoadingStrategy;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Repository;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2018-10-05 19:03
 */
@cn.taketoday.context.annotation.Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MybatisConfiguration implements BeanDefinitionLoadingStrategy {
  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String DEFAULT_CONFIG_LOCATION = "classpath:mybatis.xml";

  @Override
  public void loadBeanDefinitions(
          MetadataReader metadata, DefinitionLoadingContext loadingContext) {
    log.info("Loading Mybatis Mapper Bean Definitions");

    if (metadata.getClassMetadata().isInterface()) {
      // must be an interface
      MergedAnnotations annotations = metadata.getAnnotationMetadata().getAnnotations();
      MergedAnnotation<Repository> repository = annotations.get(Repository.class);
      if (repository.isPresent()) {
        String className = metadata.getAnnotationMetadata().getClassName();
        log.debug("Found Mapper: [{}]", className);
        String[] names = repository.getStringValueArray();
        BeanDefinition beanDefinition = createBeanDefinition(className);
        String name = ObjectUtils.isNotEmpty(names)
                      ? names[0] : loadingContext.generateBeanName(beanDefinition);

        loadingContext.registerBeanDefinition(name, beanDefinition);
      }
    }

  }

  protected BeanDefinition createBeanDefinition(String className) {
    BeanDefinition ret = new BeanDefinition();
    ret.setBeanClassName(className);
    ret.setSynthetic(true);
    ret.setInitMethods("applySqlSession");
    ret.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    ret.setInstanceSupplier(() -> new MapperFactoryBean<>(className));
    return ret;
  }

  @Component
  public SqlSession sqlSession(
          @Autowired DataSource dataSource,
          @Env("mybatis.env") String envId,
          @Env("mybatis.config") String configLocation,
          @Autowired(required = false) TransactionFactory transactionFactory,
          @Props(prefix = "mybatis.", replace = true) Properties properties) throws IOException //
  {

    if (StringUtils.isEmpty(envId)) {
      envId = "TODAY-MYBATIS";
    }

    if (StringUtils.isEmpty(configLocation)) {
      configLocation = DEFAULT_CONFIG_LOCATION;
    }

    Configuration configuration = new XMLConfigBuilder(
            ResourceUtils.getResourceAsStream(configLocation), envId, properties).parse();

    if (transactionFactory == null) {
      transactionFactory = new MybatisTransactionFactory();
    }
    configuration.setEnvironment(new Environment(envId, transactionFactory, dataSource));
    return new SessionTemplate(new DefaultSqlSessionFactory(configuration));
  }

}

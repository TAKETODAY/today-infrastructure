/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import java.lang.reflect.Method;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Repository;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.factory.DefaultBeanDefinition;
import cn.taketoday.context.factory.FactoryBeanDefinition;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.SingletonSupplier;
import cn.taketoday.context.utils.StringUtils;

import static cn.taketoday.context.utils.ContextUtils.getResourceAsStream;

/**
 * @author TODAY <br>
 * 2018-10-05 19:03
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MybatisConfiguration implements ApplicationListener<LoadingMissingBeanEvent> {

  public static final String DEFAULT_CONFIG_LOCATION = "classpath:mybatis.xml";
  public static final Method[] initMethods = ContextUtils.resolveInitMethod(null, MapperFactoryBean.class);

  @Override
  public void onApplicationEvent(LoadingMissingBeanEvent event) {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.info("Loading Mybatis Mapper Bean Definitions");

    final ApplicationContext context = event.getApplicationContext();
    final BeanNameCreator beanNameCreator = context.getEnvironment().getBeanNameCreator();

    for (final Class<?> beanClass : event.getCandidates()) {
      if (beanClass.isInterface()) {
        Repository repository = beanClass.getAnnotation(Repository.class);
        if (repository == null) {
          continue;
        }

        log.debug("Found Mapper: [{}]", beanClass.getName());

        final String[] names = repository.value();
        final String name = ObjectUtils.isNotEmpty(names) ? names[0] : beanNameCreator.create(beanClass);
        // context.registerSingleton(BeanFactory.FACTORY_BEAN_PREFIX.concat(name), new MapperFactoryBean<>(beanClass));
        context.registerBeanDefinition(name, createBeanDefinition(beanClass, name));
      }
    }
  }

  protected FactoryBeanDefinition<?> createBeanDefinition(final Class<?> beanClass, final String name) {
    final DefaultBeanDefinition ret = new DefaultBeanDefinition(name, beanClass);
    ret.setDestroyMethods(Constant.EMPTY_STRING_ARRAY)
            .setInitMethods(initMethods);
    return new FactoryBeanDefinition<>(ret, SingletonSupplier.of(new MapperFactoryBean<>(beanClass)));
  }

  @MissingBean
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

    final Configuration configuration = new XMLConfigBuilder(
            getResourceAsStream(configLocation), envId, properties).parse();

    if (transactionFactory == null) {
      transactionFactory = new MybatisTransactionFactory();
    }
    configuration.setEnvironment(new Environment(envId, transactionFactory, dataSource));
    return new SessionTemplate(new DefaultSqlSessionFactory(configuration));
  }

}

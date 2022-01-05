/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.orm.mybatis.sample.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.mapper.MapperFactoryBean;
import cn.taketoday.orm.mybatis.sample.mapper.UserMapper;
import cn.taketoday.orm.mybatis.sample.service.FooService;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;
import cn.taketoday.transaction.PlatformTransactionManager;

@Configuration
public class SampleConfig {
  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
        .addScript("org/mybatis/spring/sample/db/database-schema.sql")
        .addScript("org/mybatis/spring/sample/db/database-test-data.sql").build();
  }

  @Bean
  public PlatformTransactionManager transactionalManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    SqlSessionFactoryBean ss = new SqlSessionFactoryBean();
    ss.setDataSource(dataSource());
    ss.setMapperLocations(new ClassPathResource("org/mybatis/spring/sample/mapper/UserMapper.xml"));
    return ss.getObject();
  }

  @Bean
  public UserMapper userMapper() throws Exception {
    // when using javaconfig a template requires less lines than a MapperFactoryBean
    SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory());
    return sqlSessionTemplate.getMapper(UserMapper.class);
  }

  @Bean
  public UserMapper userMapperWithFactory() throws Exception {
    MapperFactoryBean<UserMapper> mapperFactoryBean = new MapperFactoryBean<>();
    mapperFactoryBean.setMapperInterface(UserMapper.class);
    mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory());
    mapperFactoryBean.afterPropertiesSet();
    return mapperFactoryBean.getObject();
  }

  @Bean
  public FooService fooService() throws Exception {
    return new FooService(userMapper());
  }

  @Bean
  public FooService fooServiceWithMapperFactoryBean() throws Exception {
    return new FooService(userMapperWithFactory());
  }

}

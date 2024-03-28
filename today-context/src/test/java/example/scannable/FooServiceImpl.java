/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package example.scannable;

import java.util.Comparator;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Lookup;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.annotation.DependsOn;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.stereotype.Service;
import cn.taketoday.util.concurrent.Future;
import jakarta.annotation.PostConstruct;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Service
@Lazy
@DependsOn("myNamedComponent")
public abstract class FooServiceImpl implements FooService {

  // Just to test ASM5's bytecode parsing of INVOKESPECIAL/STATIC on interfaces
  @SuppressWarnings("unused")
  private static final Comparator<MessageBean> COMPARATOR_BY_MESSAGE = Comparator.comparing(MessageBean::getMessage);

  @Autowired
  private FooDao fooDao;

  @Autowired
  public BeanFactory beanFactory;

  @Autowired
  public List<BeanFactory> listableBeanFactory;

  @Autowired
  public ResourceLoader resourceLoader;

  @Autowired
  public PatternResourceLoader resourcePatternResolver;

  @Autowired
  public ApplicationEventPublisher eventPublisher;

  @Autowired
  public MessageSource messageSource;

  @Autowired
  public ApplicationContext context;

  @Autowired
  public ConfigurableApplicationContext[] configurableContext;

  @Autowired
  public AbstractApplicationContext genericContext;

  private boolean initCalled = false;

  @PostConstruct
  private void init() {
    if (this.initCalled) {
      throw new IllegalStateException("Init already called");
    }
    this.initCalled = true;
  }

  @Override
  public String foo(int id) {
    return this.fooDao.findFoo(id);
  }

  public String lookupFoo(int id) {
    return fooDao().findFoo(id);
  }

  @Override
  public Future<String> asyncFoo(int id) {
    System.out.println(Thread.currentThread().getName());
    Assert.state(ServiceInvocationCounter.getThreadLocalCount() != null, "Thread-local counter not exposed");
    return Future.ok(fooDao().findFoo(id));
  }

  @Override
  public boolean isInitCalled() {
    return this.initCalled;
  }

  @Lookup
  protected abstract FooDao fooDao();

}

package infra.context.annotation;

import java.util.LinkedHashSet;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.context.BootstrapContext;
import infra.core.Ordered;
import infra.core.type.AnnotationMetadata;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Registers the {@link RequiredByPostProcessor} to handle initialization dependency
 * relationships declared via the {@link RequiredBy} annotation.
 * <p>
 * This configurer ensures that beans marked with {@code @RequiredBy} correctly
 * establish their dependency order on other beans specified by name or type.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/24 23:24
 */
public class RequiredByDependencyConfigurer implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    String name = RequiredByPostProcessor.class.getName();
    if (!context.containsBeanDefinition(name)) {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RequiredByPostProcessor.class)
              .setEnableDependencyInjection(false)
              .setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      context.registerBeanDefinition(name, builder.getBeanDefinition());
    }
  }

  /**
   * {@link BeanFactoryPostProcessor} used to configure initialization
   * dependency relationships.
   */
  static class RequiredByPostProcessor implements BeanFactoryPostProcessor, Ordered {

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
      for (String beanName : beanFactory.getBeanDefinitionNames()) {
        var annotation = beanFactory.findAnnotationOnBean(beanName, RequiredBy.class, false);
        if (annotation.isPresent()) {
          String[] names = annotation.getStringValueArray();
          addDependsOn(beanFactory, names, beanName);
          Class<?>[] types = annotation.getClassArray("types");
          for (Class<?> type : types) {
            names = beanFactory.getBeanNamesForType(type, true, false);
            addDependsOn(beanFactory, names, beanName);
          }
        }
      }
    }

    private void addDependsOn(ConfigurableBeanFactory beanFactory, String[] names, String dependsOn) {
      for (String name : names) {
        BeanDefinition beanDefinition = getBeanDefinition(name, beanFactory);
        String[] depends = beanDefinition.getDependsOn();
        if (depends != null) {
          LinkedHashSet<String> result = new LinkedHashSet<>();
          CollectionUtils.addAll(result, depends);
          result.add(dependsOn);
          beanDefinition.setDependsOn(StringUtils.toStringArray(result));
        }
        else {
          beanDefinition.setDependsOn(dependsOn);
        }
      }
    }

    private static BeanDefinition getBeanDefinition(String beanName, ConfigurableBeanFactory beanFactory) {
      try {
        return beanFactory.getBeanDefinition(beanName);
      }
      catch (NoSuchBeanDefinitionException ex) {
        BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
        if (parentBeanFactory instanceof ConfigurableBeanFactory cbf) {
          return getBeanDefinition(beanName, cbf);
        }
        throw ex;
      }
    }

  }

}

package cn.taketoday.beans.factory;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/24 17:37
 */
public class BeanDefinitionCustomizers {

  @Nullable
  protected List<BeanDefinitionCustomizer> customizers;

  @Nullable
  private StrategiesDetector strategiesDetector;

  @Nullable
  public List<BeanDefinitionCustomizer> getCustomizers() {
    return customizers;
  }

  public void addCustomizers(@Nullable BeanDefinitionCustomizer... customizers) {
    if (ObjectUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
  }

  public void addCustomizers(@Nullable List<BeanDefinitionCustomizer> customizers) {
    if (CollectionUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
  }

  /**
   * clear exist customizers and set
   *
   * @param customizers new customizers
   */
  public void setCustomizers(@Nullable BeanDefinitionCustomizer... customizers) {
    if (ObjectUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
    else {
      // clear
      if (this.customizers != null) {
        this.customizers.clear();
      }
    }
  }

  /**
   * set customizers
   *
   * @param customizers new customizers
   */
  public void setCustomizers(@Nullable List<BeanDefinitionCustomizer> customizers) {
    this.customizers = customizers;
  }

  @NonNull
  private List<BeanDefinitionCustomizer> customizers() {
    if (customizers == null) {
      customizers = new ArrayList<>();
    }
    return customizers;
  }

  public void setStrategiesDetector(@Nullable StrategiesDetector strategiesDetector) {
    this.strategiesDetector = strategiesDetector;
  }

  @Nullable
  public StrategiesDetector getStrategiesDetector() {
    return strategiesDetector;
  }

  public void loadDefaultCustomizers() {
    loadDefaultCustomizers(null);
  }

  public void loadDefaultCustomizers(@Nullable BeanFactory beanFactory) {
    StrategiesDetector strategiesDetector = getStrategiesDetector();
    if (strategiesDetector == null) {
      strategiesDetector = TodayStrategies.getDetector();
    }
    addCustomizers(strategiesDetector.getStrategies(BeanDefinitionCustomizer.class, beanFactory));
  }

}

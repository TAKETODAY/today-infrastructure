package example.gh24375;

import cn.taketoday.lang.Component;

@Component
@EnclosingAnnotation(nested2 = @NestedAnnotation)
public class AnnotatedComponent {
}

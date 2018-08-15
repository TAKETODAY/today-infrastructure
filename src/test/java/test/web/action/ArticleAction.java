package test.web.action;

import cn.taketoday.context.annotation.RestProcessor;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.Session;
import test.interceptor.AdminInterceptor;
import test.web.domain.User;


@RestProcessor
public class ArticleAction extends BaseAction{

	private static final long serialVersionUID = 3914176913983375014L;

	
	@ActionMapping(value = {"/article"})
	@Interceptor(AdminInterceptor.class)
	public String index(@Session("USER_INFO") User userInfo) {
		
		
		return userInfo.getUserId();
	}
	
	
	
	
	
	
	
}

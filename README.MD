# Today Web Framework

Today Web Framework 高性能 轻量级web框架

	1.1.0 已经应用到了Today Blog https://www.yanghaijian.top
	1.2.0 正在开发中


###	通过@ActionMapping配置请求
	@ActionMapping(value = {"/index.action", "/index.jsp"}, method = RequestMethod.GET)

### 通过@ActionProcessor配置处理器Bean	并配置资源路径 以及后缀 后缀默认为	.jsp
	@ActionProcessor(prefix = "/WEB-INF/view/index")


##	登录实例
```java
	@ActionMapping(value = "/login" , method = RequestMethod.GET)
	public String login() {
		return "/login/login";
	}
	
	
	@ActionMapping(value = "/login.do" , method = RequestMethod.POST)
	public String login(@RequestParam(required = true) String userId,
                            @RequestParam(required = true) String passwd, HttpServletRequest request)  {    
                   
		request.setAttribute("userId", userId);
		if(userId.equals(passwd)) {
			request.setAttribute("msg", "登录成功");
			return "/success";
		}
		request.setAttribute("msg", "登录失败");
		return "/error";
	}
```



###	也可以通过xml文件配置视图，不需要处理器Bean，也可以配置简单的处理器Bean，但是不能完成参数注入

```xml	
	<!DOCTYPE Web-Views-configuration PUBLIC
			"-//Today Technology//Web-View Configuration DTD 1.0//CN"
					"https://www.yanghaijian.top/dtd/web/views-configuration-1.0.dtd">
	
	<Web-Views-configuration>
	
		<!-- 状态码 -->
		<common baseDir="/WEB-INF/error" class="com.yhj.core.action.IndexAction">
			<view jsp="/400.jsp" name="BadRequest" 		 type="dispatcher"/>
			<view jsp="/403.jsp" name="Forbidden" 		 type="dispatcher"/>
			<view jsp="/404.jsp" name="NotFound" 		 type="dispatcher"/>
			<view jsp="/500.jsp" name="ServerIsBusy" 	 type="dispatcher"/>
			<view jsp="/405.jsp" name="MethodNotAllowed" type="dispatcher"/>
		</common>
		
		<!-- 首页 -->
		<common class="com.yhj.core.action.IndexAction" baseDir="/WEB-INF/view">
			<action name="index.html" type="dispatcher" method="index" jsp="/index.jsp"></action>
		</common>
		
	</Web-Views-configuration>

```
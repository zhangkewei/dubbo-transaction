# dubbo-transaction
基于dubbo2.5.3的两阶段提交分布式数据库事务;

## dubbo provider
	<bean id="transactionManager" class="me.zkevin.transaction.jdbc.datasource.DubboTransactionDataSourceTransactonManager" init-method="transactionStart">
    	<property name="dataSource" ref="dataSource" />
	</bean>



## dubbo consumer
### spring mvc
	<bean id="transactionHook" class="me.zkevin.transaction.support.DefaultSessionHook"/>
	<bean class="com.chit.sso.server.interceptor.ExceptionResolver"></bean>
	<bean class="org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping">
    	<property name="interceptors">
       	 	<list>
            	<bean class="me.zkevin.transaction.support.interceptor.spring.DefaultMethodHandler">
                <property name="hook" ref="transactionHook"/>
            	</bean>
        	</list>
    	</property>
	</bean>


### servlet web.xml
	<listener>
		<listener-class>me.zkevin.transaction.support.interceptor.servlet.DefaultServletListener</listener-class>
	</listener>

## 配置文件配置
### 完整配置文件
	resources/dubbo-transaction.properties.all
	resources/dubbo-transaction.properties.consumer
	resources/dubbo-transaction.properties.provider

### -D || dubbo-transaction.properties

	transaction.contextCount
	transaction.contextClass
	transaction.ip
	transaction.port
	transaction.executeWorkerSize
	transaction.bootstrapSize
	transaction.channelSize
	transaction.callTimeoutPerLoop
	transaction.callTimeoutLoops
	transaction.isProxy


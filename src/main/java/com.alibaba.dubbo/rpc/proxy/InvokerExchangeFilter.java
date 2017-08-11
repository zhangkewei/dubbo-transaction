package com.alibaba.dubbo.rpc.proxy;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.dubbo.rpc.proxy.invoker.InvokerChainContextFactory;
import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import org.apache.log4j.Logger;

/**
 * 扩展dubbo，支持自定义rpc进程间扩展参数传递
 * 类名称：ApplicationParamsExchange  
 * 类描述：  
 * 创建人：张科伟  
 * 创建时间：2016年11月22日 下午1:45:23
 * @version
 */
public class InvokerExchangeFilter{
	private static final Logger logger= Logger.getLogger(InvokerExchangeFilter.class);
	private static final ThreadLocal<ExchangeObject> _EO_THIS=new ThreadLocal<ExchangeObject>();
	private static final ThreadLocal<ExchangeObject> _EO_INVOKER=new ThreadLocal<ExchangeObject>();
	public static final String CLIENT_KEY="CHIT_CLIENT_KEY";
	public static final String SERVER_KEY="CHIT_SERVER_KEY";

	private InvokerExchangeFilter(){}
	public static boolean isInvoker(){
		return null!=getThis()&&getThis().isInvoker();
	}
	public static ExchangeObject getThis(){
		ExchangeObject eo=_EO_THIS.get();
		if(null==eo){
			synchronized (Thread.currentThread()){
				eo=_EO_THIS.get();
				if(eo==null){
					eo=ExchangeObject.createExchangeObject(_EO_INVOKER.get());
					_EO_THIS.set(eo);
				}
			}
		}
		return eo;
	}
	public static ExchangeObject getInvoker(){
		return _EO_INVOKER.get();
	}
	public static Map<String,Object> GLOBAL_EXCHANGE_PARAMS=new HashMap<String,Object>();
	private static final List<AbstractConsumedListener> consumedListeners=new CopyOnWriteArrayList<AbstractConsumedListener>();
	private static final List<AbstractProvidedListener> providedListeners=new CopyOnWriteArrayList<AbstractProvidedListener>();
	private static final List<AbstractProvidedBeforeListener> providedBeforeListeners=new CopyOnWriteArrayList<AbstractProvidedBeforeListener>();
	/**
	 * provider before
	 * @Title: ApplicationParamsExchange.beforeInvoke
	 * @param invocation
	 * @return void
	 */
	protected static void beforeInvoke(Invocation invocation) {
		String invokerExchange=invocation.getAttachment(CLIENT_KEY, "");
		logger.info("["+invocation.getMethodName()+"]provider invoke=>"+CLIENT_KEY+":"+ invokerExchange);
		ExchangeObject invokerInfo=null;
		ExchangeObject nowInvoker=_EO_INVOKER.get();
		if(!StringUtils.isBlank(invokerExchange)){
			try{
				invokerInfo=JSON.parse(invokerExchange,ExchangeObject.class);
			}catch(Exception e){}
		}
		if(null==nowInvoker){
			_EO_INVOKER.set(invokerInfo);
			nowInvoker=invokerInfo;
		}
		if(null==_EO_THIS.get())_EO_THIS.set(ExchangeObject.createExchangeObject(nowInvoker));
		//被调用前监听
		for(AbstractProvidedBeforeListener l:providedBeforeListeners){
			l.listen(invokerInfo,_EO_THIS.get());
		}
	}
	/**
	 * provider after
	 * @Title: ApplicationParamsExchange.afterInvoke
	 * @param invocation
	 * @param result
	 * @return void
	 */
	protected static void afterInvoke(Invocation invocation, RpcResult result) {
		try {
			String providerInfo = JSON.json(_EO_THIS.get());
			if(null!=result){
				result.getAttachments().put(SERVER_KEY, providerInfo);
			}
			logger.info("["+invocation.getMethodName()+"]provider invoked=>" + SERVER_KEY + ":" + providerInfo);
			//被调用后监听
			for(AbstractProvidedListener l:providedListeners){
				l.listen(_EO_THIS.get());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * consumer before
	 * @Title: ApplicationParamsExchange.beforeInvoke
	 * @param invoker
	 * @param invocation 
	 * @return void
	 */
	public static void beforeInvoke(Invoker<?> invoker, RpcInvocation invocation) {
		ExchangeObject eoThis=getThis();
		try {
			String consumerInfo=JSON.json(eoThis);
			invocation.getAttachments().put(CLIENT_KEY, consumerInfo);
			int i = eoThis.consumeCount.incrementAndGet();
			if (i == 1 && eoThis.isInvoker()&&InvokerExchangeVersion.isSupportTransfer(eoThis.getExtend())) {
				InvokerChainContextFactory.getInstance().add(_EO_THIS.get());
			}
			logger.info("["+invocation.getMethodName()+"]consumer invoke=>"+CLIENT_KEY+":"+consumerInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * consumer after
	 * @Title: ApplicationParamsExchange.afterInvoke
	 * @param invoker
	 * @param result
	 * @return void
	 */
	protected static void afterInvoke(Invoker<?> invoker, Result result, RpcInvocation invocation) {
		String providerExchange=null!=result?result.getAttachment(SERVER_KEY, ""):"";
		logger.info("["+invocation.getMethodName()+"]consumer invoked=>" + SERVER_KEY + ":" + providerExchange);
		try{
			ExchangeObject providerObject=!StringUtils.isBlank(providerExchange)?JSON.parse(providerExchange,ExchangeObject.class):null;
			//不支持调链协议-----------------------start
			if(!InvokerExchangeVersion.isSupportTransfer(null==providerObject?null:providerObject.getExtend())){
				return;
			}
			//不支持调链协议-----------------------end

			if(null!=providerObject&&!providerObject.isInvoker()){
				//持久化到应用容器
				InvokerChainContextFactory.getInstance().add(providerObject);
			}
			//方法调用抛出异常
			if(null!=result.getException()){
				//方法调用者
				if(null!=providerObject&&providerObject.isInvoker()){
					_EO_THIS.get().setException(result.getException());
				}
				//被调用方法
				if(null!=providerObject&&!providerObject.isInvoker()){
					providerObject.setException(result.getException());
				}
				if(null==providerObject){
					ExchangeObject eoNew=ExchangeObject.createExchangeObject(_EO_THIS.get());
					eoNew.setException(result.getException());
					InvokerChainContextFactory.getInstance().add(eoNew);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			ExchangeObject eoNew=ExchangeObject.createExchangeObject(_EO_THIS.get());
			eoNew.setException(e);
			InvokerChainContextFactory.getInstance().add(eoNew);
		}
	}

	public static class ExchangeObject{
		public static final int INVOKER=1;
		public static final int PROVIDER=2;
		private int role;
		private String exchangeId;
		private String ip;
		private Map<String,Object> extend;
		private String invokerId;
		private boolean invoker;
		private String rootId;
		private AtomicInteger consumeCount;
		private Throwable exception;
		public ExchangeObject(){
			extend=new HashMap<String, Object>();
			extend.putAll(GLOBAL_EXCHANGE_PARAMS);
			ip=RpcContext.getContext().getLocalHost();
			consumeCount=new AtomicInteger(0);
			extend.put(InvokerExchangeVersion.VERSION_KEY,InvokerExchangeVersion.DEFAULT_VERSION);
		};
		protected static ExchangeObject createExchangeObject(ExchangeObject invokerInfo) {
			ExchangeObject eObject=new ExchangeObject();
			if(null==invokerInfo){
				eObject.role=INVOKER;
				eObject.exchangeId=UUID.randomUUID().toString();
				eObject.invokerId=eObject.exchangeId;
			}else{
				eObject.role=PROVIDER;
				eObject.exchangeId=invokerInfo.exchangeId+"_"+UUID.randomUUID().toString();
				eObject.extend.put(InvokerExchangeVersion.VERSION_KEY,invokerInfo.getExtend().get(InvokerExchangeVersion.VERSION_KEY));
			}
			return eObject;
		}
		public int getRole() {
			return role;
		}
		public String getExchangeId() {
			return exchangeId;
		}
		public String getIp() {
			return ip;
		}
		public void addExtend(String key,String value) {
			extend.put(key,value);
		}
		public String getInvokerId(){
			return getInvokerIdByExchangeId(exchangeId);
		}
		public static String getInvokerIdByExchangeId(String exchangeId){
			int last_=exchangeId.lastIndexOf("_");
			if(last_>-1){
				return exchangeId.substring(0,last_);
			}else{
				return exchangeId;
			}
		}
		public static String getRootIdByExchangeId(String exchangeId){
			int last_=exchangeId.indexOf("_");
			if(last_>-1){
				return exchangeId.substring(0,last_);
			}else{
				return exchangeId;
			}
		}
		public static ExchangeObject error(Throwable throwable){
			ExchangeObject object=new ExchangeObject();
			object.extend.clear();
			object.ip="";
			object.exception=throwable;
			return object;
		}
		public String getRootId(){
			return getRootIdByExchangeId(exchangeId);
		}
		public boolean isInvoker() {
			return role==ExchangeObject.INVOKER;
		}

		public void setExchangeId(String exchangeId) {
			this.exchangeId = exchangeId;
		}


		public void setIp(String ip) {
			this.ip = ip;
		}

		public void setRole(int role) {
			this.role = role;
		}

		public void setInvoker(boolean invoker) {
			this.invoker = invoker;
		}

		public void setInvokerId(String invokerId) {
			this.invokerId = invokerId;
		}

		public void setRootId(String rootId) {
			this.rootId = rootId;
		}

		public Map<String, Object> getExtend() {
			return extend;
		}

		public void setExtend(Map<String, Object> extend) {
			this.extend = extend;
		}

		public void setException(Throwable exception) {
			this.exception = exception;
		}

		public Throwable getException() {
			return exception;
		}
	}
	public static void clearSession(){
		InvokerExchangeFilter._EO_THIS.set(null);
		InvokerExchangeFilter._EO_INVOKER.set(null);
	}

	public static  void addProvidedListener(AbstractProvidedListener listener){
		providedListeners.add(listener);
	}
	public static void addProvidedBeforeListener(AbstractProvidedBeforeListener apl) {
		providedBeforeListeners.add(apl);
	}
}

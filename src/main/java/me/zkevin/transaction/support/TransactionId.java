package me.zkevin.transaction.support;

import com.alibaba.dubbo.rpc.proxy.AbstractProvidedBeforeListener;
import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在多个分布式服务进程间传递的事务信息
 * Created by zkevin on 16/11/8.
 */
public class TransactionId {
    static {
        /**
         * 将事务代理状态传递给被调用者
         */
        DubboSupport.addProvidedBeforeListener(new AbstractProvidedBeforeListener() {
            @Override
            public void listen(InvokerExchangeFilter.ExchangeObject invoker, InvokerExchangeFilter.ExchangeObject invoked) {
                if(invoker.getExtend().containsKey(TransactionId.PROXY_KEY)){
                    invoked.addExtend(TransactionId.PROXY_KEY,DubboSupport.isSupportTransaction(invoker.getExtend())?invoker.getExtend().get(TransactionId.PROXY_KEY)+"":false+"");
                }
            }
        });
    }
    private static final Logger logger=Logger.getLogger(TransactionId.class);
    private static ThreadLocal<TransactionId> CURRENT_TID = new ThreadLocal<TransactionId>();
    private String rootId;
    private String currentId;
    private boolean isInitiator;//是否事务发起者
    //是否开启事务代理,默认开启
    private AtomicBoolean isProxy=new AtomicBoolean(TransactionConfig.getInstance().isTransactionProxy());
    private volatile boolean isSupportTransaction=true;
    public static final String PROXY_KEY="is_proxy";
    private TransactionId() {

    }
    private static TransactionId createDefault(){
        InvokerExchangeFilter.ExchangeObject eo=DubboSupport.getCurrentInvoke();
        TransactionId id =new TransactionId();
        id.setIsInitiator(eo.isInvoker());
        id.currentId=eo.getExchangeId();
        id.rootId=eo.getRootId();
        Map<String, Object> extendMap=eo.getExtend();
        id.isSupportTransaction=DubboSupport.isSupportTransaction(eo.getExtend());
        if(id.isSupportTransaction){
            /**
             * 是否启用事务代理配置
             */
            if (null != extendMap && extendMap.containsKey(PROXY_KEY)) {
                String isProxy = extendMap.get(PROXY_KEY) + "";
                if (null != isProxy && isProxy.trim().equalsIgnoreCase("false")) {
                    id.disableProxy();
                } else {
                    id.enableProxy();
                }
            }
        }else{//不支持事务代理
            id.isProxy=new AtomicBoolean(false);
            DubboSupport.getCurrentInvoke().addExtend(PROXY_KEY,false+"");
        }
        return id;
    }
    public static String getTransactionKey(TransactionId id){
        id=null==id?get():id;
        return id.getTransactionKey();
    }
    public String getTransactionKey(){
        return currentId;
    }
    public static TransactionId get() {
        TransactionId id=CURRENT_TID.get();
        if(null==id){
            synchronized (Thread.currentThread()){
                id=CURRENT_TID.get();
                if(null==id){
                    id=createDefault();
                    CURRENT_TID.set(id);
                }
            }
        }
        return id;
    }


    public boolean isInitiator() {
        return isInitiator;
    }

    public void setIsInitiator(boolean isInitiator) {
        this.isInitiator = isInitiator;
    }

    public String getCurrentId() {
        return currentId;
    }
    public void setCurrentId(String currentId) {
        this.currentId = currentId;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }
    public static void clear() {
        CURRENT_TID.set(null);
    }
    protected boolean enableProxy(){
        if(!isSupportTransaction)return isSupportTransaction;
        logger.debug("["+currentId+"]enable proxy:start");
        boolean isSet=isProxy.compareAndSet(false,true);
        logger.debug("["+currentId+"]enable proxy status:" + isSet);
        if(isSet){
            DubboSupport.getCurrentInvoke().addExtend(PROXY_KEY,true+"");
        }
        return  isSet;
    }
    protected boolean disableProxy(){
        if(!isSupportTransaction)return isSupportTransaction;
        logger.debug("["+currentId+"]disable proxy:start");
        boolean isSet=isProxy.compareAndSet(true,false);
        logger.debug("["+currentId+"]disable proxy status:" + isSet);
        if (isSet){
            DubboSupport.getCurrentInvoke().addExtend(PROXY_KEY,false+"");
        }
        return isSet;
    }
    protected boolean revertProxy(){
        if(!isSupportTransaction)return isSupportTransaction;
        logger.debug("["+currentId+"]revert proxy:start");
        boolean isSet=isProxy.compareAndSet(isProxy.get(),TransactionConfig.getInstance().isTransactionProxy());
        logger.debug("["+currentId+"]revert proxy status:"+isSet);
        if(isSet){
            DubboSupport.getCurrentInvoke().addExtend(PROXY_KEY,TransactionConfig.getInstance().isTransactionProxy()+"");
        }
        return  isSet;
    }
    protected boolean isProxy() {
        return isSupportTransaction&&isProxy.get();
    }
}


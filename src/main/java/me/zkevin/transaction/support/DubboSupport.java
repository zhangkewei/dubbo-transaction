package me.zkevin.transaction.support;

import com.alibaba.dubbo.rpc.proxy.AbstractProvidedBeforeListener;
import com.alibaba.dubbo.rpc.proxy.AbstractProvidedListener;
import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;
import com.alibaba.dubbo.rpc.proxy.InvokerExchangeVersion;
import com.alibaba.dubbo.rpc.proxy.invoker.InvokerChainContextFactory;

import java.util.List;
import java.util.Map;

/**
 * dubbo工具包中的门面方法
 * 从该类中引用
 */
public class DubboSupport {
    /**
     * 清除会话
     */
    public static  void clearSession(){
        InvokerExchangeFilter.clearSession();
    }
    /**
     * 根据调用链id查找该id下的调用链
     * @param invokeId
     * @return
     */
    public static List<InvokerExchangeFilter.ExchangeObject> listInvokeChain(String invokeId){
        return InvokerChainContextFactory.getInstance().listExchangeObject(invokeId);
    }

    /**
     * 删除该id下的调用链
     * @param invokeId
     */
    public static void removeInvokeChain(String invokeId){
       InvokerChainContextFactory.getInstance().remove(invokeId);
    }
    public static InvokerExchangeFilter.ExchangeObject getCurrentInvoke(){
        return InvokerExchangeFilter.getThis();
    }

    public static void addProvidedListener(AbstractProvidedListener apl){
        InvokerExchangeFilter.addProvidedListener(apl);
    }
    public static void addProvidedBeforeListener(AbstractProvidedBeforeListener apl){
        InvokerExchangeFilter.addProvidedBeforeListener(apl);
    }

    /**
     *是否支持事务
     * @return
     */
    public static boolean isSupportTransaction(Map<String,Object> transactionMap){
        return InvokerExchangeVersion.isSupportTransfer(transactionMap);
    }
}

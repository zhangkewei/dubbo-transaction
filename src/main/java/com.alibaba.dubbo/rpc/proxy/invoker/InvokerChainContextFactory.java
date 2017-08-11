package com.alibaba.dubbo.rpc.proxy.invoker;

import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * 调用关系存储容器
 * 类名称：InvokerExchangeApplicationContext
 * 类描述：
 * 创建人：张科伟
 * 创建时间：2016年11月22日
 * @version
 */
public class InvokerChainContextFactory implements IInvokerChainContext{
    private static final int CONTEXT_COUNT=5;
    private List<IInvokerChainContext> _CONTEXTES=new ArrayList<IInvokerChainContext>();
    private InvokerChainContextFactory(){
        for(int i=0;i<CONTEXT_COUNT;i++){
            _CONTEXTES.add(InvokerChainContext.createContext());
        }
    }
    public static InvokerChainContextFactory getInstance(){
        return InvokerChainContextFactoryHelper.factory;
    }

    public InvokerExchangeFilter.ExchangeObject getExchangeObject(String exchangeId) {
        return getContext(InvokerExchangeFilter.ExchangeObject.getRootIdByExchangeId(exchangeId)).getExchangeObject(exchangeId);
    }

    public List<InvokerExchangeFilter.ExchangeObject> listExchangeObject(String exchangeId) {
        return getContext(InvokerExchangeFilter.ExchangeObject.getRootIdByExchangeId(exchangeId)).listExchangeObject(exchangeId);
    }

    public void add(InvokerExchangeFilter.ExchangeObject eo) {
       getContext(InvokerExchangeFilter.ExchangeObject.getRootIdByExchangeId(eo.getExchangeId())).add(eo);
    }

    public void remove(InvokerExchangeFilter.ExchangeObject eo) {
        remove(eo.getExchangeId());
    }
    public void remove(String exchangeId){
        getContext(InvokerExchangeFilter.ExchangeObject.getRootIdByExchangeId(exchangeId)).remove(exchangeId);
    }

    private static class InvokerChainContextFactoryHelper{
        private static  InvokerChainContextFactory factory=new InvokerChainContextFactory();
    }
    private IInvokerChainContext getContext(String exchangeId){
        return _CONTEXTES.get(getHashKey(exchangeId));
    }
    private  int getHashKey(String exchangeId){
        int hashCode=exchangeId.hashCode();
        int hashKey=hashCode%CONTEXT_COUNT;
        return hashKey>0&&hashKey<CONTEXT_COUNT?hashKey:0;
    }
}

package com.alibaba.dubbo.rpc.proxy.invoker;

import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;

import java.util.List;

/**
 * Created by zkevin on 16/11/22.
 */
public interface IInvokerChainContext {
     InvokerExchangeFilter.ExchangeObject getExchangeObject(String exchangeId);
     List<InvokerExchangeFilter.ExchangeObject> listExchangeObject(String exchangeId);
     void add(InvokerExchangeFilter.ExchangeObject eo);
     void remove(String exchangeId);
}

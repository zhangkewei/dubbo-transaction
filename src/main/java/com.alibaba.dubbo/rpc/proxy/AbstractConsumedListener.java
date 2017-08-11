package com.alibaba.dubbo.rpc.proxy;

/**
 * Created by zkevin on 16/12/14.
 */
public abstract class AbstractConsumedListener {
        public abstract void listen(InvokerExchangeFilter.ExchangeObject eo);
}

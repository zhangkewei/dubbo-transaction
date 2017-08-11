package com.alibaba.dubbo.rpc.proxy;

/**
 * 服务提供者调用完成，即会话结束，监听
 */
public abstract class AbstractProvidedListener {
        public abstract void listen(InvokerExchangeFilter.ExchangeObject eo);
}

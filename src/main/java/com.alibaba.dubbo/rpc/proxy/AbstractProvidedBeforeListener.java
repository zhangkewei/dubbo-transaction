package com.alibaba.dubbo.rpc.proxy;

/**
 * 服务提供者调用完成，即会话结束，监听
 */
public abstract class AbstractProvidedBeforeListener {
        public abstract void listen(InvokerExchangeFilter.ExchangeObject invoker,InvokerExchangeFilter.ExchangeObject invoked);
}

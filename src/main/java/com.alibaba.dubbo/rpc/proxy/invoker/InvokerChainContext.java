package com.alibaba.dubbo.rpc.proxy.invoker;

import com.alibaba.dubbo.rpc.proxy.InvokerExchangeFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 调用关系存储容器
 * 类名称：InvokerExchangeApplicationContext
 * 类描述：
 * 创建人：张科伟
 * 创建时间：2016年11月22日
 * @version
 */
public class InvokerChainContext implements IInvokerChainContext{
    private Map<String,List<InvokerExchangeFilter.ExchangeObject>> _EO_CHILDREN=new ConcurrentHashMap<String,List<InvokerExchangeFilter.ExchangeObject>>();
    private Map<String,InvokerExchangeFilter.ExchangeObject> _EO_OBJECT=new ConcurrentHashMap<String,InvokerExchangeFilter.ExchangeObject>();
    private  InvokerChainContext(){}
    protected static InvokerChainContext createContext(){
        InvokerChainContext context=new InvokerChainContext();
        return  context;
    }
    public InvokerExchangeFilter.ExchangeObject getExchangeObject(String exchangeId){
        return _EO_OBJECT.get(exchangeId);
    }
    public List<InvokerExchangeFilter.ExchangeObject> listExchangeObject(String exchangeId){
        return _EO_CHILDREN.containsKey(exchangeId)?Collections.unmodifiableList(_EO_CHILDREN.get(exchangeId)):new ArrayList<InvokerExchangeFilter.ExchangeObject>();
    }
    public void add(InvokerExchangeFilter.ExchangeObject eo){
       if(!_EO_CHILDREN.containsKey(eo.getInvokerId())){
           synchronized (_EO_CHILDREN) {
               if(!_EO_CHILDREN.containsKey(eo.getInvokerId())) {
                   _EO_CHILDREN.put(eo.getInvokerId(), new CopyOnWriteArrayList<InvokerExchangeFilter.ExchangeObject>());
               }
           }
       }
       _EO_CHILDREN.get(eo.getInvokerId()).add(eo);
       _EO_OBJECT.put(eo.getExchangeId(),eo);
    }
    public void remove(String exchangeId){
        if(_EO_OBJECT.containsKey(exchangeId))_EO_OBJECT.remove(exchangeId);
        if(_EO_CHILDREN.containsKey(exchangeId)){
            //删除exchangeId链路
            List<InvokerExchangeFilter.ExchangeObject> children=_EO_CHILDREN.get(exchangeId);
            _EO_CHILDREN.remove(exchangeId);
            //在_EO_OBJECT中删除关联链路
            for(InvokerExchangeFilter.ExchangeObject eo:children){
                if(_EO_OBJECT.containsKey(eo.getExchangeId()))_EO_OBJECT.remove(eo.getExchangeId());
            }
        }

    }
}

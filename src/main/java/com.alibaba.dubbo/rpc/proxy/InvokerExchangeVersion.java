package com.alibaba.dubbo.rpc.proxy;

import java.util.Map;

/**
 * 调用链协议版本
 * VERSION_UNSUPPORT VERSION_UNKNOWN_NULL不参与调用链协议
 */
public enum InvokerExchangeVersion {
    UNSUPPORT("UNSUPPORT","不支持"),
    UNKNOWN_NULL("UNKNOWN_NULL","未知，等同于UNSUPPORT"),
    PRODUCT_1_0("1.0","产品1.0版本");
    protected static final String VERSION_KEY="CHIT_DUBBO";
    protected static final String DEFAULT_VERSION=PRODUCT_1_0.version;
    private String version;
    private String desc;
    private InvokerExchangeVersion(String version,String desc){
        this.version=version;
        this.desc=desc;
    }

    /**
     * 判断是否支持调用链协议
     * @param transferedMap
     * @return
     */
    public static boolean isSupportTransfer(Map<String,Object> transferedMap){
        String version=null!=transferedMap&&transferedMap.containsKey(VERSION_KEY)?transferedMap.get(VERSION_KEY)+"":UNKNOWN_NULL.version;
        return null!=version&&version.equals(PRODUCT_1_0.version);
    }
    public static boolean isSupportTransfer(){
        return isSupportTransfer(InvokerExchangeFilter.getThis().getExtend());
    }
    public String getVersion(){
        return version;
    }
}

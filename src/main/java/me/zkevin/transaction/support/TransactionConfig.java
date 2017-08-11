package me.zkevin.transaction.support;

import com.alibaba.dubbo.common.utils.StringUtils;
import me.zkevin.transaction.support.utils.NetworkUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * chit-transaction.properties>-D>os env
 *
 * -D 或者 properties
 * transaction.contextCount
 * transaction.contextClass
 * transaction.ip
 * transaction.port
 * transaction.commitOn
 * transaction.executeWorkerSize
 * transaction.bootstrapSize
 * transaction.channelSize
 * transaction.callTimeoutPerLoop
 * transaction.callTimeoutLoops
 * transaction.isProxy
 *
 * os env
 * transaction_contextCount
 * transaction_contextClass
 * transaction_ip
 * transaction_port
 * transaction_commitOn
 * transaction_executeWorkerSize
 * transaction_bootstrapSize
 * transaction_channelSize
 * transaction_callTimeoutPerLoop
 * transaction_callTimeoutLoops
 * transaction_isProxy
 *
 * 系统配置参数
 */
public class TransactionConfig {
    private static final String TRANSACTION_PROPERTIES="/chit-transaction.properties";
    public static final String COMMIT_AUTO="auto";
    public static final String COMMIT_HAND="hand";
    public static final String CALL_SYNC_FINITY="syncFinity";
    public static final String CALL_SYNC="sync";
    //是否默认代理数据库事务,即开启分布式事务
    private static final boolean DEFAULT_TRANSACTION_PROXY = true;
    protected static Logger logger= Logger.getLogger(TransactionConfig.class);
    //hash映射对象池数量
    private String contextCount="2";
    //hash映射对象池实现class
    private String contextClass="com.chit.transaction.context.DBTransactionContext";
    //事务netty服务器ip、端口配置
    private String commitAddr;
    //事务默认提交开关(预留暂不可用)
    private String commitOn=COMMIT_HAND;
    //每bootstrap客户端netty连接数
    private String channelSize="5";
    //客户端netty bootstrap共用事件队列
    private String bootstrapSize="2";
    //工作者线程数量
    private String executeWorkerSize="20";
    //事务为两阶段提交，请求失败事务回滚，sync:有限时间内同步等待返回结果,syncFinity:一直等待
    private String callModel=CALL_SYNC;
    //获取返回结果每次等待时间 milliseconds
    private int callTimeoutPerLoop=10;
    //获取返回结果等待循环次数
    private int callTimeoutLoops=6000;
    private boolean transactionProxy=DEFAULT_TRANSACTION_PROXY;
    //必要时重试次数
    private int retries=3;
    private TransactionConfig(){
        commitAddr=getConfig("transaction.ip",NetworkUtils.getLocalHost().getHostAddress())+":"+getConfig("transaction.port","8910");
        contextCount=getConfig("transaction.contextCount", contextCount);
        contextClass=getConfig("transaction.contextClass",contextClass);
        commitOn=getConfig("transaction.commitOn",commitOn);
        channelSize=getConfig("transaction.channelSize",channelSize);
        bootstrapSize=getConfig("transaction.bootstrapSize",bootstrapSize);
        executeWorkerSize=getConfig("transaction.executeWorkerSize",executeWorkerSize);
        callModel=getConfig("transaction.callModel",callModel);
        String callTimeoutPerLoopStr=getConfig("transaction.callTimeoutPerLoop",callTimeoutPerLoop+"");
        String callTimeoutLoopsStr=getConfig("transaction.callTimeoutLoops",callTimeoutLoops+"");
        if(null!=callTimeoutPerLoopStr&&!callTimeoutPerLoopStr.trim().isEmpty()&& StringUtils.isNumeric(callTimeoutPerLoopStr)){
            callTimeoutPerLoop=Double.valueOf(callTimeoutPerLoopStr).intValue();
        }
        if(null!=callTimeoutLoopsStr && !callTimeoutLoopsStr.trim().isEmpty()&& StringUtils.isNumeric(callTimeoutLoopsStr)){
            callTimeoutLoops=Double.valueOf(callTimeoutLoopsStr).intValue();
        }

        transactionProxy=Boolean.valueOf(getConfig("transaction.isProxy",DEFAULT_TRANSACTION_PROXY+""));
        logger.info(this.toString());
    }

    public static  TransactionConfig getInstance(){
        return TransactionConfigHelper.CONFIG;
    }
    private static  class TransactionConfigHelper{
        protected  static TransactionConfig CONFIG=new TransactionConfig();
    }
    protected String getConfig(String key,String defaultValue){
        String configValue=null;
        //先从XXXX.properties中拿值
        configValue=ConfigLoad.getInstance().getConfig(key);
        //如果拿不到值再从java -DXXX=XXX提取配置
        if((null==configValue||configValue.trim().isEmpty())&&null!=System.getProperty(key)){
            configValue=System.getProperty(key).trim();
        }
        //最后从环境变量中取值，由于操作系统环境变量设置.为特殊字符故需要特殊处理
        if(null==configValue||configValue.trim().isEmpty()){
            configValue=System.getenv(key.replace(".","_"));
        }
        return null==configValue||configValue.trim().isEmpty()?defaultValue:configValue;
    }
    private static class ConfigLoad{
        private static ConfigLoad cLoad=new ConfigLoad();
        private Properties properties;
        private ConfigLoad(){
            properties = new Properties();
            try {
                InputStream is=TransactionConfig.class.getResourceAsStream(TRANSACTION_PROPERTIES);
                if(null!=is)properties.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public String getConfig(String key){
            return null!=properties&&properties.containsKey(key)?properties.getProperty(key,null):null;
        }
        public static ConfigLoad getInstance(){
            return cLoad;
        }
    }

    public String getCommitAddr() {
        return commitAddr;
    }

    public void setCommitAddr(String commitAddr) {
        this.commitAddr = commitAddr;
    }

    public String getCommitOn() {
        return commitOn;
    }

    public void setCommitOn(String commitOn) {
        this.commitOn = commitOn;
    }

    public String getContextClass() {
        return contextClass;
    }

    public void setContextClass(String contextClass) {
        this.contextClass = contextClass;
    }

    public String getContextCount() {
        return contextCount;
    }

    public void setContextCount(String contextCount) {
        this.contextCount = contextCount;
    }

    public int contextCount2Int(){
        try {
            return Double.valueOf(contextCount).intValue();
        }catch (Exception e){
            return 1;
        }
    }
    public InetSocketAddress commitAddr2SocketAddress(){
        String[] addr=commitAddr.split(":");
        return null!=addr&&addr.length==2?new InetSocketAddress(addr[0],Integer.parseInt(addr[1])):null;
    }

    public int channelSize2Int(){
        try {
            return Double.valueOf(channelSize).intValue();
        }catch (Exception e){
            return 10;
        }
    }
    public int executeWorkerSize2Int(){
        try {
            return Double.valueOf(executeWorkerSize).intValue();
        }catch (Exception e){
            return 200;
        }
    }
    public int bootstrapSize2Int(){
        try {
            return Double.valueOf(bootstrapSize).intValue();
        }catch (Exception e){
            return 2;
        }
    }
    public Class contextClass2Class(){
        Class clazz=null;
        try {
            clazz=Class.forName(contextClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return clazz;
    }

    public int getRetries() {
        return retries;
    }

    public String getCallModel() {
        return callModel;
    }

    public int getCallTimeoutLoops() {
        return callTimeoutLoops;
    }

    public int getCallTimeoutPerLoop() {
        return callTimeoutPerLoop;
    }

    public String toString(){
        return "TransactionConfig:contextCount=>"+contextCount+",contextClass=>"+contextClass+",commitAddr=>"+commitAddr+",commitOn=>"+commitOn;
    }

    public boolean isTransactionProxy() {
        return transactionProxy;
    }
}

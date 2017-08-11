package me.zkevin.transaction.support.notice.netty.command;



import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.UUID;


/**
 * Created by zkevin on 16/11/23.
 */
public abstract class Command  implements Serializable{
    protected static final Logger logger=Logger.getLogger(Command.class);
    public static final String COMMAND_PREPARE = "prepare";
    public static final String COMMAND_COMMIT = "commit";
    public static final String COMMAND_ROLLBACK = "rollback";
    private String ip;
    private String port;
    private String transactionId;
    private String type;
    private boolean isInvoker=false;
    private String uniqKey;
    public Command(){
    }
    public Command(String type,String transactionId){
        this.type=type;
        this.transactionId=transactionId;
    }
    public Command beforeExecute(String ip,String port){
        this.ip=ip;
        this.port=port;
        this.uniqKey=Base64.encodeBase64URLSafeString(UUID.randomUUID().toString().getBytes());
        return this;
    }
    public CommandResult execute(){
        //logger.debug("command execute:"+JSONObject.toJSONString(this));
        CommandResult result=doExecute();
        //logger.debug("command executed:"+JSONObject.toJSONString(result));
        return result;
    }

    protected abstract CommandResult doExecute();

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public String getUniqKey(){
        return  uniqKey;
    }
    public String getServerKey(){
        return Base64.encodeBase64URLSafeString((ip+"_"+port).getBytes());
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(String port) {
        this.port = port;
    }
    public Command convert2(String commandType){
        Command command=null;
        if(commandType.equals(COMMAND_PREPARE)){
            command=new PrepareCommand(transactionId);
        }else if(commandType.equals(COMMAND_ROLLBACK)){
            command=new RollbackCommand(transactionId);
        }
        if(null!=command){
            command.setUniqKey(null);
            command.beforeExecute(ip, port);
        }
        return command;
    }
    public boolean isInvoker() {
        return isInvoker;
    }

    public void setIsInvoker(boolean isInvoker) {
        this.isInvoker = isInvoker;
    }

    public void setUniqKey(String uniqKey) {
        this.uniqKey = uniqKey;
    }

    public  String toJSONString(){
        return JSONObject.toJSONString(this);
    }
}

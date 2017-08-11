package me.zkevin.transaction.support.notice.netty.command;

import com.alibaba.fastjson.JSONObject;
import me.zkevin.transaction.support.notice.netty.NettyClientFacade;
import io.netty.channel.ChannelFuture;

import java.io.Serializable;

/**
 * Created by zkevin on 16/11/23.
 */
public class CommandResult  implements Serializable {
    public static final String CODE_SUCCESS="200";
    public static final String CODE_ERROR="500";
    public static final String CODE_HANDLING="100";
    public static final String CODE_RETRY="400";
    private String code;
    private String msg;
    private Command command;
    private ChannelFuture future;

    public static CommandResult result(String code,String msg){
        CommandResult result=new CommandResult();
        result.code=code;
        result.msg=msg;
        return result;
    }
    public static CommandResult success(){
        CommandResult result=new CommandResult();
        result.code=CODE_SUCCESS;
        return result;
    }
    public static CommandResult error(Throwable e){
        CommandResult result=new CommandResult();
        result.code=CODE_ERROR;
        if(null!=e)result.msg=e.getMessage();
        return result;
    }
    public CommandResult setCommand(Command command){
        this.command=command;
        return this;
    }
    private void sync(){
        if(null!=future&&command!=null&&(null==code||code.trim().isEmpty()||code.trim().equals(CODE_HANDLING))) {
            NettyClientFacade.getInstance().bindResult(this);
        }
    }
    public boolean isSuccess(){
        sync();
        return CODE_SUCCESS.equals(code);
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Command getCommand() {
        return command;
    }

    public String getMsg() {
        sync();
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static CommandResult async(ChannelFuture future) {
        CommandResult result=new CommandResult();
        result.future=future;
        result.code=CODE_HANDLING;
        return result;
    }
    public void copy(CommandResult result){
        code=result.code;
        msg=result.msg;
        future=null;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    public boolean isHandling() {
        return code.equals(CODE_HANDLING);
    }
    public boolean isRetry() {
        return code.equals(CODE_RETRY);
    }
    public String getCode() {
        sync();
        return code;
    }
    public String toJSONString(){
        return JSONObject.toJSONString(this);
    }
}

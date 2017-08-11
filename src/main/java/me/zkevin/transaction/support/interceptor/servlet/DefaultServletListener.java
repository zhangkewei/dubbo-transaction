package me.zkevin.transaction.support.interceptor.servlet;

import me.zkevin.transaction.exceptions.RpcTransactionRollbackException;
import me.zkevin.transaction.support.DefaultSessionHook;
import me.zkevin.transaction.support.SessionHook;
import org.apache.log4j.Logger;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * 基于javaee servlet容器实现的事务控制
 * Created by zkevin on 16/12/7.
 */
public class DefaultServletListener implements ServletRequestListener {
    private static final Logger log= Logger.getLogger(DefaultServletListener.class);
    private ThreadLocal<SessionHook> HOOK=new ThreadLocal<SessionHook>();
    public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
        HttpServletRequest request=(HttpServletRequest)servletRequestEvent.getServletRequest();
        long now=System.currentTimeMillis();
        try {
            HOOK.get().postHook();
        } catch (RpcTransactionRollbackException e) {
            e.printStackTrace();
        }
        log.debug(request.getRequestURL()+" spend:"+(System.currentTimeMillis()-now));
        HOOK.set(null);
    }

    public void requestInitialized(ServletRequestEvent servletRequestEvent) {
        HOOK.set(new DefaultSessionHook());
        HOOK.get().preHook();
    }
}

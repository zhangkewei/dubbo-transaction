package me.zkevin.transaction.support.interceptor.spring;

import me.zkevin.transaction.support.SessionHook;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * spring mvc控制器捕获
 * Created by zkevin on 16/12/5.
 */
public  class DefaultExceptionHandler  implements HandlerExceptionResolver {
    private static final Logger log= Logger.getLogger(DefaultExceptionHandler.class);
    //在什么异常下回滚事务
    private String rollbackFor;
    //事务提交回滚钩子
    private SessionHook hook;
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse httpServletResponse, Object o, Exception e) {
        long now=System.currentTimeMillis();
        hook.exceptionHook(e,rollbackFor);
        log.debug(request.getRequestURL() + " spend:" + (System.currentTimeMillis() - now));
        return  new ModelAndView("/error");
    }
    public void setHook(SessionHook hook) {
        this.hook = hook;
    }
    public void setRollbackFor(String rollbackFor) {
        this.rollbackFor = rollbackFor;
    }
}

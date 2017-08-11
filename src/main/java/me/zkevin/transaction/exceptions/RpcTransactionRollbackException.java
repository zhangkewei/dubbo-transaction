package me.zkevin.transaction.exceptions;

import java.sql.SQLException;

/**
 * rpc transaction rollback exception
 * Created by zkevin on 16/12/19.
 */
public class RpcTransactionRollbackException extends SQLException {
    public RpcTransactionRollbackException(String msg) {
        super(msg);
    }
}

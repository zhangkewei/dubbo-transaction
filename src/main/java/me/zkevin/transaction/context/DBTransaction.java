package me.zkevin.transaction.context;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;
import me.zkevin.transaction.support.TransactionId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by zkevin on 16/11/11.
 */
public class DBTransaction  extends Transaction{
    private DefaultTransactionStatus status;
    private DataSource dataSource;
    private boolean isNewConnectionHolder;
    private DBTransaction(){

    }

    public static DBTransaction createDbTransaction(DefaultTransactionStatus status){
        DBTransaction dbt=new DBTransaction();
        dbt.status=status;
        dbt.setTransactionId(TransactionId.get());
        return dbt;
    }

    public DefaultTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(DefaultTransactionStatus status) {
        this.status = status;
    }
    public boolean prepare(){
        try {
            if (status.getTransaction() instanceof JdbcTransactionObjectSupport) {
                JdbcTransactionObjectSupport support = (JdbcTransactionObjectSupport) status.getTransaction();
                ConnectionHolder holder = support.getConnectionHolder();
                Connection con = holder.getConnection();
                return !con.isClosed() && !con.isReadOnly();
            }
        }catch (Exception e){
        }
        return false;
    }
    @Override
    public boolean commit() throws SQLException {
        logger.debug("[realDbCommit]"+getTransactionId().getCurrentId());
        if(status.getTransaction() instanceof JdbcTransactionObjectSupport){
            JdbcTransactionObjectSupport support=(JdbcTransactionObjectSupport)status.getTransaction();
            ConnectionHolder holder=support.getConnectionHolder();
            Connection con = holder.getConnection();
            con.commit();
            logger.debug("[realDbCommit]["+getTransactionId().getCurrentId()+"]"+con);
        }
        clearConnection();
        return true;
    }
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    public void setIsNewConnectionHolder(boolean isNewConnectionHolder) {
        this.isNewConnectionHolder = isNewConnectionHolder;
    }

    @Override
    public boolean rollback() throws SQLException {
        if(status.getTransaction() instanceof JdbcTransactionObjectSupport) {
            JdbcTransactionObjectSupport support = (JdbcTransactionObjectSupport) status.getTransaction();
            ConnectionHolder holder = support.getConnectionHolder();
            Connection con = holder.getConnection();
            con.rollback();
        }
        clearConnection();
        return true;
    }
    private void clearConnection(){
        if(status.getTransaction() instanceof JdbcTransactionObjectSupport) {
            JdbcTransactionObjectSupport support = (JdbcTransactionObjectSupport) status.getTransaction();
            ConnectionHolder holder = support.getConnectionHolder();
            Connection con = holder.getConnection();
            try {
                if (con.getAutoCommit()) {
                    con.setAutoCommit(true);
                }
                DataSourceUtils.resetConnectionAfterTransaction(con, support.getPreviousIsolationLevel());
            } catch (Throwable ex) {
                logger.error("Could not reset JDBC Connection after transaction", ex);
            }
            if (isNewConnectionHolder) {
                try {
                    logger.info("Releasing JDBC Connection [" + con + "] after transaction");
                    holder.released();
                    logger.debug("Returning JDBC Connection to DataSource");
                    DataSourceUtils.doCloseConnection(con, dataSource);
                }
                catch (SQLException ex) {
                    logger.debug("Could not close JDBC Connection", ex);
                } catch (Throwable ex) {
                    logger.debug("Unexpected exception on closing JDBC Connection", ex);
                }
            }
            holder.clear();
        }
    }
}

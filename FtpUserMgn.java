package cn.com.miaoto.ftpServer;

import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.DbUserManagerFactory;
import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * ftp用户管理
 * Created by hx on 2017-11-22.
 */
@Component
public class FtpUserMgn {

    public static final Logger LOGGER = LoggerFactory.getLogger(FtpUserMgn.class);

    private DbUserManagerFactory userManagerFactory;

    private UserManager userManager;

    @Resource
    @Qualifier("dataSource")
    DataSource dataSource;


    @PostConstruct
    public void init() {

        userManagerFactory = new DbUserManagerFactory();
        userManagerFactory.setDataSource(dataSource);
        // md5加密
        userManagerFactory.setPasswordEncryptor(new Md5PasswordEncryptor());
        userManagerFactory.setSqlUserSelect("SELECT " +
                "email as userid, " +
                "ftp_pwd as userpassword, " +
                "ftp_home as homedirectory, " +
                "ftp_enable as enableflag, " +
                "ftp_write_enable as writepermission, " +
                "ftp_idle as idletime, " +
                "ftp_update_rate as uploadrate, " +
                "ftp_down_rate as downloadrate, " +
                "ftp_max_login as maxloginnumber, " +
                "ftp_max_login_ip as maxloginperip " +
                "FROM sys_userinfo WHERE email = '{userid}'");
        userManagerFactory.setAdminName("");
        userManagerFactory.setSqlUserDelete("");
        userManagerFactory.setSqlUserAuthenticate("SELECT ftp_pwd as userpassword from sys_userinfo WHERE email='{userid}'");
        userManagerFactory.setSqlUserSelectAll("");
        userManagerFactory.setSqlUserUpdate("");
        userManagerFactory.setSqlUserInsert("");
        userManagerFactory.setSqlUserAdmin("");
        userManager = userManagerFactory.createUserManager();
    }


    public UserManager getUserManager() {
        return userManager;
    }

}

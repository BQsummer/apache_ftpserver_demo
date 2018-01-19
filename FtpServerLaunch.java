package cn.com.miaoto.ftpServer;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hx on 2017-11-22.
 */
@Component
public class FtpServerLaunch {

    private static final Logger logger = LoggerFactory.getLogger(FtpServerLaunch.class);

    private FtpServer ftpServer;

    @Resource
    private FtpUserMgn ftpUserMgn;

    @Resource
    private PggFtplet pggFtplet;

    private static final int MAX_IDLE_TIME = 300;

    private int port = 2121;
    private int activePort = 2122;
    private String passivePort = "2120";
    private Integer maxLogin = 100;
    private Integer maxThreads = 100;


    @PostConstruct
    private void start() {

        FtpServerFactory serverFactory = new FtpServerFactory();

        // FTP服务连接配置
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);
        connectionConfigFactory.setMaxLogins(maxLogin);
        connectionConfigFactory.setMaxThreads(maxThreads);
        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

        ListenerFactory listenerFactory = new ListenerFactory();
        // 配置FTP端口
        listenerFactory.setPort(port);
        // 设置passive
        DataConnectionConfigurationFactory dataConnectionConfFactory = new DataConnectionConfigurationFactory();
        dataConnectionConfFactory.setActiveEnabled(true);
        dataConnectionConfFactory.setActiveLocalPort(activePort);
        //dataConnectionConfFactory.setActiveLocalAddress("localhost");
        dataConnectionConfFactory.setPassivePorts(passivePort);
        //dataConnectionConfFactory.setPassiveAddress("localhost");
        //dataConnectionConfFactory.setPassiveExternalAddress("106.14.137.132");
        listenerFactory.setDataConnectionConfiguration(dataConnectionConfFactory.createDataConnectionConfiguration());

        // 替换默认监听器
        serverFactory.addListener("default", listenerFactory.createListener());
        // 设置用户管理
        serverFactory.setUserManager(ftpUserMgn.getUserManager());
        // 设置ftplet
        Map<String, Ftplet> map = new HashMap<>();
        map.put("default", pggFtplet);
        serverFactory.setFtplets(map);

        // 创建并启动FTP服务
        ftpServer = serverFactory.createServer();
        try {
            System.out.println("start ftp");
            ftpServer.start();
        } catch (Exception e) {
            logger.warn("ftp start failed, ", e);
            throw new RuntimeException(e);
        }
        logger.info("ftp start success, port = :" + port);
    }

    @PreDestroy
    private void stop() {
        if (ftpServer != null) {
            ftpServer.stop();
        }
    }

}

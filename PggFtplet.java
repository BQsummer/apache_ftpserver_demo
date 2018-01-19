package cn.com.miaoto.ftpServer;

import cn.com.miaoto.common.AppConstants;
import cn.com.miaoto.component.GlobalSetting;
import cn.com.miaoto.model.File;
import cn.com.miaoto.model.Userinfo;
import cn.com.miaoto.service.FileService;
import cn.com.miaoto.service.UserinfoService;
import cn.com.miaoto.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;

/**
 * Created by hx on 2017-11-27.
 */
@Component
@Scope("prototype")
public class PggFtplet extends DefaultFtplet {

    private String rnfrName;

    @Resource
    FileService fileService;

    @Resource
    UserinfoService userinfoService;

    @Resource
    private GlobalSetting globalSetting;

    public static final Logger LOGGER = LoggerFactory.getLogger(PggFtplet.class);

    @Override
    public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
        String command = request.getCommand().toUpperCase();
        return "DELE".equals(command) ? this.onDeleteStart(session, request) : ("STOR".equals(command) ? this.onUploadStart(session, request) : ("RETR".equals(command) ? this.onDownloadStart(session, request) : ("RMD".equals(command) ? this.onRmdirStart(session, request) : ("MKD".equals(command) ? this.onMkdirStart(session, request) : ("APPE".equals(command) ? this.onAppendStart(session, request) : ("STOU".equals(command) ? this.onUploadUniqueStart(session, request) : ("RNTO".equals(command) ? this.onRenameStart(session, request) : ("SITE".equals(command) ? this.onSite(session, request) : ("RNFR".equals(command) ? this.onRnftStart(session, request) : null)))))))));
    }

    @Override
    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
        String command = request.getCommand().toUpperCase();
        return "PASS".equals(command) ? this.onLogin(session, request) : ("DELE".equals(command) ? this.onDeleteEnd(session, request) : ("STOR".equals(command) ? this.onUploadEnd(session, request) : ("RETR".equals(command) ? this.onDownloadEnd(session, request) : ("RMD".equals(command) ? this.onRmdirEnd(session, request) : ("MKD".equals(command) ? this.onMkdirEnd(session, request) : ("APPE".equals(command) ? this.onAppendEnd(session, request) : ("STOU".equals(command) ? this.onUploadUniqueEnd(session, request) : ("RNTO".equals(command) ? this.onRenameEnd(session, request) : null))))))));
    }

    @Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request) throws FtpException, IOException {
        // home目录是否存在
        Userinfo user = userinfoService.findByMail(session.getUser().getName(), AppConstants.SYS_FROM_ALL);
        // 用户不存在
        if (user == null) {
            LOGGER.error("ftp onConnect: find user by email failed, email = {}, sysfrom = {}", session.getUser().getName(), AppConstants.SYS_FROM_ALL);
            return null;
        }
        if (StringUtils.isEmpty(user.getFtpHome())) {
            LOGGER.error("ftp onUploadEnd: find home dir failed, homeDir = {}", user.getFtpHome());
            return null;
        }
        // 检查目录是否存在,不存在及创建
        java.io.File file = new java.io.File(user.getFtpHome());
        if (!file.exists() && !file.isDirectory()) {
            LOGGER.warn("uid = {}, ftpHome = {} not exist, wanna mkdir", user.getId(), user.getFtpHome());
            file.mkdir();
        }
        return null;
    }

    @Override
    public FtpletResult onDeleteStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        Userinfo user = userinfoService.findByMail(session.getUser().getName(), AppConstants.SYS_FROM_ALL);
        // 用户不存在
        if (user == null) {
            LOGGER.error("ftp onConnect: find user by email failed, email = {}, sysfrom = {}", session.getUser().getName(), AppConstants.SYS_FROM_ALL);
            return null;
        }
        // 更新status
        int effected = fileService.updateStatusByUidAndPath(user.getId(), StringUtil.createDir(user.getFtpHome(), session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath()), AppConstants.FILE_STATUS_DELETED);
        if (effected == 0) {
            LOGGER.error("ftp onDeleteEnd: update file status deleted failed, uid = {}, path = {}", user.getId(), StringUtil.createDir(user.getFtpHome(), session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath()));
        }
        return null;
    }

    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        // TODO
        return null;
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {//System.out.println(session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath());  // 相对于home的路径
        Userinfo user = userinfoService.findByMail(session.getUser().getName(), AppConstants.SYS_FROM_ALL);
        // 用户不存在
        if (user == null) {
            LOGGER.error("ftp onUploadEnd: find user by email failed, email = [}, sysFrom = {}", session.getUser().getName(), AppConstants.SYS_FROM_ALL);
            return null;
        }

        File file = new File();
        file.setName(request.getArgument());
        file.setPath(StringUtil.createDir(user.getFtpHome(), session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath()));
        file.setSize(session.getFileSystemView().getFile(request.getArgument()).getSize());
        file.setUid(user.getId());
        file.setCreatetime(new Date());
        file.setUpdatetime(new Date());
        file.setStatus(AppConstants.FILE_STATUS_OK);
        file.setType(AppConstants.FILE_TYPE_FTP);
        int effected = fileService.saveModel(file);
        if (effected != 1) {
            LOGGER.error("ftp onUploadEnd: insert file failed, file = {}", file.toString());
            return null;
        }
        return null;
    }

    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onDownloadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onRmdirStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onAppendStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onAppendEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onUploadUniqueStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

    @Override
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }


    public FtpletResult onRnftStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        rnfrName = request.getArgument();
        return null;
    }

    @Override
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        System.out.println(session.getRenameFrom());
        // 更新文件名
        Userinfo user = userinfoService.findByMail(session.getUser().getName(), AppConstants.SYS_FROM_ALL);
        // 用户不存在
        if (user == null) {
            LOGGER.error("ftp onUploadEnd: find user by email failed, email = {}, sysFrom = {}", session.getUser().getName(), AppConstants.SYS_FROM_ALL);
            return null;
        }
        String absDir = session.getFileSystemView().getFile(request.getArgument()).getAbsolutePath();
        int effected = fileService.updateNameByUidAndPath(
                user.getId(),
                StringUtil.createDir(user.getFtpHome(), absDir.substring(0, absDir.length() - request.getArgument().length()), rnfrName),
                request.getArgument(),
                StringUtil.createDir(user.getFtpHome(), absDir));
        if (effected == 0) {
            LOGGER.error("ftp onRenameEnd: update name failed, uid = {}, rnfr = {}, rnto = {}", user.getId(), rnfrName, request.getArgument());
        }
        return null;
    }

    @Override
    public FtpletResult onSite(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return null;
    }

}

package com.zcunsoft.clklog.sysmgmt.bootstrap;

import com.zcunsoft.clklog.common.utils.SecurityUtils;
import com.zcunsoft.clklog.common.utils.StringUtils;
import com.zcunsoft.clklog.sysmgmt.domains.User;
import com.zcunsoft.clklog.sysmgmt.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * 空库时创建唯一管理员，并强制首次改密。
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    @Value("${CLKLOG_BOOTSTRAP_ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${CLKLOG_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Resource
    private IUserRepository userRepository;

    @Override
    @Transactional("mysqlTransactionManager")
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (adminPassword == null || adminPassword.isEmpty()) {
            throw new IllegalStateException("CLKLOG_BOOTSTRAP_ADMIN_PASSWORD is required to create the initial admin");
        }
        if (!StringUtils.isPasswordStandard(adminPassword)) {
            throw new IllegalStateException("CLKLOG_BOOTSTRAP_ADMIN_PASSWORD does not meet complexity policy");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        User admin = new User();
        admin.setUserId(UUID.randomUUID().toString());
        admin.setUserName(adminUsername);
        admin.setDisplayName("管理员");
        admin.setPassword(SecurityUtils.encryptPassword(adminPassword));
        admin.setPwdResetRequired(Boolean.TRUE);
        admin.setCreatetime(now);
        userRepository.save(admin);
    }
}

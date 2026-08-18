package com.zcunsoft.clklog.sysmgmt.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 启动时自动修复 MySQL 表结构，兼容镜像版旧库升级到源码版。
 * <p>
 * 源码版 User 实体新增了 pwd_reset_required 列，镜像版 1.3.0 建的库没有该列，
 * 会导致 JPA 查询被 catch 吞掉后返回 null，表现为"登录用户不存在"。
 * 本 runner 在应用启动前检查并补列，避免每次切源码都要手工跑 DDL。
 */
@Component
@Order(Integer.MIN_VALUE)
public class SchemaMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    @Resource(name = "mysqlDataSource")
    private DataSource mysqlDataSource;

    @Override
    public void run(String... args) {
        if (mysqlDataSource == null) {
            logger.warn("mysqlDataSource is null, skip schema migration");
            return;
        }
        try (Connection conn = mysqlDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 自动推断当前库名（兼容不同部署环境）
            String catalog = conn.getCatalog();
            if (catalog == null || catalog.isEmpty()) {
                catalog = "clklog";
            }

            String checkSql = "SELECT 1 FROM information_schema.columns " +
                    "WHERE table_schema = '" + catalog + "' " +
                    "AND table_name = 'sys_user' " +
                    "AND column_name = 'pwd_reset_required'";

            boolean exists;
            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                exists = rs.next();
            }

            if (!exists) {
                logger.info("Adding missing column sys_user.pwd_reset_required ...");
                String alterSql = "ALTER TABLE sys_user ADD COLUMN pwd_reset_required tinyint(1) NOT NULL DEFAULT 0 COMMENT '首次登录必须改密'";
                stmt.execute(alterSql);
                logger.info("Column sys_user.pwd_reset_required added successfully");
            } else {
                logger.info("sys_user.pwd_reset_required already exists");
            }

            // 官方默认账号（admin / clklog）升级后应可直接登录，不强制改密
            int cleared = stmt.executeUpdate(
                    "UPDATE sys_user SET pwd_reset_required = 0 WHERE user_name IN ('admin', 'clklog') AND pwd_reset_required = 1");
            if (cleared > 0) {
                logger.info("Cleared pwd_reset_required for {} official default account(s)", cleared);
            }

        } catch (Exception e) {
            // 不要因迁移失败阻断启动，让应用按原有逻辑报错，便于排查
            logger.error("Schema migration failed, please run DDL manually: " +
                    "ALTER TABLE sys_user ADD COLUMN pwd_reset_required tinyint(1) NOT NULL DEFAULT 0;", e);
        }
    }
}

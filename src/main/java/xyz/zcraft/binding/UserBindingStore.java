package xyz.zcraft.binding;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class UserBindingStore {
    private static final Logger LOG = LogManager.getLogger(UserBindingStore.class);
    private static final Object INIT_LOCK = new Object();

    private static volatile String jdbcUrl;

    private UserBindingStore() {
    }

    public static void init(String sqlitePath) {
        synchronized (INIT_LOCK) {
            if (jdbcUrl != null) {
                return;
            }
            try {
                Path dbPath = Path.of(sqlitePath).toAbsolutePath().normalize();
                Path parent = dbPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                jdbcUrl = "jdbc:sqlite:" + dbPath;
                createTablesIfNeeded();
                LOG.info("SQLite binding store initialized at {}", dbPath);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize sqlite store: " + sqlitePath, e);
            }
        }
    }

    public static void bind(String platform, String platformUserId, int osuUid) {
        ensureInitialized();
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO user_bindings(platform, platform_user_id, osu_uid, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?)
                ON CONFLICT(platform, platform_user_id) DO UPDATE SET
                    osu_uid = excluded.osu_uid,
                    updated_at = excluded.updated_at
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, platform);
            statement.setString(2, platformUserId);
            statement.setInt(3, osuUid);
            statement.setLong(4, now);
            statement.setLong(5, now);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist binding", e);
        }
    }

    public static Integer findBoundUid(String platform, String platformUserId) {
        ensureInitialized();
        String sql = "SELECT osu_uid FROM user_bindings WHERE platform = ? AND platform_user_id = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, platform);
            statement.setString(2, platformUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("osu_uid");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query binding", e);
        }
        return null;
    }

    public static boolean unbind(String platform, String platformUserId) {
        ensureInitialized();
        String sql = "DELETE FROM user_bindings WHERE platform = ? AND platform_user_id = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, platform);
            statement.setString(2, platformUserId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete binding", e);
        }
    }

    public static void upsertGroupMember(String platform, String groupId, String platformUserId) {
        ensureInitialized();
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO group_members(platform, group_id, platform_user_id, updated_at)
                VALUES(?, ?, ?, ?)
                ON CONFLICT(platform, group_id, platform_user_id) DO UPDATE SET
                    updated_at = excluded.updated_at
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, platform);
            statement.setString(2, groupId);
            statement.setString(3, platformUserId);
            statement.setLong(4, now);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist group member", e);
        }
    }

    public static List<Integer> findBoundUidsByGroup(String platform, String groupId) {
        ensureInitialized();
        String sql = """
                SELECT DISTINCT ub.osu_uid
                FROM group_members gm
                JOIN user_bindings ub
                  ON ub.platform = gm.platform
                 AND ub.platform_user_id = gm.platform_user_id
                WHERE gm.platform = ? AND gm.group_id = ?
                ORDER BY ub.osu_uid
                """;
        List<Integer> uids = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, platform);
            statement.setString(2, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    uids.add(resultSet.getInt("osu_uid"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query group bindings", e);
        }
        return uids;
    }

    private static void createTablesIfNeeded() throws SQLException {
        String bindingSql = """
                CREATE TABLE IF NOT EXISTS user_bindings (
                    platform TEXT NOT NULL,
                    platform_user_id TEXT NOT NULL,
                    osu_uid INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(platform, platform_user_id)
                )
                """;
        String groupMemberSql = """
                CREATE TABLE IF NOT EXISTS group_members (
                    platform TEXT NOT NULL,
                    group_id TEXT NOT NULL,
                    platform_user_id TEXT NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(platform, group_id, platform_user_id)
                )
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement bindingStatement = connection.prepareStatement(bindingSql);
             PreparedStatement memberStatement = connection.prepareStatement(groupMemberSql)) {
            bindingStatement.execute();
            memberStatement.execute();
        }
    }

    private static void ensureInitialized() {
        if (jdbcUrl == null) {
            throw new IllegalStateException("UserBindingStore is not initialized");
        }
    }
}


package net.shortninja.staffplus.core.common;

import net.shortninja.staffplus.core.domain.synchronization.ServerSyncConfig;

import java.util.UUID;

public class Constants {

    public static UUID CONSOLE_UUID = UUID.fromString("9c417515-22bc-46b8-be4d-538482992f8f");
    public static final String BUNGEE_CORD_CHANNEL = "BungeeCord";
    public static final String BUNGEE_REPORT_CREATED_CHANNEL = "staffplusplus.report-created";
    public static final String BUNGEE_REPORT_DELETED_CHANNEL = "staffplusplus.report-deleted";
    public static final String BUNGEE_REPORT_ACCEPTED_CHANNEL = "staffplusplus.report-accepted";
    public static final String BUNGEE_REPORT_CLOSED_CHANNEL = "staffplusplus.report-closed";
    public static final String BUNGEE_REPORT_REOPEN_CHANNEL = "staffplusplus.report-reopen";

    public static final String BUNGEE_PLAYER_BANNED_CHANNEL = "staffplusplus.banned-player";
    public static final String BUNGEE_PLAYER_UNBANNED_CHANNEL = "staffplusplus.unbanned-player";

    public static final String BUNGEE_IP_BANNED_CHANNEL = "staffplusplus.banned-ip";
    public static final String BUNGEE_IP_UNBANNED_CHANNEL = "staffplusplus.unbanned-ip";

    public static final String BUNGEE_STAFFCHAT_CHANNEL = "StaffPlusPlusChat";
    public static final String BUNGEE_INVESTIGATION_STARTED_CHANNEL = "staffplusplus.investigation-started";
    public static final String BUNGEE_INVESTIGATION_PAUSED_CHANNEL = "staffplusplus.investigation-paused";
    public static final String BUNGEE_INVESTIGATION_CONCLUDED_CHANNEL = "staffplusplus.investigation-concluded";

    public static String getServerNameFilterWithAnd(ServerSyncConfig serverSyncConfig) {
        return getServerNameFilterWithAnd("", serverSyncConfig);
    }

    public static String getServerNameFilterWithAnd(String tableName, ServerSyncConfig serverSyncConfig) {
        if(!tableName.isEmpty()) {
            tableName = tableName + ".";
        }
        if (serverSyncConfig.isMatchesAll()) {
            return "";
        }
        return " AND (" + tableName + "server_name is null OR " + tableName + "server_name IN (" + String.join(",", serverSyncConfig.getServers()) + ") ";
    }

    public static String getServerNameFilterWithWhere(ServerSyncConfig serverSyncConfig) {
        if (serverSyncConfig.isMatchesAll()) {
            return "";
        }
        return " WHERE (server_name is null OR server_name IN (" + String.join(",", serverSyncConfig.getServers()) + ") ";
    }
}

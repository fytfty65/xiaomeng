package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** GET /api/v1/risk/notifications 的 data 段。 */
public final class RiskNotificationsData {

    @SerializedName("notifications")
    private List<RiskNotificationDto> notifications;

    @SerializedName("unread_count")
    private int unreadCount;

    public List<RiskNotificationDto> getNotifications() {
        return notifications == null ? new ArrayList<>() : notifications;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}

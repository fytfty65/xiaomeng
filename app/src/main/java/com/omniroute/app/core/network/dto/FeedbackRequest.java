package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** 路线反馈：score 只允许 1（认可）/ -1（需要调整）。 */
public final class FeedbackRequest {

    @SerializedName("room_id")
    private final String roomId;

    @SerializedName("user_id")
    private final String userId;

    @SerializedName("target")
    private final String target;

    @SerializedName("score")
    private final int score;

    @SerializedName("reason")
    private final String reason;

    public FeedbackRequest(String roomId, String userId, String target, int score, String reason) {
        this.roomId = roomId;
        this.userId = userId;
        this.target = target;
        this.score = score;
        this.reason = reason;
    }
}

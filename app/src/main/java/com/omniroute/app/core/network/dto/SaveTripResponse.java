package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

/** POST /api/user/trip 响应：{"message","trip_id"}。 */
public final class SaveTripResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("trip_id")
    private String tripId;

    public String getMessage() {
        return message;
    }

    public String getTripId() {
        return tripId;
    }
}

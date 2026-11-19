package com.omniroute.app.core.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** GET /api/user/trips 的扁平响应：{"trips": [...]}。 */
public final class TripsResponse {

    @SerializedName("trips")
    private List<TripPlanDto> trips;

    public List<TripPlanDto> getTrips() {
        return trips == null ? new ArrayList<>() : trips;
    }
}

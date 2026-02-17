package com.example.minecraftjira.backend.api;

import com.example.minecraftjira.backend.model.ApiResponse;
import com.example.minecraftjira.backend.model.HelpData;
import com.example.minecraftjira.backend.model.StandupData;
import com.example.minecraftjira.backend.model.StandupRequest;
import com.example.minecraftjira.backend.model.StatusData;
import com.example.minecraftjira.backend.model.UpdateData;
import com.example.minecraftjira.backend.model.UpdateRequest;
import com.example.minecraftjira.backend.service.JiraService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/jira")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JiraResource {
    @Inject
    JiraService jiraService;

    @GET
    @Path("/help")
    public ApiResponse<HelpData> help() {
        return ApiResponse.ok("Available /jira commands", jiraService.help());
    }

    @GET
    @Path("/status")
    public ApiResponse<StatusData> status(@QueryParam("item") String item) {
        if (item == null || item.isBlank()) {
            throw new BadRequestException("Query parameter 'item' is required");
        }

        StatusData data = jiraService.status(item);
        return ApiResponse.ok("Current status fetched", data);
    }

    @POST
    @Path("/update")
    public ApiResponse<UpdateData> update(UpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Body is required");
        }

        UpdateData data = jiraService.update(request);
        return ApiResponse.ok("Project item updated", data);
    }

    @POST
    @Path("/standup")
    public ApiResponse<StandupData> standup(StandupRequest request) {
        StandupData data = jiraService.standup(request);
        return ApiResponse.ok("Standup summary generated", data);
    }
}

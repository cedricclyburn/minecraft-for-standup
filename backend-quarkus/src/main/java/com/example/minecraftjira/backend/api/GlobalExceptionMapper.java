package com.example.minecraftjira.backend.api;

import com.example.minecraftjira.backend.model.ApiResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof BadRequestException badRequestException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(ApiResponse.error(badRequestException.getMessage()))
                    .build();
        }

        if (exception instanceof IllegalArgumentException illegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(ApiResponse.error(illegalArgumentException.getMessage()))
                    .build();
        }

        LOG.error("Unhandled backend exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(ApiResponse.error("Internal error"))
                .build();
    }
}

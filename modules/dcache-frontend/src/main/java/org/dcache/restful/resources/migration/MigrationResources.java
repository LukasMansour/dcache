package org.dcache.restful.resources.migration;

import diskCacheV111.util.AccessLatency;
import diskCacheV111.util.CacheException;
import diskCacheV111.util.PnfsId;
import diskCacheV111.util.RetentionPolicy;
import dmg.cells.nucleus.NoRouteToCellException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import java.util.Collections;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.dcache.auth.attributes.Restriction;
import org.dcache.auth.attributes.Restrictions;
import org.dcache.cells.CellStub;
import org.dcache.pool.migration.PoolMigrationCopyReplicaMessage;
import org.dcache.pool.repository.ReplicaState;
import org.dcache.restful.util.RequestUser;
import org.dcache.vehicles.FileAttributes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * <p>RESTful API for Migration.</p>
 *
 * @author Lukas Mansour
 */
@Component
@Api(value = "migrations", authorizations = {@Authorization("basicAuth")})
@Path("/migrations")
public final class MigrationResources {
    @Context
    private HttpServletRequest request;

    @Inject
    @Named("pool-stub")
    private CellStub poolStub;

    /**
     * Submit a migration copy request.
     * Migration requestto copy all data to another
     *
     * @return response which includes a location HTTP response header with a value that is the
     * absolute URL for the resource associated with this bulk request.
     */
    @POST
    @ApiOperation(value = "Submit a migration copy request. (See Pool Operator Commands 'migration copy')")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 429, message = "Too many requests"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitMigrationCopy(
            @ApiParam(value = "Name of the pool to migrate from. (String)", required = true) String targetPool,
            @ApiParam(value = "ReplicaState after the migration. (String)", required = true) String replicaStateName, // TODO: Make sure this is correct.
            @ApiParam(value = "aTime") Long aTime,
            @ApiParam(value = "forceSourceMode (Boolean)", required = true) boolean forceSourceMode,
            @ApiParam(value = "metaOnly (Boolean)", required = true) boolean metaOnly,
            @ApiParam(value = "fileAttributes via key values in an object." +
                    "accessed - String (<n>|[<n>]..[<m>])- Only copy replicas within a given time period." +
                    "accessLatency - ONLINE|NEARLINE - Only copy replicas with the given access latency." +
                    "pnfsid - Array of PNFSIDs - Only copy replicas with the given PNFSIDs." +
                    "retentionPolicy - CUSTODIAL|REPLICA|OUTPUT - Only copy replicas with the given retention policy." +
                    "size - String (<n>|[<n>]..[<m>]) - Only copy replicas with size <n>, or a size within the given, possibly open-ended, interval." +
                    "sticky - Array of Owners (Strings) - Only copy replicas that are sticky, if the array is not empty, then it will be restricted to the specified owners." +
                    "storage - String - Only copy replicas with a certain storage class.",
                    required = true) String requestPayload
            ) {
        // First convert to JSON.
        JSONObject jsonPayload = new JSONObject(requestPayload);

        // Something to note here: We assume in getSubject() that you HAVE to be an Administrator!
        // Should this change, then the Restrictions must be reapplied accordingly.
        Subject subject = getSubject();
        Restriction restriction = Restrictions.none();
        // Replica State
        ReplicaState replicaState;
        try {
            replicaState = ReplicaState.valueOf(replicaStateName);
        } catch (IllegalArgumentException iae) {
            throw new BadRequestException("Replica state '" + replicaStateName + "' is not a valid replica state.");
        }

        // Create the file attributes from the request.
        FileAttributes.Builder fileAttributesBuilder = FileAttributes.of();
        // Handle the file attributes from the request
        {
            if (jsonPayload.has("accessed")) {
                // TODO: Accessed in FileAttributes.
            }
            if (jsonPayload.has("accessLatency")) {
                AccessLatency accessLatency;
                Object object = jsonPayload.get("accessLatency");
                if (object instanceof String) {
                    accessLatency = AccessLatency.getAccessLatency((String) object);
                } else if (object instanceof Integer) {
                    accessLatency = AccessLatency.getAccessLatency((Integer) object);
                } else {
                    throw new BadRequestException("Unknown format for access latency: " + object);
                }
                fileAttributesBuilder.accessLatency(accessLatency);
            }
            if (jsonPayload.has("pnfsid")) {
                JSONArray pnfsid = jsonPayload.getJSONArray("pnfsid");
                for (int i = 0; i < pnfsid.length(); i++) {
                    String s = pnfsid.getString(i);
                    if (!PnfsId.isValid(s)) {
                        throw new BadRequestException("PNFSID: " + s + " is not valid!");
                    }
                   fileAttributesBuilder.pnfsId(s);
                }
            }
            if (jsonPayload.has("retentionPolicy")) {
                RetentionPolicy retentionPolicy;
                Object object = jsonPayload.get("retentionPolicy");
                if (object instanceof String) {
                    retentionPolicy = RetentionPolicy.getRetentionPolicy((String) object);
                } else if (object instanceof Integer) {
                    retentionPolicy = RetentionPolicy.getRetentionPolicy((Integer) object);
                } else {
                    throw new BadRequestException("Unknown format for retention policy: " + object);
                }
                fileAttributesBuilder.retentionPolicy(retentionPolicy);
            }
            if (jsonPayload.has("size")) {
                // TODO: Size in FileAttributes.
            }
            if (jsonPayload.has("sticky")) {
                // TODO: Sticky Records
            }
            if (jsonPayload.has("storage")) {
                fileAttributesBuilder.storageClass(jsonPayload.getString("storage"));
            }

        }
        FileAttributes fileAttributes = fileAttributesBuilder.build();

        // We need to create the PoolMigrationCopyReplicaMessage
        PoolMigrationCopyReplicaMessage pmcrm = new PoolMigrationCopyReplicaMessage(
                UUID.randomUUID(),
                targetPool,
                fileAttributes,
                replicaState,
                Collections.emptyList(), //TODO: Sticky Records
                true,
                forceSourceMode,
                aTime,
                metaOnly
        );

        try {
            pmcrm.setSubject(subject);

            pmcrm = poolStub.sendAndWait(pmcrm);
        } catch (CacheException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (NoRouteToCellException e) {
            throw new RuntimeException(e);
        }


        // TODO: Proper Response
        return Response.status(Response.Status.CREATED)
                .header("request-url",pmcrm.getId())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    public static Subject getSubject() {
        if (RequestUser.isAnonymous()) {
            throw new NotAuthorizedException("User cannot be anonymous.");
        }

        if (!RequestUser.isAdmin()) {
            throw new NotAuthorizedException("User must be an administrator.");
        }

        return RequestUser.getSubject();
    }
}

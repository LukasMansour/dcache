package org.dcache.restful.resources.migration;

import diskCacheV111.util.AccessLatency;
import diskCacheV111.util.PnfsId;
import diskCacheV111.util.RetentionPolicy;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import javax.security.auth.Subject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.dcache.auth.attributes.Restriction;
import org.dcache.auth.attributes.Restrictions;
import org.dcache.restful.util.RequestUser;
import org.dcache.vehicles.FileAttributes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationResources.class);

    /**
     * Submit a migration copy request.
     * Request to migrate (copy) all data from a source pool to a target pool.
     *
     * @return response which will confirm the execution of the command (no output).
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
            @ApiParam("Description of the request. Which contains the following: " +
                    "sourcePool - Name of the pool to migrate from. (String)" +
                    "concurrency - Integer - Amount of Concurrent Transfers to be performed." +
                    "pins - String (MOVE | KEEP) - Controls how sticky flags owned by the PinManager are handled." +
                    "smode - String (SAME | CACHED | PRECIOUS | REMOVABLE | DELETE)[+<owner>[(<lifetime>)] - " +
                    "Update the local replica to the given mode after transfer. An optional list of sticky flags can be specified." +

                    "tmode - String (SAME | CACHED | PRECIOUS )[+<owner>[(<lifetime>)] - " +
                    "Sets the target replica to the given mode after transfer. An optional list of sticky flags can be specified." +

                    "verify - Boolean - Force checksum computation when an existing target is updated." +
                    "eager - Boolean - Copy replicas rather than retrying when pools with existing replicas fail to respond." +

                    "exclude - Array of Pools (Strings) - Exclude Target Pools. Single character (?) and multi character (*) wildcards may be used." +
                    "include - Array of Pools (Strings) - Only include the specified pools as target pools." +
                    "refresh - Integer - Specifies the period in seconds of when target pool information is queried from the pool manager. The default is 300 seconds." +
                    "select - String (PROPORTIONAL | BEST | RANDOM) - Determines how a pool is selected from the set of target pools." +
                    "target - String (POOL | PGROUP | LINK) - Determines the interpretation of the included pools." +
                    "fileAttributes - Description of the file attributes containing: " +
                    " accessed - String (<n>|[<n>]..[<m>]) - Only copy replicas within a given time period." +
                    " al - String (ONLINE | NEARLINE) - Only copy replicas with the given access latency." +
                    " pnfsid - Array of String (PNFSIDs) - Only copy replicas with the given PNFSIDs, must contain 1 or more PNFSIDs." +
                    " state - String (CACHED | PRECIOUS) - Only copy replicas with the given replica state." +
                    " rp - String (CUSTODIAL | REPLICA | OUTPUT) - Only copy replicas with the given retention policy." +
                    " size - String (<n>|[<n>]..[<m>]) - Only copy replicas with size <n>, or a size within the given, possibly open-ended, interval." +
                    " sticky - Array of Owners (Strings) - Only copy replicas that are sticky, if the array is not empty, then it will be restricted to the specified owners." +
                    " storage - String - Only copy replicas with a certain storage class.") String requestPayload
    ) {
        // TODO: Add expressions (pause-when, include-when, exclude-when, stop-when) to the request.
        // TODO: Pass the migration request as a message and not via a CLI-Message.
        // This was a quick and dirty trick to fulfill some other projects' programmatic contracts.

        // First convert to JSON.
        LOGGER.info("JSON Request: {}", new JSONObject(requestPayload));

        // Something to note here: We assume in getSubject() that you HAVE to be an Administrator!
        // Should this change, then the Restrictions must be reapplied accordingly.
        Subject subject = getSubject();
        Restriction restriction = Restrictions.none();

        // Create the file attributes from the request.
        FileAttributes.Builder fileAttributesBuilder = FileAttributes.of();

        JSONObject jsonPayload = new JSONObject("");
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


        // TODO: Proper Response
        return Response.status(Response.Status.CREATED)
                .header("request-url", "7")
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

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
import javax.json.JsonObject;
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
import org.dcache.restful.resources.billing.BillingResources;
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

    @Context
    private HttpServletRequest request;

    @Inject
    @Named("pool-stub")
    private CellStub poolStub;

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
            @ApiParam(value = "Name of the pool to migrate from. (String)", required = true)
            String sourcePool,
            @ApiParam(value = "-accessed - String (<n>|[<n>]..[<m>]) - Only copy replicas within a given time period.")
            String fileAttrAccessed,
            @ApiParam(value = "-al - String (ONLINE | NEARLINE) - Only copy replicas with the given access latency.")
            String fileAttrAL,
            @ApiParam(value = "-pnfsid - Array of String (PNFSIDs) - Only copy replicas with the given PNFSIDs, must contain 1 or more PNFSIDs.")
            JSONArray fileAttrPnfsids,
            @ApiParam(value = "-state - String (CACHED | PRECIOUS) - Only copy replicas with the given replica state.")
            String fileAttrRS,
            @ApiParam(value = "-rp - String (CUSTODIAL | REPLICA | OUTPUT) - Only copy replicas with the given retention policy.")
            String fileAttrRP,
            @ApiParam(value = "-size - String (<n>|[<n>]..[<m>]) - Only copy replicas with size <n>, or a size within the given, possibly open-ended, interval.")
            String fileAttrSize,
            @ApiParam(value = "-sticky - Array of Owners (Strings) - Only copy replicas that are sticky, if the array is not empty, then it will be restricted to the specified owners.")
            String fileAttrSticky,
            @ApiParam(value = "-storage - String - Only copy replicas with a certain storage class.")
            String fileAttrStorage,
            @ApiParam(value = "-concurrency - Integer - Amount of Concurrent Transfers to be performed.")
            Long concurrency,
            @ApiParam(value = "-pins - String (MOVE | KEEP) - Controls how sticky flags owned by the PinManager are handled.")
            String pins,
            @ApiParam(value = "-smode - String (SAME | CACHED | PRECIOUS | REMOVABLE | DELETE)[+<owner>[(<lifetime>)] - " +
                    "Update the local replica to the given mode after transfer. An optional list of sticky flags can be specified.")
            String smode,
            @ApiParam(value = "-smode - String (SAME | CACHED | PRECIOUS )[+<owner>[(<lifetime>)] - " +
                    "Sets the target replica to the given mode after transfer. An optional list of sticky flags can be specified.")
            String tmode,
            @ApiParam(value = "-verify - Boolean - Force checksum computation when an existing target is updated.")
            boolean verify,
            @ApiParam(value = "-eager - Boolean - Copy replicas rather than retrying when pools with existing replicas fail to respond." )
            boolean eager,
            @ApiParam(value = "-exclude - Array of Pools (Strings) - Exclude Target Pools. Single character (?) and multi character (*) wildcards may be used.")
            JSONArray excludePools,
            @ApiParam(value = "-include - Array of Pools (Strings) - Only include the specified pools as target pools.")
            JSONArray includePools,
            @ApiParam(value = "-refresh - Integer - Specifies the period in seconds of when target pool information is queried from the pool manager. The default is 300 seconds.")
            Long refresh,
            @ApiParam(value = "-select - String (PROPORTIONAL | BEST | RANDOM) - Determines how a pool is selected from the set of target pools.")
            String selector,
            @ApiParam(value = "-target - String (POOL | PGROUP | LINK) - Determines the interpretation of the included pools.")
            String target
    ) {
        // TODO: Add expressions (pause-when, include-when, exclude-when, stop-when) to the request.
        // TODO: Pass the migration request as a message and not via a CLI-Message.
        // This was a quick and dirty trick to fulfill some other projects' programmatic contracts.

        // First convert to JSON.
        LOGGER.info("Source Pool {}", sourcePool);
        LOGGER.info(fileAttrAccessed);
        LOGGER.info(fileAttrAL);
        LOGGER.info(fileAttrPnfsids.toString());
        LOGGER.info(fileAttrRS);
        LOGGER.info(fileAttrRP);
        LOGGER.info(fileAttrSize);
        LOGGER.info(fileAttrSticky);
        LOGGER.info(fileAttrStorage);
        LOGGER.info(concurrency.toString());
        LOGGER.info(pins);
        LOGGER.info(smode);
        LOGGER.info(tmode);
        LOGGER.info(String.valueOf(verify));
        LOGGER.info(String.valueOf(eager));
        LOGGER.info(String.valueOf(excludePools));
        LOGGER.info(String.valueOf(includePools));
        LOGGER.info(String.valueOf(refresh));
        LOGGER.info(String.valueOf(selector));
        LOGGER.info(String.valueOf(target));

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

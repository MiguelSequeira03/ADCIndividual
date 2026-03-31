package pt.unl.fct.di.adc.firstwebapp.resources;


import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.LoginRequest;


@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	public static final String USER_PWD = "user_pwd";
	public static final String USER_ROLE = "user_role";
	private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Attempt to login user: ";
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    public static final String INVALID_CREDENTIALS = "The username-password pair is not valid";
    public static final String USER_NOT_FOUND = "The username referred in the operation doesn’t exist in registered accounts";

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public LoginResource() { } // Nothing to be done here
	
    
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLoginV2(LoginRequest loginRequest,
                              @Context HttpServletRequest request,
                              @Context HttpHeaders headers) {
    	LoginData data = loginRequest.input;
        LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.username);

        KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
        Key userKey = userKeyFactory.newKey(data.username);
        
        Key ctrsKey = datastore.newKeyFactory()
            .addAncestor(PathElement.of("User", data.username))
            .setKind("UserStats")
            .newKey("counters");

        Key logKey = datastore.allocateId(
            datastore.newKeyFactory()
                .addAncestor(PathElement.of("User", data.username))
                .setKind("UserLog").newKey());

        
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
            	JsonObject error = new JsonObject();
            	error.addProperty("status", "9902");
            	error.addProperty("data", USER_NOT_FOUND);
                return Response.ok(g.toJson(error)).build();
            }

            Entity stats = txn.get(ctrsKey);
            if (stats == null) {
                stats = Entity.newBuilder(ctrsKey)
                    .set("user_stats_logins", 0L)
                    .set("user_stats_failed", 0L)
                    .set("user_first_login", Timestamp.now())
                    .build();
            }

            String hashedPWD = (String) user.getString(USER_PWD);
            
            if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
                
                String cityLatLong = headers.getHeaderString("X-AppEngine-CityLatLong");
                
                Entity log = Entity.newBuilder(logKey)
                    .set("user_login_ip", request.getRemoteAddr())
                    .set("user_login_host", request.getRemoteHost())
                    .set("user_login_latlon", cityLatLong != null 
                        ? StringValue.newBuilder(cityLatLong).setExcludeFromIndexes(true).build() 
                        : StringValue.newBuilder("").setExcludeFromIndexes(true).build())
                    .set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
                    .set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
                    .set("user_login_time", Timestamp.now())
                    .build();

                Entity ustats = Entity.newBuilder(ctrsKey)
                    .set("user_stats_logins", stats.getLong("user_stats_logins") + 1)
                    .set("user_stats_failed", 0L) // Reset failures on success
                    .set("user_first_login", stats.getTimestamp("user_first_login"))
                    .set("user_last_login", Timestamp.now())
                    .build();

                txn.put(log, ustats);
                

                AuthToken authToken = new AuthToken(data.username, user.getString(USER_ROLE));
                
                
                KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");
                Key tokenKey = tokenKeyFactory.newKey(authToken.tokenId);
                
                Entity tokenEnt = Entity.newBuilder(tokenKey)
                		.set("token_id", authToken.tokenId)
                		.set("user_name", data.username)
                        .set("role", authToken.role)
                        .set("issued_at", authToken.issuedAt)
                        .set("expired_at", authToken.expiredAt)
                        .build();
                txn.put(tokenEnt);
                txn.commit();
                
                
                JsonObject responseData = new JsonObject();
                JsonObject responseToken = new JsonObject();
                
                responseToken.addProperty("tokenId", authToken.tokenId);
                responseToken.addProperty("username", data.username);
                responseToken.addProperty("role", authToken.role);
                responseToken.addProperty("issuedAt", authToken.issuedAt);
                responseToken.addProperty("expiredAt", authToken.expiredAt);
                
                responseData.add("token", responseToken);
                
                JsonObject succResponse = new JsonObject();
                succResponse.addProperty("status", "success");
                succResponse.add("data", responseData);
                
                return Response.ok(g.toJson(succResponse)).build();
                
            } else {
                Entity ustats = Entity.newBuilder(ctrsKey)
                    .set("user_stats_logins", stats.getLong("user_stats_logins"))
                    .set("user_stats_failed", stats.getLong("user_stats_failed") + 1L)
                    .set("user_first_login", stats.getTimestamp("user_first_login"))
                    .set("user_last_attempt", Timestamp.now())
                    .build();

                txn.put(ustats);
                txn.commit();
                
                JsonObject error = new JsonObject();
            	error.addProperty("status", "9900");
            	error.addProperty("data", INVALID_CREDENTIALS);
                return Response.ok(g.toJson(error)).build();
                
            }
        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            LOG.severe(e.getMessage());
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}
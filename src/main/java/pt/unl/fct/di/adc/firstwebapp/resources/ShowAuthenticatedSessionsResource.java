package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.List;


import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.gson.Gson;
import com.google.gson.JsonObject;


import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.NoInputRequest;


@Path("/showauthsessions")
public class ShowAuthenticatedSessionsResource {
	
	public static final String INVALID_TOKEN = "The operation is called with an invalid token (wrong format for example)";
    public static final String TOKEN_EXPIRED = "The operation is called with a token that is expired";
    public static final String UNAUTHORIZED = "The operation is not allowed for the user role";
   public static final String FORBIDDEN = "The operation generated a forbidden error by other reason";

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();
    
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ShowAuthSessions(NoInputRequest request ){
    	AuthToken token = request.token;
    	
    	
    	KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");
    	Key tokenKey = tokenKeyFactory.newKey(token.tokenId);

    	Entity tokenEnt = datastore.get(tokenKey);

    	
    	if (tokenEnt == null) {
    	    JsonObject error = new JsonObject();
    	    error.addProperty("status", "9903");
    	    error.addProperty("data", INVALID_TOKEN);
    	    return Response.ok(g.toJson(error)).build();
    	}
    	
    	if (!token.isValid()) {
        	JsonObject error = new JsonObject();
            error.addProperty("status", "9903");
            error.addProperty("data", INVALID_TOKEN);
            
            return Response.ok(g.toJson(error)).build();
        }
        
        if(System.currentTimeMillis() > token.expiredAt) {
        	JsonObject error = new JsonObject();
            error.addProperty("status", "9904");
            error.addProperty("data", TOKEN_EXPIRED);
            
            return Response.ok(g.toJson(error)).build();
        }
        if (!token.role.equals("ADMIN")) {
        	JsonObject error = new JsonObject();
            error.addProperty("status", "9905");
            error.addProperty("data", UNAUTHORIZED);
        }
        
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .build();

            QueryResults<Entity> results = datastore.run(query);

            List<JsonObject> tokens = new ArrayList<>();
            results.forEachRemaining(entity -> {
            	if(entity.getLong("expired_at") > System.currentTimeMillis()) {
	                JsonObject currentToken = new JsonObject();
	                currentToken.addProperty("tokenId", entity.getString("token_id"));
	                currentToken.addProperty("username", entity.getString("user_name"));
	                currentToken.addProperty("role", entity.getString("role"));
	                currentToken.addProperty("expiredAt", entity.getLong("expired_at"));
	                tokens.add(currentToken);
            	}
            });
            
            JsonObject responseData = new JsonObject();
            responseData.add("sessions", g.toJsonTree(tokens));
            
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("data", responseData);
            
            return Response.ok(g.toJson(responseJson)).build();      	
    }
}

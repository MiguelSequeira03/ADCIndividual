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

@Path("/showusers")
public class ShowUsersResource {

	public static final String USER_ROLE = "user_role";
    public static final String INVALID_TOKEN = "The operation is called with an invalid token (wrong format for example)";
    public static final String TOKEN_EXPIRED = "The operation is called with a token that is expired";
    public static final String UNAUTHORIZED = "The operation is not allowed for the user role";

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();
    
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showUsers(NoInputRequest resquest) {
    	AuthToken token = resquest.token;
        
    	
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
        
        if(token.role.equals("USER")) {
        	JsonObject error = new JsonObject();
            error.addProperty("status", "9905");
            error.addProperty("data", UNAUTHORIZED);
            
            return Response.ok(g.toJson(error)).build();
        }
        
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .build();

            QueryResults<Entity> results = datastore.run(query);

            List<JsonObject> users = new ArrayList<>();
            results.forEachRemaining(entity -> {
                JsonObject user = new JsonObject();
                user.addProperty("username", entity.getString("user_name"));
                user.addProperty("role", entity.getString(USER_ROLE));
                users.add(user);
            });
            
            JsonObject responseData = new JsonObject();
            responseData.add("users", g.toJsonTree(users));
            
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("data", responseData);
            
            return Response.ok(g.toJson(responseJson)).build();    
        
    }

}

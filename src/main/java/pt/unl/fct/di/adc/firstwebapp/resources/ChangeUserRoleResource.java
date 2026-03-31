package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.changeuserrole.ChangeRoleInput;
import pt.unl.fct.di.adc.firstwebapp.util.changeuserrole.ChangeRoleRequest;

@Path("/changeuserrole")
public class ChangeUserRoleResource {


    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());     public static final String USER_NOT_FOUND = "The username referred in the operation doesn’t exist in registered accounts";
    public static final String INVALID_TOKEN = "The operation is called with an invalid token (wrong format for example)";
    public static final String TOKEN_EXPIRED = "The operation is called with a token that is expired";
    public static final String UNAUTHORIZED = "The operation is not allowed for the user role";
    public static final String FORBIDDEN = "The operation generated a forbidden error by other reason";
    public static final String ROLE_UPDATED= "Role updated successfully";

    
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();
    
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ChangeUserRole(ChangeRoleRequest request) {
    	ChangeRoleInput input = request.input;
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
    	
      if(!token.role.equals("ADMIN") ) {
       	JsonObject error = new JsonObject();
           error.addProperty("status", "9905");
           error.addProperty("data", UNAUTHORIZED);
           return Response.ok(g.toJson(error)).build();
       }
      
      KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
      Key userKey = userKeyFactory.newKey(input.username);
      
      Transaction txn = datastore.newTransaction();
      try {
          Entity user = txn.get(userKey);
          if (user == null) {
          	JsonObject error = new JsonObject();
          	error.addProperty("status", "9902");
          	error.addProperty("data", USER_NOT_FOUND);
              return Response.ok(g.toJson(error)).build();
          }
          Entity updatedUser = Entity.newBuilder(user)
        	        .set("user_role", input.newRole)
        	        .build();

          txn.put(updatedUser);
          txn.commit();

          JsonObject message = new JsonObject();
          message.addProperty("message", ROLE_UPDATED);
          
          
          JsonObject responseJson = new JsonObject();
          responseJson.addProperty("status", "success");
          responseJson.add("data", message);

          return Response.ok(g.toJson(responseJson)).build();    

    	
      } catch (Exception e) {
          if (txn.isActive()) txn.rollback();
          LOG.severe(e.getMessage());
          return Response.status(Status.INTERNAL_SERVER_ERROR).build();
      } finally {
          if (txn.isActive()) {
              txn.rollback();
          }
      } 
    	
    }
}

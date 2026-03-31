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
import pt.unl.fct.di.adc.firstwebapp.util.modifyaccount.ModifyAccountInput;
import pt.unl.fct.di.adc.firstwebapp.util.modifyaccount.ModifyAccountRequest;
import pt.unl.fct.di.adc.firstwebapp.util.modifyaccount.ModifyAttributes;

@Path("/modaccount")
public class ModifyResource {
	
	public static final String USER_ROLE = "user_role";
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    public static final String USER_NOT_FOUND = "The username referred in the operation doesn’t exist in registered accounts";
    public static final String INVALID_TOKEN = "The operation is called with an invalid token (wrong format for example)";
    public static final String TOKEN_EXPIRED = "The operation is called with a token that is expired";
    public static final String UNAUTHORIZED = "The operation is not allowed for the user role";
    public static final String UPDATED = "Updated Successfully";

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();
    
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ModifyAccount(ModifyAccountRequest request) {
    	ModifyAccountInput input = request.input;
    	ModifyAttributes attributes = input.attributes;
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
             if (token.role.equals("USER") && !token.username.equals(input.username)) {
            	 JsonObject error = new JsonObject();
                 error.addProperty("status", "9905");
                 error.addProperty("data", UNAUTHORIZED);
                 
                 return Response.ok(g.toJson(error)).build();
             }
             if(token.role.equals("BOFFICER") && user.getString(USER_ROLE).equals("ADMIN")) {
            	 JsonObject error = new JsonObject();
                 error.addProperty("status", "9905");
                 error.addProperty("data", UNAUTHORIZED);
                 
                 return Response.ok(g.toJson(error)).build(); 
             }
             if(token.role.equals("BOFFICER") && user.getString(USER_ROLE).equals("BOFFICER") && !token.username.equals(input.username)) {
            	 JsonObject error = new JsonObject();
                 error.addProperty("status", "9905");
                 error.addProperty("data", UNAUTHORIZED);
                 
                 return Response.ok(g.toJson(error)).build(); 
             }
             
             if(attributes.phone.equals("")) {
            	 Entity updatedUser = Entity.newBuilder(user)
                         .set("user_address", attributes.address)
                         .build();
                 
                 txn.put(updatedUser);
                 txn.commit();
             }
             
             else if(attributes.address.equals("")) {
            	 Entity updatedUser = Entity.newBuilder(user)
                         .set("user_phone", attributes.phone)
                         .build();
                 
                 txn.put(updatedUser);
                 txn.commit();
             }
             
             else {
             
             Entity updatedUser = Entity.newBuilder(user)
                     .set("user_phone", attributes.phone)
                     .set("user_address", attributes.address)
                     .build();
             
             txn.put(updatedUser);
             txn.commit();
             }
             
             JsonObject message = new JsonObject();
             message.addProperty("message", UPDATED);
             
             
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

package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.cloud.datastore.*;
import com.google.cloud.Timestamp;
import org.apache.commons.codec.digest.DigestUtils;

import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterRequest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Path("/createaccount")
public class RegisterResource {
    public static final String USER_ALREADY_EXISTS = "Error in creating an account because the username already exists";
    public static final String INVALID_INPUT = "The call is using input data not following the correct specification";
    public static final String FORBIDDEN = "The operation generated a forbidden error by other reason";

    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUserV3(RegisterRequest request) {
        RegisterData data = request.input;
        LOG.fine("Attempt to register user: " + data.username);

        if (!data.validRegistration()) {
            JsonObject error = new JsonObject();
            error.addProperty("status", "9906");
            error.addProperty("data", INVALID_INPUT);
            
            return Response.ok(g.toJson(error)).build();
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
            Entity user = txn.get(userKey);
            
            if (user != null) {
                txn.rollback();
                JsonObject error = new JsonObject();
                error.addProperty("status", "9901");
                error.addProperty("data", USER_ALREADY_EXISTS);
                
                return Response.ok(g.toJson(error)).build();
            } 
                
            if (!(data.role.equals("ADMIN") || data.role.equals("BOFFICER") || data.role.equals("USER"))) {
            	JsonObject error = new JsonObject();
                error.addProperty("status", "9907");
                error.addProperty("data", FORBIDDEN);
                
                return Response.ok(g.toJson(error)).build();
            }
            
            
            user = Entity.newBuilder(userKey)
                    .set("user_name", data.username)
                    .set("user_pwd", DigestUtils.sha512Hex(data.password))
                    .set("user_phone", data.phone)
                    .set("user_address", data.address) // Fixed potential typo 'addres' -> 'address'
                    .set("user_role", data.role)
                    .set("user_creation_time", Timestamp.now()).build();
                
                txn.put(user);
                txn.commit();
                
                LOG.info("User registered: " + data.username);

                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("status", "success");
                
                JsonObject userData = new JsonObject();
                userData.addProperty("username", data.username);
                userData.addProperty("role", data.role);
                
                responseJson.add("data", userData);
                
                return Response.ok(g.toJson(responseJson)).build();
                
        } catch (DatastoreException e) {
            LOG.severe("Datastore error: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}
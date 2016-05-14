package cotton.storagecomponents;

import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.services.CloudContext;
import cotton.services.Service;
import cotton.services.ServiceFactory;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author mats
 * @author Gunnlaugur
 */
public class DatabaseService implements Service{

    @Override
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
        MongoDBConnector wrapper = MongoDBConnector.getInstance();
        String convertToJson = new String(data);
        JSONObject databaseInput = new JSONObject(convertToJson);
        boolean retCheck = false;
        JSONObject retVal = null;
        JSONObject returnValue = new JSONObject();

        switch (databaseInput.remove("command").toString()) {
            case "getDataFromDatabase":
                retVal = wrapper.getDataFromDatabase(databaseInput);
                if(retVal != null){
                    returnValue.put("type", "data");
                    returnValue.put("content", retVal);
                    String send = returnValue.toString();
                    byte[] message = send.getBytes(StandardCharsets.UTF_8);
                    return message;
                }else{
                    String send = "Data could not be retrieved, Check Authorise level";
                    returnValue.put("type", "ERROR");
                    returnValue.put("content", send);
                    String m = returnValue.toString();
                    byte[] message = m.getBytes(StandardCharsets.UTF_8);
                    return message;
                }
            case "removeDataFromDatabase":
                retCheck = wrapper.removeDataFromDatabase(databaseInput);
                if(retCheck == false){
                    String send = "Data could not be removed, Check Authorise level";
                    returnValue.put("type", "ERROR");
                    returnValue.put("content", send);
                    String m = returnValue.toString();
                    byte[] message = m.getBytes(StandardCharsets.UTF_8);
                    return message;
                }
                break;
            case "removeUserFromDatabase":
                retCheck = wrapper.removeUserFromDatabase(databaseInput);
                if(retCheck == false){
                    String send = "User could not be removed, Check Authorise level";
                    returnValue.put("type", "ERROR");
                    returnValue.put("content", send);
                    String m = returnValue.toString();
                    byte[] message = m.getBytes(StandardCharsets.UTF_8);
                    return message;
                }
                break;
            case "authoriseRequest":
                retCheck = wrapper.authoriseRequest(databaseInput);
                if(retCheck == false){
                    String send = "Data could not be added";
                    returnValue.put("type", "ERROR");
                    returnValue.put("content", send);
                    String m = returnValue.toString();
                    byte[] message = m.getBytes(StandardCharsets.UTF_8);
                    return message;
                }
                break;
            case "addUserInDatabase":
                retCheck = wrapper.addUserInDatabase(databaseInput);
                if(retCheck == false){
                    String send = "User could not be added";
                    returnValue.put("type", "ERROR");
                    returnValue.put("content", send);
                    String m = returnValue.toString();
                    byte[] message = m.getBytes(StandardCharsets.UTF_8);
                    return message;
                }
                break;
            case "authoriseUser":
                byte[] returnedToken = null;
                try {
                    returnedToken = wrapper.authoriseUser(databaseInput);
                } catch (IllegalBlockSizeException e) {
                    //TODO log error
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    //TODO log error
                    e.printStackTrace();
                } catch (IOException e) {
                    //TODO log error
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    //TODO log error
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    //TODO log error
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    //TODO log error
                    e.printStackTrace();
                }

                if(returnedToken != null){
                    returnValue.put("type", "token");
                    returnValue.put("content", returnedToken);
                    String send = returnValue.toString();
                    byte[] message = send.getBytes(StandardCharsets.UTF_8);
                    return message;
                }else{
                    String send = "User could not be authorised";
                    returnValue.put("type", "ERROR");
                    returnValue.put("content", send);
                    String m = returnValue.toString();
                    byte[] message = send.getBytes(StandardCharsets.UTF_8);
                    return message;
                }
            default:
                break;
        }

        returnValue.put("type", "boolean");
        returnValue.put("content", true);
        String m = returnValue.toString();
        byte[] message = m.getBytes(StandardCharsets.UTF_8);
        return message;
    }

    @Override
    public ServiceFactory loadFactory (){
        return new Factory();
    }
    
    public class Factory implements ServiceFactory{

        @Override
        public Service newService() {

            return new DatabaseService();
        }
    }
}

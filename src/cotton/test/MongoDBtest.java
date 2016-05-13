package cotton.test;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import cotton.services.Service;
import cotton.storagecomponents.DatabaseService;
import cotton.storagecomponents.MongoDBConnector;
import org.bson.Document;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

/**
 * @author Mats
 * @author Gunnlaugur
 */
public class MongoDBtest {

    private MongoClient client = null;
    private MongoDatabase db =  null;

    public MongoDBtest (){

    }

    @Test
    public  void  dbGetDataServiceTest (){
        addData();
        Service s = new DatabaseService();
        JSONObject dbInput = new JSONObject();

        dbInput.put("command", "getDataFromDatabase");
        dbInput.put("data", "data test one");
        dbInput.put("accessLevel", 1);

        String m = dbInput.toString();
        byte[] message = m.getBytes(StandardCharsets.UTF_8);

        byte[] receivedByteArray = s.execute(null, null, message, null);
        String convertToJson = new String(receivedByteArray);
        JSONObject databaseInput = new JSONObject(convertToJson);

        System.out.println("\n" +databaseInput.toString() +"\n");
        assertTrue("data".equals(databaseInput.get("type")));
    }

    @Test
    public  void  dbRemoveDataServiceTest (){
        addData();
        System.out.println("\n\n\n\nAdded ,data test one, to be removed:");
        printDB("requestTable");

        Service s = new DatabaseService();
        JSONObject dbInput = new JSONObject();

        dbInput.put("command", "removeDataFromDatabase");
        dbInput.put("data", "data test one");
        dbInput.put("accessLevel", 1);

        String m = dbInput.toString();
        byte[] message = m.getBytes(StandardCharsets.UTF_8);

        byte[] receivedByteArray = s.execute(null, null, message, null);
        String convertToJson = new String(receivedByteArray);
        JSONObject databaseInput = new JSONObject(convertToJson);

        printDB("requestTable");
        assertTrue(true == (Boolean)databaseInput.get("content"));
    }

    @Test
    public  void  dbRemoveUserServiceTest (){

        addUser();
        System.out.println("\n\n\n\nAdded ,test user, to be removed:");
        printDB("userTable");

        Service s = new DatabaseService();
        JSONObject dbInput = new JSONObject();

        dbInput.put("command", "removeUserFromDatabase");
        dbInput.put("username", "test user");
        dbInput.put("accessLevel", 73);

        String m = dbInput.toString();
        byte[] message = m.getBytes(StandardCharsets.UTF_8);

        byte[] receivedByteArray = s.execute(null, null, message, null);
        String convertToJson = new String(receivedByteArray);
        JSONObject databaseInput = new JSONObject(convertToJson);

        printDB("userTable");
        assertTrue(true == (Boolean)databaseInput.get("content"));
    }

    @Test
    public  void  dbAddDataServiceTest(){
        Service s = new DatabaseService();
        JSONObject dbInput = new JSONObject();

        dbInput.put("command", "authoriseRequest");
        dbInput.put("data", "data test one");
        dbInput.put("accessLevel", 1);

        String m = dbInput.toString();
        byte[] message = m.getBytes(StandardCharsets.UTF_8);

        byte[] receivedByteArray = s.execute(null, null, message, null);
        String convertToJson = new String(receivedByteArray);
        JSONObject databaseInput = new JSONObject(convertToJson);

        printDB("requestTable");
        assertTrue(true == (Boolean)databaseInput.get("content"));
    }

    @Test
    public  void  dbAddUserServiceTest(){
        Service s = new DatabaseService();
        JSONObject dbInput = new JSONObject();

        dbInput.put("command", "addUserInDatabase");
        dbInput.put("username", "Test user two");
        dbInput.put("accessLevel", 95);

        String m = dbInput.toString();
        byte[] message = m.getBytes(StandardCharsets.UTF_8);

        byte[] receivedByteArray = s.execute(null, null, message, null);
        String convertToJson = new String(receivedByteArray);
        JSONObject databaseInput = new JSONObject(convertToJson);

        printDB("userTable");
        assertTrue(true == (Boolean)databaseInput.get("content"));
    }

    private void addData() {
        MongoDBConnector wrapper = MongoDBConnector.getInstance();

        JSONObject inData = new JSONObject();

        inData.put("data", "data test one");
        inData.put("accessLevel", 1);

        wrapper.authoriseRequest(inData);
    }

    private void addUser() {
        MongoDBConnector wrapper = MongoDBConnector.getInstance();

        JSONObject inData = new JSONObject();

        inData.put("username", "Test user");
        inData.put("accessLevel", 73);

        wrapper.addUserInDatabase(inData);
    }

    //prints out the database table recived in the method
    private void printDB(String table){
        client = new MongoClient("localhost", 27017);
        db = client.getDatabase("cotton");

        FindIterable<Document> request = db.getCollection(table).find();
        System.out.print("\nDB printout: \n");
        for(Document d: request){
            System.out.print("\n" +d.toString() +",\n ");
        }

    }

}

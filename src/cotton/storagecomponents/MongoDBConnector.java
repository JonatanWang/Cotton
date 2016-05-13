/*

Copyright (c) 2016, Gunnlaugur Juliusson, Jonathan Kåhre, Magnus Lundmark,
Mats Levin, Tony Tran
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 * Neither the name of Cotton Production Team nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

 */


package cotton.storagecomponents;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.util.JSON;
import org.apache.log4j.BasicConfigurator;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Class that connects to the mongo database and handles the data insertion and retrival.
 *
 *@author Mats
 *@author Jonathan
 */
public class MongoDBConnector implements DatabaseConnector {

    private MongoClient client = null;
    private static MongoDBConnector instance = null;
    private DateFormat dates = null;
    private MongoDatabase db =  null;


    /**
     * Starts the connection to the database
     */
    public MongoDBConnector() {
        BasicConfigurator.configure();
        client = new MongoClient("localhost" , 27017);
        db = client.getDatabase("cotton");
        dates = new SimpleDateFormat("yyyy-MM-dd");

        //add tables

    }

    /**
     * Returns data from the database.
     *
     * @param searchKeys JSONObject containing the information regarding what data the return needs to contain.
     * @return  returns an JSONObject containing desired data.
     */
    @Override
    public synchronized JSONObject getDataFromDatabase (JSONObject searchKeys){
        JSONArray retArray = new JSONArray();
        JSONObject returnValue = new JSONObject();

        BasicDBObject dbObject = (BasicDBObject) JSON.parse(searchKeys.toString());

        FindIterable<Document> request = db.getCollection("requestTable").find(dbObject);
        for(Document d: request){
            retArray.put(new JSONObject(d.toJson()));
        }

        returnValue.put("dataArray", retArray);
        return returnValue;
    }

    /**
     * Removes the desired data from the database
     *
     * @param removeKey JSONObject containing information regarding what data that needs to be removed
     * @return true or false depending on success
     */
    @Override
    public synchronized boolean removeDataFromDatabase (JSONObject removeKey){
        MongoCollection<Document> collection = db.getCollection("requestTable");

        BasicDBObject dbObject = (BasicDBObject) JSON.parse(removeKey.toString());

        DeleteResult removeReturn = collection.deleteOne(dbObject);
        return removeReturn.wasAcknowledged();
    }



    /**
     * Removes user from database
     *
     * @param removeKey JSONObject containing data about the user that is to be removed
     * @return Returns true if successful and false if it failed.
     */
    @Override
    public synchronized boolean removeUserFromDatabase (JSONObject removeKey){
        MongoCollection<Document> collection = db.getCollection("userTable");

        BasicDBObject dbObject = (BasicDBObject) JSON.parse(removeKey.toString());

        DeleteResult removeReturn = collection.deleteOne(dbObject);
        return removeReturn.wasAcknowledged();
    }

    /**
     * Decides if the received request is valid and then save the request in the database
     *
     * @param newData Conatins information regarding the desired request and the user
     * @return true or false depending on success
     */
    @Override
    public synchronized boolean authoriseRequest (JSONObject newData){
        boolean success = true;
        MongoCollection<Document> collection = db.getCollection("requestTable");
        Document dataInput = Document.parse(newData.toString());

        try {
            collection.insertOne(dataInput);
        }catch (MongoWriteException e){
            //TODO logg error
            success = false;
        }
        return success;
    }

    /**
     * Adds user into the database.
     *
     * @param newUserData Contains user information like username, and password.
     * @return returns true od false depending on success.
     */
    @Override
    public synchronized boolean addUserInDatabase (JSONObject newUserData){
        boolean success = true;
        MongoCollection<Document> collection = db.getCollection("userTable");

        Document userInput = Document.parse(newUserData.toString());

        try {
            collection.insertOne(userInput);
        }catch (MongoWriteException e){
            //TODO logg error
            success = false;
        }
        return success;
    }

    /**
     * Returns token to user if loggin credentials are accurate.
     *
     * @param newData JSONObject containing login data.
     * @return Token and other login information.
     */
    @Override
    public synchronized byte[] authoriseUser (JSONObject newData){
        //TODO this stuff
        return null;
    }


    /**
     * Returns a mongodb object
     *
     * @return mongodb object
     */
    public static MongoDBConnector getInstance (){
        if(instance == null){
            instance = new MongoDBConnector();
        }
        return instance;
    }
}

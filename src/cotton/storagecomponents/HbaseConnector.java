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
/*
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.json.JSONObject;
*/

public class HbaseConnector{
    /*  private Configuration conf;
    private static HbaseConnector instance;

    private HbaseConnector (){

    }

    public HbaseConnector (String address, int port ){
        conf = HBaseConfiguration.create();


        Connection connection = ConnectionFactory.createConnection(conf);
        //TODO use address and port
    }

    public synchronized JSONObject getDataFromDatabase (String userName, String tableName, JSONObject keyData){
        throw new UnsupportedOperationException("Not supported yet.");
        HTable table = new HTable(conf, tableName);
        JSONObject retData = new JSONObject();

        Get Data = new Get(Bytes.toBytes(userName));

        try{
            Result returned = table.get(Data);
        }catch(IOException e){
            System.out.println("getDataFromDatabase caught exception when getting data: " +e.getMessage());
        }

        //TODO Fill json with data, possible limit get to desiered rows
        retData.put("", "");

        try{
            table.close();
        }catch(IOException e){
            System.out.println("getDataFromDatabase caught exception: " +e.getMessage());
        }

        return retData;

    }

    public synchronized boolean removeDataFromDatabase (String userName, String tableName, JSONObject removeKey){
        throw new UnsupportedOperationException("Not supported yet.");
        HTable table = new HTable(conf, tableName);
        //Delete removeData = new Delete(removeKey.remove("user"));
        Delete removeData = new Delete(Bytes.toBytes(userName));
        String colFam = "ColumnFamily";
        String col = (String) removeKey.remove("Column");

        if(colFam != null){
            if(col != null){
                removeData.deleteColumn(Bytes.toBytes(colFam), Bytes.toBytes(col));
            } else{
                removeData.deleteFamily(Bytes.toBytes(colFam));
            }
        }

        try{
            table.delete(removeData);
            table.close();
        }catch(IOException e){
            return false;
        }

        return true;
    }

    public synchronized void editDataInDatabase (String userName, String tableName, JSONObject... newData)throws IOException{
        throw new UnsupportedOperationException("Not supported yet.");

        HTable table = new HTable(conf, tableName);
        ArrayList<Put> queries = new ArrayList<>();

        for(JSONObject inc: newData){
            String columnFamily = "ColumnFamily";
            String row = (String) inc.remove("user");
            Put query = new Put(Bytes.toBytes(row));
            for(Object key: inc.keySet()){
                query.add(Bytes.toBytes(columnFamily),
                          Bytes.toBytes(key),
                          Bytes.toBytes(incData.get(key))
                          );
            }
            queries.add(query);
        }

        try{
            table.put(queries);
            table.close();
        }catch(IOException e){
            System.out.println("editDataInDatabase caught exception: " +e.getMessage());
        }
    }

    public synchronized JSONObject getUserFromDatabase (JSONObject newData){
        throw new UnsupportedOperationException("Not supported yet.");

        //TODO remove

        HTable table = new HTable(conf, "users");
        String row = (String)newData.get("username");
        Get user = new Get(Bytes.toBytes(row));
        JSONObject retData = new JSONObject();

        Result userData = table.get(user);

        //TODO create json to return containing user data
        retData.put(row);
        retData.put();

        try{
            table.close();
        }catch(IOException e){
            System.out.println("getUserFromDatabase caught exception: " +e.getMessage());
        }

        return retData;

    }

    public synchronized JSONObject addUsersInDatabase (JSONObject newData){
        throw new UnsupportedOperationException("Not supported yet.");

        HTable table = new HTable(conf, "users");
        String row = (String) newData.remove("user");
        Put user = new Put(Bytes.toBytes(row));
        String columnFamily = "ColFam";
        UUID userID = UUID.Random();
        String idString = userID.toString();
        String hashedPass = newData.getString("hashPass");
        String username = newData.getString("username");

        user.add(Bytes.toBytes(columnFamily),
                 Bytes.toBytes(username),
                 Bytes.toBytes(idString),
                 Bytes.toBytes(hashedPass)
                 );

        try{
            table.put(user);
            table.close();
        }catch(IOException e){
            System.out.println("addUsersInDatabase caught exception: " +e.getMessage());
        }

    }

    public synchronized JSONObject authoriseUser (JSONObject newData){
        throw new UnsupportedOperationException("Not supported yet.");

        HTable table = new HTable(conf, "users");
        String row = (String) newData.get("username");
        Get get = new Get(Bytes.toBytes(row));
        Result r = table.get(get);
        if(Bytes.toString(r.getValue(Bytes.toBytes("password"))).equals(newData.get("password"))){
            //TODO: Return accept
        }else{
            //TODO: Return accept
        }


    }

    public static HbaseConnector getInstance (){
        if(instance == null){
            instance = new HbaseConnector();
        }
        return instance;
    }

    public synchronized void shutDown(){
        connection.close();
        }*/
}

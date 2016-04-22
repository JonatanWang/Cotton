
package cotton.servicediscovery;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.io.FileNotFoundException;
/**
 *
 * @author Magnus
 */
public class GlobalDiscoveryDNS {

    public SocketAddress[] addressArray = null;
    
    public GlobalDiscoveryDNS()throws SecurityException{
        BufferedReader bufferedReader = null;
        try{
            FileReader fileReader = new FileReader("dnsconfig");
            bufferedReader = new BufferedReader(fileReader);
            readFile(bufferedReader);
        }catch(FileNotFoundException e){
            System.out.println("FileNotFoundException in global DNS");

        }catch(IOException e){
            System.out.println("IOException global");
        }finally{
            try{
                if(bufferedReader != null) 
                    bufferedReader.close();
            }catch(IOException e){}
        }

        //TODO: read from config and get the global ServiceDiscovery SocketAddress
    }

    private void readFile(BufferedReader bufferedReader)throws IOException{
        ArrayList<SocketAddress> tmpAddressArray = new ArrayList<SocketAddress>();
        String currentLine = null;
        SocketAddress addr = null;
        do{
            addr = null;
            try{
                currentLine = bufferedReader.readLine();
                if(currentLine != null)
                    addr = parseAddress(currentLine);
                if(addr != null)
                    tmpAddressArray.add(addr);
                
            }catch(NumberFormatException e){
                System.out.println("number format exception in readFile");

            }
        }while(currentLine != null);
        this.addressArray = tmpAddressArray.toArray(new SocketAddress[tmpAddressArray.size()]);
 /*       this.addressArray =  new SocketAddress[tmpAddressArray.size()];
        for(int i =0; i<tmpAddressArray.size(); i++){
            this.addressArray[i] = tmpAddressArray.get(i);
            
        }*/
      
    }

    private SocketAddress parseAddress(String line)throws SecurityException{
        String[] addressInfo = line.split("\\s+");
        int port = 0;
        String ipaddress = null;
        SocketAddress addr = null;
        if(addressInfo.length <= 1)
            return null;
        try{
            ipaddress = addressInfo[0];
            port = Integer.parseInt(addressInfo[1]);
            addr = new InetSocketAddress(ipaddress,port);
            System.out.println("testing" + ((InetSocketAddress)addr).toString());

        }catch(NumberFormatException e){
            System.out.println("Global DNS format error (port)");
            e.printStackTrace();
        }catch(IllegalArgumentException e){
            System.out.println("Global DNS illegal ArgumentException");


        }
        
        return addr;
    }

    public void setGlobalDiscoveryAddress(SocketAddress[] addresses) {
        this.addressArray = addresses;
    }
    
    public SocketAddress[] getGlobalDiscoveryAddress() {
        return this.addressArray;
    }
    
}

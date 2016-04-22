package cotton.cottonclientmockup;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.os.ResultReceiver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import cotton.example.ImageManipulationPacket;
import cotton.network.DefaultNetworkPacket;
import cotton.network.DefaultServiceConnection;
import cotton.network.DummyServiceChain;
import cotton.network.NetworkPacket;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;

/**
 *
 * @author Jonathan
 * @author Gunnlaugur
 */
public class NetworkService extends IntentService {
    public NetworkService() {
        super("NetworkService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String filename = intent.getExtras().getString("filename");
        ResultReceiver r = intent.getExtras().getParcelable("reciever");

        System.out.println("____NetworkService got intent with filename: " + filename);

        ServiceConnection from = new DefaultServiceConnection();

        ServiceChain path = new DummyServiceChain().into("ImageService").into("FileWriter");

        Bitmap img = BitmapFactory.decodeFile(filename);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        ImageManipulationPacket out = new ImageManipulationPacket(stream.toByteArray());

        NetworkPacket packet = new DefaultNetworkPacket(out, path, from, PathType.SERVICE, true);

        Socket s = new Socket();

        try {
            s = new Socket("130.229.184.228", 3333);

            new ObjectOutputStream(s.getOutputStream()).writeObject(packet);

            System.out.println("Finished sending image!");

            ObjectInputStream resultStream = new ObjectInputStream(s.getInputStream());

            try {
                NetworkPacket result = (NetworkPacket)resultStream.readObject();
                byte[] data = (byte[])result.getData();
                Bitmap editedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                String savename = filename.substring(0, filename.length()-4)+"edited.png";
                FileOutputStream saveImage = new FileOutputStream(savename);
                galleryAddPic(savename);
                editedImage.compress(Bitmap.CompressFormat.PNG, 100, saveImage);
                System.out.println("Recieved manipulated image, saving into: "+savename);
                Bundle activityReturn = new Bundle();
                activityReturn.putString("filename", savename);
                r.send(111, activityReturn);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] byteArrayFromInputStream(InputStream input){
        ByteArrayOutputStream o = new ByteArrayOutputStream();

        try{
            while(input.available()>0)
                o.write((int) input.read());
        }catch (Throwable ignore){}

        return o.toByteArray();
    }

    private InputStream serializableToInputStream(Serializable data){
        InputStream in = null;
        try{
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

            objectStream.writeObject(data);
            objectStream.flush();
            objectStream.close();

            in = new ByteArrayInputStream(byteStream.toByteArray());
        }catch(IOException e){
            e.printStackTrace();
        }
        return in;
    }

    private void galleryAddPic(String path) {
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
        sendBroadcast(mediaScanIntent);
    }

}

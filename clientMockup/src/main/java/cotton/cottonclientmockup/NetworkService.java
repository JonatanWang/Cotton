package cotton.cottonclientmockup;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.os.ResultReceiver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import cotton.network.DefaultServiceConnection;
import cotton.network.DummyServiceChain;
import cotton.network.PathType;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.network.TransportPacket;

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
        try {
            from.setAddress(new InetSocketAddress(InetAddress.getLocalHost(), 3333));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ServiceChain path = new DummyServiceChain().into("ImageService").into("FileWriter");

        byte[] image = getBytesFromFile(new File(filename));

        TransportPacket.Packet packet = buildTransportPacket(image, path, from, PathType.SERVICE);

        //System.out.println("Packet size: "+packet.getSerializedSize());
        image = null;
        path = null;
        from = null;

        Socket s = new Socket();

        try {
            String ip = "generic ip//REPLACE";
            s = new Socket(ip, 3333);

            packet.writeDelimitedTo(s.getOutputStream());

            packet = null;

            System.out.println("Finished sending image!");

            TransportPacket.Packet result = TransportPacket.Packet.parseFrom(s.getInputStream());
            byte[] data = result.getData().toByteArray();
            Bitmap editedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            String savename = filename.substring(0, filename.length()-4)+"edited.jpg";
            FileOutputStream saveImage = new FileOutputStream(savename);
            galleryAddPic(savename);
            editedImage.compress(Bitmap.CompressFormat.JPEG, 100, saveImage);
            System.out.println("Recieved manipulated image, saving into: "+savename);
            Bundle activityReturn = new Bundle();
            activityReturn.putString("filename", savename);
            r.send(111, activityReturn);
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

    public byte[] getBytesFromFile(File file) {
        byte[] bytes = null;
        try {

            InputStream is = new FileInputStream(file);
            long length = file.length();

            bytes = new byte[(int) length];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }

            is.close();
        } catch (IOException e) {
            //TODO Write your catch method here
        }
        return bytes;
    }

    private void galleryAddPic(String path) {
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
        sendBroadcast(mediaScanIntent);
    }

    public TransportPacket.Packet buildTransportPacket(byte[] data, ServiceChain path, ServiceConnection origin, PathType type){
        TransportPacket.Packet.Builder builder = TransportPacket.Packet.newBuilder();

        TransportPacket.Path.Builder pathBuilder = TransportPacket.Path.newBuilder();
        while (path.peekNextServiceName() != null) {
            pathBuilder.addPath(path.getNextServiceName());
        }
        pathBuilder.setPos(0);

        builder.setPath(pathBuilder);

        InetSocketAddress address = (InetSocketAddress)origin.getAddress();
        TransportPacket.Origin originInfo = TransportPacket.Origin.newBuilder()
                .setIp(address.getAddress().getHostAddress())
                .setUuid(origin.getUserConnectionId().toString())
                .setName(origin.getServiceName())
                .setPort(address.getPort())
                .build();
        builder.setOrigin(originInfo);

        builder.setData(com.google.protobuf.ByteString.copyFrom(data));

        builder.setPathtype(TransportPacket.Packet.PathType.valueOf(type.toString()));
        builder.setKeepalive(true);
        return builder.build();
    }

}
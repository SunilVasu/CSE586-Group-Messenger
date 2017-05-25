package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    //IMPL
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    //Reference PA1
    //ports declarations
    static final int SERVER_PORT = 10000;
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String[] PORTS = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};

    static int keyNum=0;
    private final Uri mUri  = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");


    final ContentValues mContentValues = new ContentValues();

    //Reference - OnPTestClickListener
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    //IMPL-ends
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        Log.v(TAG, "<<*******>>");

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */

        //Reference PA1 - hack for the ports to communicate on 10000
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        //Reference PA1
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        //Button for sent
        final Button button;
        button =(Button) findViewById(R.id.button4);


        //Sent mesg when enter pressed
        //Reference PA 1
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // This is one way to reset the input box.
                    TextView tv = (TextView) findViewById(R.id.textView1);
                    tv.append("\t" + msg +"\n"); // This is one way to display a string.

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });

        //Sent msg when button 4 is pressed. Activity on this button press
        //Reference - http://stackoverflow.com/questions/20156733/how-to-add-button-click-event-in-android-studio
        //(1) - http://stackoverflow.com/questions/16636752/android-button-onclicklistener
        button.setOnClickListener(new View.OnClickListener() {
            //onClick
            public void onClick(View v)
            {
                //Reference - PA1
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView tv = (TextView) findViewById(R.id.textView1);
                tv.setMovementMethod(new ScrollingMovementMethod());
                tv.append("\t" + msg + "\n"); // This is one way to display a string.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }



    //Reference PA1
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            boolean check = true;

            try {
                while(check) {
                    //REFERENCES:-
                    //https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                    //http://stackoverflow.com/questions/15479697/printwriter-or-any-other-output-stream-in-java-do-not-know-r-n
                    //http://java2s.com/Tutorials/Java/Socket/How_to_read_data_from_Socket_connectin_using_Java.htm

                    Socket socket = serverSocket.accept();
                    DataInputStream dataIn = null;
                    DataOutputStream dataOut = null;

                    if(!socket.isInputShutdown()) {
                        //Read the message over the socket
                        dataIn = new DataInputStream(socket.getInputStream());
                        String msg = (String) dataIn.readUTF();


                        //Sent Ack
                        if(!msg.equals(null)){
                            dataOut = new DataOutputStream(socket.getOutputStream());
                            dataOut.writeUTF("PA-1_OK");
                            publishProgress(msg);
                            //Using Thread Sleep as this solved the intermittent failure
                            //Thread.sleep(500);
                            Log.e(TAG,"******Inside Ack***");
                        }

                    }
                    dataIn.close();
                    dataOut.close();
                    socket.close();
                  /*if(!socket.isConnected())
                {check = false;}*/
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"ServerTask Exception - IOException");
            }


            return null;
            // here
        }

        protected void onProgressUpdate(String...strings) {
            //REFERENECE - PA1
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");
            tv.append("\n");


            try {

                //REFERENCE - PA1
                mContentValues.put("key",Integer.toString(keyNum));
                mContentValues.put("value", strReceived);
                keyNum++;
                Log.v(TAG,"*********\n");
                Log.v(TAG, "key: "+mContentValues.get("key")+"\t value: "+mContentValues.get("value"));

                getContentResolver().insert(mUri, mContentValues);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            return;
        }


    }

    //Reference PA1
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                for (String remotePort: PORTS){
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(remotePort));
                    String msgToSend = msgs[0];

                    boolean check =true;
                    DataOutputStream dataOut = null;
                    DataInputStream dataIn = null;
                    if (!msgToSend.equals("") && socket.isConnected()) {
                        //REFERENCES:-
                        //https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                        //http://stackoverflow.com/questions/15479697/printwriter-or-any-other-output-stream-in-java-do-not-know-r-n
                        //http://java2s.com/Tutorials/Java/Socket/How_to_read_data_from_Socket_connectin_using_Java.htm

                        //Send a message over the socket
                        dataOut = new DataOutputStream(socket.getOutputStream());
                        dataOut.writeUTF(msgToSend);
                        dataOut.flush();
                        //Using Thread Sleep as this solved the intermittent failure
                        //Thread.sleep(500);
                        do {
                            //Wait for an acknowledgement
                            dataIn = new DataInputStream(socket.getInputStream());
                            String msg = (String) dataIn.readUTF();
                            if(msg.equals("PA-1_OK")){
                                check=false;
                            }
                        }while(check);
                    }


                    //Closing Resources
                    dataOut.close();
                    dataIn.close();

                    socket.close();

                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }


}

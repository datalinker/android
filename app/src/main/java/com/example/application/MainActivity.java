package com.example.application;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
    Button button;
    TextView text;

    private String address = ""; // datalinker bt uuid
    private String name = ""; // name
    private int baudrate= 4800; // baudrate of NMEA network
    private int wanrvoltage= 8; // warning voltage - red light starts blinking if power source voltage is below than this
    private int port = 2000; // tcp port

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("main","onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button);
        text = (TextView) findViewById(R.id.text);

        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
               connectDataLinker();
            }
        });

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder exitDialog = new AlertDialog.Builder(this);
        exitDialog.setTitle("Exit application?");

        exitDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialogInterface, int i)
            {
                disconnectDataLinker();
                System.exit(0);
            }
        });

        exitDialog.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialogInterface, int i)
            {
            }
        });

        exitDialog.show();

    }

    private void connectDataLinker()
    {
        try
        {
            Intent intent = new Intent();
            intent.setClassName("net.sailracer.datalinker", "net.sailracer.datalinker.OPEN");

            // configuration

            //intent.putExtra("address", "");
            //intent.putExtra("baudrate", 4800);
            //intent.putExtra("port", 2000);
            //intent.putExtra("warnvoltage", 5);

            startActivityForResult(intent, 0);
        }
        catch (ActivityNotFoundException e)
        {
            showDialog(MainActivity.this, "DataLinker server not found", "Download from Google play store?", "Yes", "No").show();
        }
    }

    private void disconnectDataLinker()
    {
        stopCapture();
        try
        {
            Intent intent = new Intent();
            intent.setClassName("net.sailracer.datalinker", "net.sailracer.datalinker.CLOSE");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        catch (Exception e)
        {

        }
    }

    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo)
    {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);

        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialogInterface, int i)
            {
                Uri uri = Uri.parse("market://search?q=pname:" + "net.sailracer.datalinker");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try
                {
                    act.startActivity(intent);
                }
                catch (ActivityNotFoundException anfe)
                {

                }
            }
        });

        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialogInterface, int i)
            {
            }
        });

        return downloadDialog.show();

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode == 0)
        {
            if (resultCode == RESULT_OK)
            {
                text.setText(intent.getStringExtra("message")+"\n"+text.getText());

                port = intent.getIntExtra("port", 2000);
                address = intent.getStringExtra("address");
                name = intent.getStringExtra("name");
                baudrate= intent.getIntExtra("baudrate", 4800);
                wanrvoltage= intent.getIntExtra("warnvoltage", 8);

                startCapture();
            }
            else
            {
                text.setText("");
            }
        }
    }

    // TCP part
    private String serverMessage;
    Socket socket;
    BufferedReader inputReader;
    Thread networkthread;

    public void startCapture()
    {
        synchronized(this)
        {
            if (networkthread==null)
            {
                networkthread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        runListener();
                    }
                });
                networkthread.start();
            }
        }
}

    public void stopCapture()
    {
        if( networkthread != null )
        {
            networkthread.interrupt();
        }
        try
        {
            socket.close();
        }
        catch (Exception e)
        {
        }

    }

    public void runListener()
    {
        try
        {
            socket = new Socket("127.0.0.1", port);
            socket.setKeepAlive(false);
            inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!networkthread.interrupted())
            {
                serverMessage = inputReader.readLine();
                parseMessage();
            }

        }
        catch (Exception error)
        {
            //text.setText(error.toString()+"\n"+text.getText());
            stopCapture();
        }
        finally
        {
            networkthread = null;
        }
    }

    ArrayList<String> log = new ArrayList<String>();

    public void parseMessage()
    {
        // run on UI thread
        MainActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                // do your stuff here
                log.add(serverMessage);
                if (log.size() == 10) log.remove(0);
                text.setText(TextUtils.join("\n", log));
            }
        });
    }
}

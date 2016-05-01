package co.floxx.floxx;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Button more = (Button) findViewById(R.id.button13);
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {


                AlertDialog alertDialog = new AlertDialog.Builder(SettingActivity.this).create(); //Read Update
                alertDialog.setTitle("Is Tea Era open today?");

                SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
                Date d = new Date();
                String dayOfTheWeek = sdf.format(d);
                Log.i("day", dayOfTheWeek);
                if (dayOfTheWeek.equals("Tuesday"))
                    alertDialog.setMessage("Nope :(");
                else
                    alertDialog.setMessage("Yup!");
                alertDialog.setButton("Continue..", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // here you can add functions
                    }
                });

                alertDialog.show();  //<-- See This!
            }

        });
    }
}

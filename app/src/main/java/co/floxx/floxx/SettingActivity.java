package co.floxx.floxx;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ImageButton button = (ImageButton) findViewById(R.id.left_arrow);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button more = (Button) findViewById(R.id.TeaEra);
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(SettingActivity.this).create();
                alertDialog.setTitle("Is Tea Era open today?");

                SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
                Date d = new Date();
                String dayOfTheWeek = sdf.format(d);
                Log.i("day", dayOfTheWeek);
                if (dayOfTheWeek.equals("Tuesday"))
                    alertDialog.setMessage("Nope :(");
                else
                    alertDialog.setMessage("Yup!");
                alertDialog.setButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // here you can add functions
                    }
                });

                alertDialog.show(); // see this!
            }

        });

        Button addFriends = (Button) findViewById(R.id.add_friends);
        addFriends.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, RequestsActivity.class);
                startActivity(intent);
            }
        });
    }
}

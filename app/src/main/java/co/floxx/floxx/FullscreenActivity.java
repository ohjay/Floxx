package co.floxx.floxx;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The full-screen activity that serves as Floxx's splash (/landing) page.
 */
public class FullscreenActivity extends AppCompatActivity {
    private EditText unameText;
    private EditText pwText;

    @Override
    protected void onResume() {
        super.onResume();
        ((EditText) findViewById(R.id.enter_username)).setText("");
        ((EditText) findViewById(R.id.enter_password)).setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        // Cool fonts let's go!
        Typeface montserrat = Typeface.createFromAsset(getAssets(), "Montserrat-Regular.otf");
        unameText = (EditText) findViewById(R.id.enter_username);
        unameText.setTypeface(montserrat);

        pwText = (EditText) findViewById(R.id.enter_password);
        pwText.setTypeface(montserrat);

        // Assign button behaviors
        setRegisterButtonBehavior();
        setLoginButtonBehavior();
    }

    public void setLoginButtonBehavior() {
        Button button = (Button) findViewById(R.id.login_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username11 = unameText.getText().toString();
                String password11 = pwText.getText().toString();

                if (username11.isEmpty()) {
                    Toast toast = Toast.makeText(FullscreenActivity.this,
                            "You have to enter a username!", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 30);
                    toast.show();
                    return;
                } else if (password11.isEmpty()) {
                    Toast toast = Toast.makeText(FullscreenActivity.this,
                            "You have to enter a password!", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 30);
                    toast.show();
                    return;
                }

                Intent intent = new Intent(FullscreenActivity.this,
                        FirebaseActivity.class);
                Bundle extras = new Bundle();
                extras.putString("enter_username", username11);
                extras.putString("enter_password", password11);
                intent.putExtras(extras);
                startActivity(intent);
            }
        });
    }

    public void setRegisterButtonBehavior() {
        TextView tv = (TextView) findViewById(R.id.register_button);

        tv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FullscreenActivity.this,
                        RegisterActivity.class));
            }
        });
    }
}

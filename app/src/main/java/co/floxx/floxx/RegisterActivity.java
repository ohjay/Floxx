package co.floxx.floxx;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button button = (Button) findViewById(R.id.register_button);
        final Bundle extras = new Bundle();

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, FirebaseActivity.class);
                String password = ((EditText) findViewById(R.id.register_password)).getText().toString();
                String confirm = ((EditText) findViewById(R.id.confirm_password)).getText().toString();

                if (!password.equals(confirm)) {
                    Toast.makeText(RegisterActivity.this, "Passwords did not match. Try again!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String username = ((EditText) findViewById(R.id.register_user)).getText().toString();
                String email = ((EditText) findViewById(R.id.register_email)).getText().toString();

                extras.putString("register_user", username);
                extras.putString("register_email", email);
                extras.putString("register_password", password);
                intent.putExtras(extras);
                RegisterActivity.this.startActivity(intent);
            }
        });
    }
}

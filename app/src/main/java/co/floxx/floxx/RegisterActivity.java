package co.floxx.floxx;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button button = (Button)findViewById(R.id.registering_button);
        final Bundle extras = new Bundle();
        final EditText username1 = (EditText) findViewById(R.id.register_user);
        final EditText password1 = (EditText) findViewById(R.id.register_password);

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, FirebaseActivity.class);
                String username11 = username1.getText().toString();
                String password11 = password1.getText().toString();
                extras.putString("register_user", username11);
                extras.putString("register_password", password11);
                intent.putExtras(extras);
                RegisterActivity.this.startActivity(intent);


//                intent = new Intent(RegisterActivity.this, FullscreenActivity.class);
//                Log.i("message","register complete!");
//                RegisterActivity.this.startActivity(intent);
            }
        });
    }

}

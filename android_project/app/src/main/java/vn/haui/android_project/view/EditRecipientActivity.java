package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import vn.haui.android_project.R;

public class EditRecipientActivity extends AppCompatActivity {
    private String locationId,address,phoneNumber,defaultLocation;
    private EditText et_recipient_name,et_phone_number,et_address;
    private Button btn_save,btn_delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_recipient);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_edit_recipient), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mapping();

        Intent intent = getIntent();
        if (intent != null) {
            locationId = intent.getStringExtra("location_id");
            address = intent.getStringExtra("address");
            phoneNumber = intent.getStringExtra("phoneNumber");
            defaultLocation = intent.getStringExtra("defaultLocation");
            et_recipient_name.setText(defaultLocation);
            et_phone_number.setText(phoneNumber);
            et_address.setText(address);
        }
    }

    private void  mapping(){
        et_address=findViewById(R.id.et_address);
        et_phone_number=findViewById(R.id.et_phone_number);
        et_recipient_name=findViewById(R.id.et_recipient_name);
        btn_save=findViewById(R.id.btn_save);
        btn_delete=findViewById(R.id.btn_delete);

    }
}
package com.example.database;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class SwitchActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_switch);
	}

	public void gotoBasic(View view) {
		startActivity(new Intent(this, BasicActivity.class));
		finish();
	}

	public void gotoAdvance(View view) {
		startActivity(new Intent(this, SignInActivity.class));
		finish();
	}
}
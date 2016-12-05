package com.example.database;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application{
	@Override
	public void onCreate() {
		super.onCreate();
		FirebaseDatabase.getInstance().setPersistenceEnabled(true);
	}
}
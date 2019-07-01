package com.example.database;

import android.app.Dialog;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.database.models.FriendlyMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import static com.example.database.R.string.username;

public class BasicActivity extends AppCompatActivity {
	private static final String CHILD_USERS = "chat-users";
	private static final String CHILD_MESSAGES = "chat";
	private static final String UID = "id-12345";
	private Button mButtonSet, mButtonPush, mButtonUpdateChildren, mButtonRemove;
	private DatabaseReference mRootRef, mUsersRef, mMessageRef;
	private Dialog mDialog;
	private EditText mEdtUsername, mEdtMessage;
	private String mUsername;
	private TextView mTextView;
	private ValueEventListener mValueEventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sample);
		initWidget();

		FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

		mRootRef = firebaseDatabase.getReference();
		mUsersRef = mRootRef.child(CHILD_USERS);
		mMessageRef = mRootRef.child(CHILD_MESSAGES);

		setEventListener();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mDialog.show();
		mValueEventListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				mDialog.dismiss();
				mUsername = dataSnapshot.child(CHILD_USERS).child(UID).getValue(String.class);

				mTextView.setText(getString(username, mUsername));
				if (TextUtils.isEmpty(mUsername)) {
					mButtonPush.setEnabled(false);
					mButtonUpdateChildren.setEnabled(false);
				} else {
					mButtonPush.setEnabled(true);
					mButtonUpdateChildren.setEnabled(true);
				}
				Iterable<DataSnapshot> children = dataSnapshot.child(CHILD_MESSAGES).getChildren();
				while(children.iterator().hasNext()){
					String key= children.iterator().next().getKey();
					FriendlyMessage friendlyMessage = dataSnapshot.child(CHILD_MESSAGES).child(key).getValue(FriendlyMessage.class);

					long now = System.currentTimeMillis();
					long past = now - (60 * 60 * 24 * 45 * 1000L);
					String x = DateUtils.getRelativeTimeSpanString(past, now, DateUtils.MINUTE_IN_MILLIS).toString();

					mTextView.append("username: " + friendlyMessage.getUsername() + " | ");
					mTextView.append("text: " + friendlyMessage.getText() + " (" + x + ")" + "\n");
				}
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				mDialog.dismiss();
				mTextView.setText(getString(R.string.fail_read, databaseError.getMessage()));
			}
		};
		mRootRef.addValueEventListener(mValueEventListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mValueEventListener != null) {
			mRootRef.removeEventListener(mValueEventListener);
		}
	}

	private void initWidget() {
		mTextView = findViewById(R.id.txt_result);
		mTextView.setMovementMethod(new ScrollingMovementMethod());
		mEdtUsername = findViewById(R.id.edt_username);
		mEdtMessage = findViewById(R.id.edt_message);
		mButtonSet = findViewById(R.id.btn_set);
		mButtonPush = findViewById(R.id.btn_push);
		mButtonUpdateChildren = findViewById(R.id.btn_update);
		mButtonRemove = findViewById(R.id.btn_remove);
		mDialog = new Dialog(this, R.style.NewDialog);
		mDialog.addContentView(
				new ProgressBar(this),
				new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
		);
		mDialog.setCancelable(true);
	}

	private void setEventListener() {
		mButtonSet.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mUsername = mEdtUsername.getText().toString().trim();
				if (TextUtils.isEmpty(mUsername)) {
					mEdtUsername.setError(getString(R.string.required));
				} else {
					mUsersRef.child(UID).setValue(mUsername);
					mEdtUsername.setError(null);
					mEdtUsername.setText(null);
				}
			}
		});
		mButtonPush.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String message = mEdtMessage.getText().toString().trim();
				if (TextUtils.isEmpty(message)) {
					mEdtMessage.setError(getString(R.string.required));
				} else {
					FriendlyMessage friendlyMessage = new FriendlyMessage(message, mUsername);
					mMessageRef.push().setValue(friendlyMessage);
					mEdtMessage.setError(null);
					mEdtMessage.setText(null);
				}
			}
		});
		mButtonUpdateChildren.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String key = mMessageRef.push().getKey();
				String message = mEdtMessage.getText().toString().trim();
				if (TextUtils.isEmpty(message)) {
					mEdtMessage.setError(getString(R.string.required));
				} else {
					HashMap<String, Object> postValues = new HashMap<>();
					postValues.put("username", mUsername);
					postValues.put("text", message);

					Map<String, Object> childUpdates = new HashMap<>();
					childUpdates.put("/chat/" + key, postValues);
					childUpdates.put("/chat-each-users/" + mUsername + "/" + key, postValues);
					mRootRef.updateChildren(childUpdates);

					mEdtMessage.setError(null);
					mEdtMessage.setText(null);
				}
			}
		});
		mButtonRemove.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mMessageRef.removeValue();
			}
		});
	}
}
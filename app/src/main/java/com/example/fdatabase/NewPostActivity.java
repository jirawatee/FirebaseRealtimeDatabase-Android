package com.example.fdatabase;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.fdatabase.models.Post;
import com.example.fdatabase.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class NewPostActivity extends BaseActivity {
	private DatabaseReference mDatabase;
	private EditText mTitleField, mBodyField;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_post);
		mTitleField = (EditText) findViewById(R.id.field_title);
		mBodyField = (EditText) findViewById(R.id.field_body);

		mDatabase = FirebaseDatabase.getInstance().getReference();

		findViewById(R.id.fab_submit_post).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				submitPost();
			}
		});
	}

	private boolean validateForm(String title, String body) {
		if (TextUtils.isEmpty(title)) {
			mTitleField.setError(getString(R.string.required));
			return false;
		} else if (TextUtils.isEmpty(body)) {
			mBodyField.setError(getString(R.string.required));
			return false;
		} else {
			mTitleField.setError(null);
			mBodyField.setError(null);
			return true;
		}
	}

	private void submitPost() {
		final String title = mTitleField.getText().toString().trim();
		final String body = mBodyField.getText().toString().trim();
		final String userId = getUid();

		if (validateForm(title, body)) {
			mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					User user = dataSnapshot.getValue(User.class);
					if (user == null) {
						Toast.makeText(NewPostActivity.this, "Error: could not fetch user.", Toast.LENGTH_LONG).show();
					} else {
						writeNewPost(userId, user.username, title, body);
					}
					finish();
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					Toast.makeText(NewPostActivity.this, "onCancelled: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	private void writeNewPost(String userId, String username, String title, String body) {
		// Create new post at /user-posts/$userid/$postid
		// and at /posts/$postid simultaneously
		String key = mDatabase.child("posts").push().getKey();
		Post post = new Post(userId, username, title, body);
		Map<String, Object> postValues = post.toMap();

		Map<String, Object> childUpdates = new HashMap<>();
		childUpdates.put("/posts/-KOo1jaSnU2XFiSonIjB", postValues);
		childUpdates.put("/user-posts/" + userId + "/-KOo1jaSnU2XFiSonIjB", postValues);

		mDatabase.updateChildren(childUpdates);
	}
}
package com.example.database;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.database.models.FriendlyMessage;
import com.example.database.models.User;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChatActivity extends AppCompatActivity {
	public static final String MESSAGES_CHILD = "messages";

	private DatabaseReference mFirebaseDatabaseReference;
	private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;

	private Button mSendButton;
	private RecyclerView mMessageRecyclerView;
	private LinearLayoutManager mLinearLayoutManager;
	private EditText mMessageEditText;
	private String mUsername = "Anonymous";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		mMessageEditText = (EditText) findViewById(R.id.messageEditText);
		mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
		mLinearLayoutManager = new LinearLayoutManager(this);
		mLinearLayoutManager.setStackFromEnd(true);

		// Initialize Firebase Auth
		FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
		FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		mFirebaseDatabaseReference.child("users").child(mFirebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				User user = dataSnapshot.getValue(User.class);
				mUsername = user.username;
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});

		mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(
				FriendlyMessage.class,
				R.layout.item_message,
				MessageViewHolder.class,
				mFirebaseDatabaseReference.child(MESSAGES_CHILD)
		) {
			@Override
			protected void populateViewHolder(MessageViewHolder viewHolder, FriendlyMessage friendlyMessage, int position) {
				if (friendlyMessage.getUsername().equals(mUsername)) {
					viewHolder.row.setGravity(Gravity.END);
				} else {
					viewHolder.row.setGravity(Gravity.START);
				}
				viewHolder.messageTextView.setText(friendlyMessage.getText());
				viewHolder.messengerTextView.setText(friendlyMessage.getUsername());
			}
		};
		mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
			@Override
			public void onItemRangeInserted(int positionStart, int itemCount) {
				super.onItemRangeInserted(positionStart, itemCount);
				int friendlyMessageCount = mFirebaseAdapter.getItemCount();
				int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
				// If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
				// to the bottom of the list to show the newly added message.
				if (lastVisiblePosition == -1 || (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
					mMessageRecyclerView.scrollToPosition(positionStart);
				}
			}
		});
		mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
		mMessageRecyclerView.setAdapter(mFirebaseAdapter);

		mMessageEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				if (charSequence.toString().trim().length() > 0) {
					mSendButton.setEnabled(true);
				} else {
					mSendButton.setEnabled(false);
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		});

		mSendButton = (Button) findViewById(R.id.sendButton);
		mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername);
				mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(friendlyMessage);
				mMessageEditText.setText("");
			}
		});
	}

	public static class MessageViewHolder extends RecyclerView.ViewHolder {
		public LinearLayout row;
		public TextView messageTextView;
		public TextView messengerTextView;

		public MessageViewHolder(View v) {
			super(v);
			row = (LinearLayout) itemView.findViewById(R.id.row);
			messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
			messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
		}
	}
}
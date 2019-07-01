package com.example.database;

import android.content.Context;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.database.models.Comment;
import com.example.database.models.Post;
import com.example.database.models.User;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "PostDetailActivity";
	public static final String EXTRA_POST_KEY = "post_key";
	private DatabaseReference mPostReference, mCommentsReference;
	private ValueEventListener mPostListener;
	private CommentAdapter mAdapter;
	private TextView mAuthorView, mTitleView, mBodyView;
	private EditText mCommentField;
	private RecyclerView mCommentsRecycler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post_detail);
		mAuthorView = findViewById(R.id.post_author);
		mTitleView = findViewById(R.id.post_title);
		mBodyView = findViewById(R.id.post_body);
		mCommentField = findViewById(R.id.field_comment_text);

		mCommentsRecycler = findViewById(R.id.recycler_comments);
		mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));

		Button mCommentButton = findViewById(R.id.button_post_comment);
		mCommentButton.setOnClickListener(this);

		// Get post key from intent
		String mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
		if (mPostKey == null) {
			throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
		}

		// Initialize Database
		mPostReference = FirebaseDatabase.getInstance().getReference().child("posts").child(mPostKey);
		mCommentsReference = FirebaseDatabase.getInstance().getReference().child("post-comments").child(mPostKey);
	}

	@Override
	public void onStart() {
		super.onStart();

		// Add value event listener to the post
		ValueEventListener postListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				// Get Post object and use the values to update the UI
				Post post = dataSnapshot.getValue(Post.class);

				mAuthorView.setText(post.author);
				mTitleView.setText(post.title);
				mBodyView.setText(post.body);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// Getting Post failed, log a message
				Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
				Toast.makeText(PostDetailActivity.this, "Failed to load post.", Toast.LENGTH_SHORT).show();
			}
		};
		mPostReference.addValueEventListener(postListener);

		// Keep copy of post listener so we can remove it when app stops
		mPostListener = postListener;

		// Listen for comments
		mAdapter = new CommentAdapter(this, mCommentsReference);
		mCommentsRecycler.setAdapter(mAdapter);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mPostListener != null) {
			mPostReference.removeEventListener(mPostListener);
		}
		mAdapter.cleanupListener();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_post_comment:
				postComment();
				break;
		}
	}

	private void postComment() {
		final String uid = getUid();
		FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				// Get user information
				User user = dataSnapshot.getValue(User.class);
				String authorName = user.username;

				// Create new comment object
				String commentText = mCommentField.getText().toString().trim();
				Comment comment = new Comment(uid, authorName, commentText);

				// Push the comment, it will appear in the list
				mCommentsReference.push().setValue(comment);

				// Clear the field
				mCommentField.setText(null);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Toast.makeText(PostDetailActivity.this, "onCancelled: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
			}
		});
	}

	private static class CommentViewHolder extends RecyclerView.ViewHolder {
		TextView authorView;
		TextView bodyView;
		CommentViewHolder(View itemView) {
			super(itemView);
			authorView = itemView.findViewById(R.id.comment_author);
			bodyView = itemView.findViewById(R.id.comment_body);
		}
	}

	private static class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> {
		private Context mContext;
		private DatabaseReference mDatabaseReference;
		private ChildEventListener mChildEventListener;
		private List<String> mCommentIds = new ArrayList<>();
		private List<Comment> mComments = new ArrayList<>();

		CommentAdapter(final Context context, DatabaseReference ref) {
			mContext = context;
			mDatabaseReference = ref;

			// Create child event listener
			ChildEventListener childEventListener = new ChildEventListener() {
				@Override
				public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
					Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

					// A new comment has been added, add it to the displayed list
					Comment comment = dataSnapshot.getValue(Comment.class);

					// Update RecyclerView
					mCommentIds.add(dataSnapshot.getKey());
					mComments.add(comment);
					notifyItemInserted(mComments.size() - 1);
				}

				@Override
				public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
					Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

					// A comment has changed, use the key to determine if we are displaying this
					// comment and if so displayed the changed comment.
					Comment newComment = dataSnapshot.getValue(Comment.class);
					String commentKey = dataSnapshot.getKey();

					int commentIndex = mCommentIds.indexOf(commentKey);
					if (commentIndex > -1) {
						// Replace with the new data
						mComments.set(commentIndex, newComment);

						// Update the RecyclerView
						notifyItemChanged(commentIndex);
					} else {
						Log.w(TAG, "onChildChanged:unknown_child:" + commentKey);
					}
				}

				@Override
				public void onChildRemoved(DataSnapshot dataSnapshot) {
					Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

					// A comment has changed, use the key to determine if we are displaying this
					// comment and if so remove it.
					String commentKey = dataSnapshot.getKey();

					int commentIndex = mCommentIds.indexOf(commentKey);
					if (commentIndex > -1) {
						// Remove data from the list
						mCommentIds.remove(commentIndex);
						mComments.remove(commentIndex);

						// Update the RecyclerView
						notifyItemRemoved(commentIndex);
					} else {
						Log.w(TAG, "onChildRemoved:unknown_child:" + commentKey);
					}
				}

				@Override
				public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
					Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

					// A comment has changed position, use the key to determine if we are
					// displaying this comment and if so move it.
					//Comment movedComment = dataSnapshot.getValue(Comment.class);
					//String commentKey = dataSnapshot.getKey();
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					Log.w(TAG, "postComments:onCancelled", databaseError.toException());
					Toast.makeText(mContext, "Failed to load comments.", Toast.LENGTH_SHORT).show();
				}
			};
			ref.addChildEventListener(childEventListener);

			// Store reference to listener so it can be removed on app stop
			mChildEventListener = childEventListener;
		}

		@Override
		public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			View view = inflater.inflate(R.layout.item_comment, parent, false);
			return new CommentViewHolder(view);
		}

		@Override
		public void onBindViewHolder(CommentViewHolder holder, int position) {
			Comment comment = mComments.get(position);
			holder.authorView.setText(comment.author);
			holder.bodyView.setText(comment.text);
		}

		@Override
		public int getItemCount() {
			return mComments.size();
		}

		void cleanupListener() {
			if (mChildEventListener != null) {
				mDatabaseReference.removeEventListener(mChildEventListener);
			}
		}
	}
}
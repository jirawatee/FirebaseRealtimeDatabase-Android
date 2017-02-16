package com.example.database.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.database.PostDetailActivity;
import com.example.database.R;
import com.example.database.models.Post;
import com.example.database.viewholder.PostViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;

public abstract class PostListFragment extends Fragment {
	private Activity mActivity;
	private DatabaseReference mDatabase;
	private FirebaseRecyclerAdapter<Post, PostViewHolder> mAdapter;
	private RecyclerView mRecycler;

	public PostListFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View rootView = inflater.inflate(R.layout.fragment_all_posts, container, false);
		mRecycler = (RecyclerView) rootView.findViewById(R.id.messages_list);
		mRecycler.setHasFixedSize(true);

		mDatabase = FirebaseDatabase.getInstance().getReference();
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mActivity = getActivity();

		final Dialog mDialog = new Dialog(mActivity, R.style.NewDialog);
		mDialog.addContentView(
				new ProgressBar(mActivity),
				new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
		);
		mDialog.setCancelable(true);
		mDialog.show();

		// Set up Layout Manager, reverse layout
		LinearLayoutManager mManager = new LinearLayoutManager(mActivity);
		mManager.setReverseLayout(true);
		mManager.setStackFromEnd(true);
		mRecycler.setLayoutManager(mManager);

		// Set up FirebaseRecyclerAdapter with the Query
		Query postsQuery = getQuery(mDatabase);
		mAdapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(Post.class, R.layout.item_post, PostViewHolder.class, postsQuery) {
			@Override
			protected void onDataChanged() {
				super.onDataChanged();
				mDialog.dismiss();
			}

			@Override
			protected void populateViewHolder(final PostViewHolder viewHolder, final Post model, final int position) {
				final DatabaseReference postRef = getRef(position);

				// Determine if the current user has liked this post and set UI accordingly
				if (model.stars.containsKey(getUid())) {
					viewHolder.starView.setImageResource(R.drawable.ic_toggle_star_24);
				} else {
					viewHolder.starView.setImageResource(R.drawable.ic_toggle_star_outline_24);
				}

				// Bind Post to ViewHolder, setting OnClickListener for the star button
				viewHolder.bindToPost(model, new View.OnClickListener() {
					@Override
					public void onClick(View starView) {
						// Need to write to both places the post is stored
						DatabaseReference globalPostRef = mDatabase.child("posts").child(postRef.getKey());
						DatabaseReference userPostRef = mDatabase.child("user-posts").child(model.uid).child(postRef.getKey());

						// Run two transactions
						onStarClicked(globalPostRef);
						onStarClicked(userPostRef);
					}
				});

				viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(mActivity, PostDetailActivity.class);
						intent.putExtra(PostDetailActivity.EXTRA_POST_KEY, postRef.getKey());
						startActivity(intent);
					}
				});
			}
		};
		mRecycler.setAdapter(mAdapter);
	}

	private void onStarClicked(DatabaseReference postRef) {
		postRef.runTransaction(new Transaction.Handler() {
			@Override
			public Transaction.Result doTransaction(MutableData mutableData) {
				Post p = mutableData.getValue(Post.class);
				if (p == null) {
					return Transaction.success(mutableData);
				}

				if (p.stars.containsKey(getUid())) {
					// Unstar the post and remove self from stars
					p.starCount = p.starCount - 1;
					p.stars.remove(getUid());
				} else {
					// Star the post and add self to stars
					p.starCount = p.starCount + 1;
					p.stars.put(getUid(), true);
				}

				// Set value and report transaction success
				mutableData.setValue(p);
				return Transaction.success(mutableData);
			}

			@Override
			public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
				Log.d("postTransaction", "onComplete:" + dataSnapshot.getKey());
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			mAdapter.cleanup();
		}
	}

	public String getUid() {
		return FirebaseAuth.getInstance().getCurrentUser().getUid();
	}

	public abstract Query getQuery(DatabaseReference databaseReference);
}
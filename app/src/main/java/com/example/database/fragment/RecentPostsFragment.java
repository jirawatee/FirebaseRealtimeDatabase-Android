package com.example.database.fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public class RecentPostsFragment extends PostListFragment {
	public RecentPostsFragment() {
	}

	@Override
	public Query getQuery(DatabaseReference databaseReference) {
		// Last 100 posts, these are automatically the 100 most recent
		// due to sorting by push() keys
		return databaseReference.child("posts").limitToFirst(5);
		//return databaseReference.child("posts").orderByChild("title").equalTo("test").limitToLast(2);


		//return databaseReference.child("posts").orderByKey().startAt("-KRN9eHLLMJbYmJNFz9U").limitToFirst(10);
	}
}
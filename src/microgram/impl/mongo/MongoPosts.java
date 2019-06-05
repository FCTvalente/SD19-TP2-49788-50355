package microgram.impl.mongo;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;
import static microgram.impl.mongo.MongoProfiles.Profiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import microgram.api.Post;
import microgram.api.java.Posts;
import microgram.api.java.Result;
import microgram.impl.pojos.*;

public final class MongoPosts extends MongoAbsClient implements Posts {
	
	static MongoPosts Posts;
	
	private static final String DB_NAME = "profilesdb";
	private static final String DB_TABLE_POSTS = "poststable";
	private static final String DB_TABLE_LIKES = "likestable";
	private static final String DB_TABLE_USERPOSTS = "userpoststable";

	final MongoCollection<Post> posts;
	final MongoCollection<PairPOJO> likes;
	final MongoCollection<PairPOJO> userposts;
	
	public MongoPosts() {
		super(DB_NAME);
		posts = dbName.getCollection(DB_TABLE_POSTS, Post.class);
		likes = dbName.getCollection(DB_TABLE_LIKES, PairPOJO.class);
		userposts = dbName.getCollection(DB_TABLE_USERPOSTS, PairPOJO.class);
		
		posts.createIndex(Indexes.ascending(Post.ID), new IndexOptions().unique(true));
		likes.createIndex(Indexes.ascending(PairPOJO.ID1, PairPOJO.ID2), new IndexOptions().unique(true));
		userposts.createIndex(Indexes.ascending(PairPOJO.ID1, PairPOJO.ID2), new IndexOptions().unique(true));
		
		Posts = this;
	}

	@Override
	public Result<Post> getPost(String postId) {
		Post post = posts.find(Filters.eq(Post.ID, postId)).first();
		if(post == null) {
			return error(NOT_FOUND);
		}
		int nlikes = (int) likes.countDocuments(Filters.eq(PairPOJO.ID1, postId));
		post.setLikes(nlikes);
		
		return ok(post);
	}

	@Override
	public Result<String> createPost(Post post) {
		Post p = post;
		String ownerId = p.getOwnerId();
		
		if(!Profiles.getProfile(ownerId).isOK()) {
			return error(NOT_FOUND);
		}
		String postId = p.getPostId();
		posts.insertOne(p);
		userposts.insertOne(new PairPOJO(ownerId, postId));
		
		return ok(postId);
	}

	@Override
	public Result<Void> deletePost(String postId) {
		Post post = posts.findOneAndDelete(Filters.eq(Post.ID, postId));
		if(post == null) {
			return error(NOT_FOUND);
		}
		likes.deleteMany(Filters.eq(PairPOJO.ID1, postId));
		userposts.deleteOne(Filters.eq(PairPOJO.ID2, postId));
		return ok();
	}

	@Override
	public Result<Void> like(String postId, String userId, boolean isLiked) {
		int test = (int) posts.countDocuments(Filters.eq(Post.ID, postId));
		if(test == 0) {
			return error(NOT_FOUND);
		}
		if (isLiked) {
			try {
			    likes.insertOne(new PairPOJO(postId, userId)); 
			} catch( MongoWriteException x ) {
				return error(CONFLICT);
			}
		} else {
			PairPOJO pair = likes.findOneAndDelete(Filters.and(Filters.eq(PairPOJO.ID1, postId), Filters.eq(PairPOJO.ID2, userId)));
			if (pair == null) {
				return error(NOT_FOUND);
			}
		}
		return ok();
	}

	@Override
	public Result<Boolean> isLiked(String postId, String userId) {
		int test = (int) posts.countDocuments(Filters.eq(Post.ID, postId));
		if(test == 0) {
			return error(NOT_FOUND);
		}
		PairPOJO pair = likes.find(Filters.and(Filters.eq(PairPOJO.ID1, postId), Filters.eq(PairPOJO.ID2, userId))).first();
		if(pair == null) {
			return ok(false);
		}
		return ok(true);
	}

	@Override
	public Result<List<String>> getPosts(String userId) {
		if(!Profiles.getProfile(userId).isOK()) {
			return error(NOT_FOUND);
		}
		Set<String> res = new HashSet<String>();
		for (PairPOJO doc : userposts.find(Filters.eq(PairPOJO.ID1, userId))) {
			res.add(doc.getId2());
		}
		
		return ok(new ArrayList<>(res));
	}

	@Override
	public Result<List<String>> getFeed(String userId) {
		Set<String> following = Profiles.following(userId);
		if(following == null) {
			return error(NOT_FOUND);
		}
		Set<String> res = new HashSet<>();
		for (String followee : following) {
			for (PairPOJO pair : userposts.find(Filters.eq(PairPOJO.ID1, followee))) {
				res.add(pair.getId2());
			}
		}
		return ok(new ArrayList<>(res));
	}

	int getUserPostsStats(String userId) {
		int res = (int) userposts.countDocuments(Filters.eq(PairPOJO.ID1, userId));
		return res;
	}
	
	void deleteAllUserPosts(String userId) {
		for (PairPOJO pair : userposts.find(Filters.eq(PairPOJO.ID1, userId))) {
			deletePost(pair.getId2());
		}
	}
	
	

}

package microgram.impl.mongo;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ok;
import static microgram.api.java.Result.ErrorCode.CONFLICT;
import static microgram.api.java.Result.ErrorCode.NOT_FOUND;
import static microgram.impl.mongo.MongoPosts.Posts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import microgram.api.Profile;
import microgram.api.java.Profiles;
import microgram.api.java.Result;
import microgram.impl.pojos.*;

public final class MongoProfiles extends MongoAbsClient implements Profiles {
	
	static MongoProfiles Profiles;
	
	private static final String DB_NAME = "profilesdb";
	private static final String DB_TABLE_USERS = "userstable";
	private static final String DB_TABLE_RELATIONS = "followrelationstable";
	
	static final String FOLLOWER = PairPOJO.ID1;
	static final String FOLLOWEE = PairPOJO.ID2;

	final MongoCollection<Profile> users;
	final MongoCollection<PairPOJO> followrelations;
	
	public MongoProfiles() {
		super(DB_NAME);
		users = dbName.getCollection(DB_TABLE_USERS, Profile.class);
		followrelations = dbName.getCollection(DB_TABLE_RELATIONS, PairPOJO.class);
		
		users.createIndex(Indexes.ascending(Profile.ID), new IndexOptions().unique(true));
		followrelations.createIndex(Indexes.ascending(FOLLOWER, FOLLOWEE), new IndexOptions().unique(true));
		
		Profiles = this;
	}

	@Override
	public Result<Profile> getProfile(String userId) {
		Profile user = users.find(Filters.eq(Profile.ID, userId)).first();
		if(user == null) {
			return error(NOT_FOUND);
		}
		int followers = (int) followrelations.countDocuments(Filters.eq(FOLLOWEE, userId));
		int following = (int) followrelations.countDocuments(Filters.eq(FOLLOWER, userId));
		user.setFollowers(followers);
		user.setFollowing(following);
		user.setPosts(Posts.getUserPostsStats(userId));
		
		return ok(user);
	}

	@Override
	public Result<Void> createProfile(Profile profile) {
		try {
		    users.insertOne( profile ); 
		} catch( MongoWriteException x ) {
			return error(CONFLICT);
		}
		
		return ok();
	}

	@Override
	public Result<Void> deleteProfile(String userId) {
		Profile p = users.findOneAndDelete(Filters.eq(Profile.ID, userId));
		if(p == null) {
			return error(NOT_FOUND);
		}
		followrelations.deleteMany(Filters.or(Filters.eq(FOLLOWER, userId), Filters.eq(FOLLOWEE, userId)));
		Posts.deleteAllUserPosts(userId);
		return ok();
	}

	@Override
	public Result<List<Profile>> search(String prefix) {
		Set<Profile> res = new HashSet<>();
		Pattern query = Pattern.compile("^"+Pattern.quote(prefix));
		for (Profile profile : users.find(Filters.regex(Profile.ID, query))) {
			res.add(profile);
		}
		
		return ok(new ArrayList<>(res));
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing) {
		long test = users.countDocuments(Filters.or(Filters.eq(Profile.ID, userId1), Filters.eq(Profile.ID, userId2)));
		if(test != 2) {
			return error(NOT_FOUND);
		}
		if(isFollowing) {
			try {
			    followrelations.insertOne(new PairPOJO(userId1, userId2)); 
			} catch( MongoWriteException x ) {
				//nothing
			}
		} else {
			followrelations.deleteOne(Filters.and(Filters.eq(FOLLOWER, userId1), Filters.eq(FOLLOWEE, userId2)));
		}
		return ok();
	}

	@Override
	public Result<Boolean> isFollowing(String userId1, String userId2) {
		long test = users.countDocuments(Filters.eq(Profile.ID, userId1));
		if(test == 0) {
			return error(NOT_FOUND);
		}
		PairPOJO pair = followrelations.find(Filters.and(Filters.eq(FOLLOWER, userId1), Filters.eq(FOLLOWEE, userId2))).first();
		if(pair == null) {
			return ok(false);
		}
		return ok(true);
	}
	
	Set<String> following(String userId) {
		Set<String> res = new HashSet<>();
		for (PairPOJO pair : followrelations.find(Filters.eq(FOLLOWER, userId))) {
			res.add(pair.getId2());
		}
		
		return res;
	}
}

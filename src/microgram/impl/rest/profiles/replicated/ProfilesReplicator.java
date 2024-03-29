package microgram.impl.rest.profiles.replicated;

import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ErrorCode.NOT_IMPLEMENTED;
import static microgram.impl.rest.replication.MicrogramOperation.Operation.*;

import java.util.List;

import microgram.api.Profile;
import microgram.api.java.Profiles;
import microgram.api.java.Result;
import microgram.impl.rest.replication.MicrogramOperation;
import microgram.impl.rest.replication.MicrogramOperation.Operation;
import microgram.impl.rest.replication.MicrogramOperationExecutor;
import microgram.impl.rest.replication.OrderedExecutor;

public class ProfilesReplicator implements MicrogramOperationExecutor, Profiles {

	private static final int FOLLOWER = 0, FOLLOWEE = 1;
	
	final Profiles localReplicaDB;
	final OrderedExecutor executor;
	
	ProfilesReplicator( Profiles localDB, OrderedExecutor executor) {
		this.localReplicaDB = localDB;
		this.executor = executor.init(this);
	}
	
	@Override
	public Result<?> execute( MicrogramOperation op ) {
		switch( op.type ) {
			case GetProfile: {
				return localReplicaDB.getProfile( op.arg( String.class));
			}
			case CreateProfile: {
				return localReplicaDB.createProfile( op.arg( Profile.class));
			}
			case DeleteProfile: {
				return localReplicaDB.deleteProfile( op.arg( String.class));
			}
			case SearchProfile: {
				return localReplicaDB.search( op.arg( String.class));
			}
			case FollowProfile: {
				String[] users = op.args(String[].class);
				return localReplicaDB.follow( users[FOLLOWER], users[FOLLOWEE], true);
			}
			case UnFollowProfile: {
				String[] users = op.args(String[].class);
				return localReplicaDB.follow( users[FOLLOWER], users[FOLLOWEE], false);
			}
			case IsFollowing: {
				String[] users = op.args(String[].class);
				return localReplicaDB.isFollowing( users[FOLLOWER], users[FOLLOWEE]);
			}
			default:
				return error(NOT_IMPLEMENTED);
		}	
	}

	@Override
	public Result<Profile> getProfile(String userId) {
		//return executor.replicate( new MicrogramOperation(GetProfile, userId));
		return localReplicaDB.getProfile(userId);
	}

	@Override
	public Result<Void> createProfile(Profile profile) {
		return executor.replicate( new MicrogramOperation(CreateProfile, profile));
	}

	@Override
	public Result<Void> deleteProfile(String userId) {
		return executor.replicate( new MicrogramOperation(DeleteProfile, userId));
	}

	@Override
	public Result<List<Profile>> search(String prefix) {
		//return executor.replicate( new MicrogramOperation(SearchProfile, prefix));
		return localReplicaDB.search(prefix);
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing) {
		Operation op;
		if(isFollowing) {
			op = FollowProfile;
		} else {
			op = UnFollowProfile;
		}
		return executor.replicate( new MicrogramOperation(op, new String[] {userId1, userId2}));
	}

	@Override
	public Result<Boolean> isFollowing(String userId1, String userId2) {
		//return executor.replicate( new MicrogramOperation(IsFollowing, new String[] {userId1, userId2}));
		return localReplicaDB.isFollowing(userId1, userId2);
	}
}

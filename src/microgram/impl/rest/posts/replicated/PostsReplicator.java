package microgram.impl.rest.posts.replicated;
import static microgram.api.java.Result.error;
import static microgram.api.java.Result.ErrorCode.NOT_IMPLEMENTED;
import static microgram.impl.rest.replication.MicrogramOperation.Operation.*;

import java.util.List;

import microgram.api.Post;
import microgram.api.java.Posts;
import microgram.api.java.Result;
import microgram.impl.rest.replication.MicrogramOperation;
import microgram.impl.rest.replication.MicrogramOperationExecutor;
import microgram.impl.rest.replication.OrderedExecutor;
import microgram.impl.rest.replication.MicrogramOperation.Operation;

public class PostsReplicator implements MicrogramOperationExecutor, Posts {

	private static final int PostID = 0, UserID = 1;
	
	final Posts localReplicaDB;
	final OrderedExecutor executor;
	
	PostsReplicator( Posts localDB, OrderedExecutor executor) {
		this.localReplicaDB = localDB;
		this.executor = executor.init(this);
	}

	@Override
	public Result<?> execute(MicrogramOperation op) {
		switch( op.type ) {
			case GetPost: {
				return localReplicaDB.getPost( op.arg( String.class));
			}
			case CreatePost: {
				return localReplicaDB.createPost( op.arg( Post.class));
			}
			case DeletePost: {
				return localReplicaDB.deletePost( op.arg( String.class));
			}
			case LikePost: {
				String[] args = op.args(String[].class);
				return localReplicaDB.like( args[PostID], args[UserID], true);
			}
			case UnLikePost: {
				String[] args = op.args(String[].class);
				return localReplicaDB.like( args[PostID], args[UserID], false);
			}
			case IsLiked: {
				String[] args = op.args(String[].class);
				return localReplicaDB.isLiked( args[PostID], args[UserID]);
			}
			case GetPosts: {
				return localReplicaDB.getPosts( op.arg( String.class));
			}
			case GetFeed: {
				return localReplicaDB.getFeed( op.arg( String.class));
			}
			default:
				return error(NOT_IMPLEMENTED);
		}	
	}

	@Override
	public Result<Post> getPost(String postId) {
		//return executor.replicate( new MicrogramOperation(GetPost, postId));
		return localReplicaDB.getPost(postId);
	}

	@Override
	public Result<String> createPost(Post post) {
		return executor.replicate( new MicrogramOperation(CreatePost, post));
	}

	@Override
	public Result<Void> deletePost(String postId) {
		return executor.replicate( new MicrogramOperation(DeletePost, postId));
	}

	@Override
	public Result<Void> like(String postId, String userId, boolean isLiked) {
		Operation op;
		if(isLiked) {
			op = LikePost;
		} else {
			op = UnLikePost;
		}
		return executor.replicate( new MicrogramOperation(op, new String[] {postId, userId}));
	}

	@Override
	public Result<Boolean> isLiked(String postId, String userId) {
		//return executor.replicate( new MicrogramOperation(IsLiked, new String[] {postId, userId}));
		return localReplicaDB.isLiked(postId, userId);
	}

	@Override
	public Result<List<String>> getPosts(String userId) {
		//return executor.replicate( new MicrogramOperation(GetPosts, userId));
		return localReplicaDB.getPosts(userId);
	}

	@Override
	public Result<List<String>> getFeed(String userId) {
		//return executor.replicate( new MicrogramOperation(GetFeed, userId));
		return localReplicaDB.getFeed(userId);
	}
}

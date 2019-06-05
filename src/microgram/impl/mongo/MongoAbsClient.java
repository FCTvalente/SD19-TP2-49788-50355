package microgram.impl.mongo;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public abstract class MongoAbsClient {

	private static final String MONGO_HOSTNAME = "localhost";

	protected final MongoClient mongo;

	protected final CodecRegistry pojoCodecRegistry;
	
	protected final MongoDatabase dbName;
	
	public MongoAbsClient(String dbname) {
		mongo = new MongoClient( MONGO_HOSTNAME );
		pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(), fromProviders(PojoCodecProvider.builder().automatic(true).build()));
		dbName = mongo.getDatabase(dbname).withCodecRegistry(pojoCodecRegistry);
	}
}

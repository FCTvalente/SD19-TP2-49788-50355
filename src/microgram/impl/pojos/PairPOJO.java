package microgram.impl.pojos;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class PairPOJO {

	@BsonIgnore
	public static final String ID1 = "id1";
	@BsonIgnore
	public static final String ID2 = "id2";
	@BsonIgnore
	public static final String VALUE = "profile";
	
	private String id1;
	private String id2;
	
	@BsonCreator
	public PairPOJO(@BsonProperty(ID1) String id1, @BsonProperty(ID2) String id2) {
		this.id1 = id1;
		this.id2 = id2;
	}
	
	public String getId1() {
		return id1;
	}
	
	public String getId2() {
		return id2;
	}
}

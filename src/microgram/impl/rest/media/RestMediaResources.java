package microgram.impl.rest.media;

import microgram.api.java.Media;
import microgram.api.rest.RestMedia;
import microgram.impl.dropbox.DropboxMedia;
import microgram.impl.rest.RestResource;

public class RestMediaResources extends RestResource implements RestMedia {

	final Media impl;
	final String baseUri;

	public RestMediaResources(String baseUri) {
		this.baseUri = baseUri;
		this.impl = new DropboxMedia();
	}

	@Override
	public String upload(byte[] bytes) {
		return baseUri + "/" + super.resultOrThrow(impl.upload(bytes));
	}

	@Override
	public byte[] download(String id) {
		return super.resultOrThrow(impl.download(id));
	}

	@Override
	public void delete(String id) {
		super.resultOrThrow(impl.delete(id));
	}
}

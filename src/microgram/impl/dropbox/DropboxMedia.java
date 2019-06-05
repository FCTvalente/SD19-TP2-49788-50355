package microgram.impl.dropbox;

import java.io.InputStream;

import org.pac4j.scribe.builder.api.DropboxApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import dropbox.msgs.AccessFileV2Args;
import dropbox.msgs.CreateFileV2Args;
import microgram.api.java.Media;
import microgram.api.java.Result;
import utils.*;

public class DropboxMedia implements Media {
	
	private static final String apiKey = "586o9bg81qssp4s";
	private static final String apiSecret = "pk65i3l0xn7u8uj";
	private static final String accessTokenStr = "mFHHflVgCUAAAAAAAAAACjyhbnnYZ_LYPGYDm4_b_zEXTQBL5Nph4q9DhzNy4X9u";
	
	protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	protected static final String OCTETSTREAM_CONTENT_TYPE = "application/octet-stream";

	private static final String CREATE_FILE_V2_URL = "https://content.dropboxapi.com/2/files/upload";
	private static final String DELETE_FILE_V2_URL = "https://api.dropboxapi.com/2/files/delete";
	private static final String DOWNLOAD_FILE_V2_URL = "https://content.dropboxapi.com/2/files/download";
	
	private static final String DROPBOX_API_ARG = "Dropbox-API-Arg";
	private static final String CONTENT_TYPE = "Content-Type";
	
	private static final String ROOT_DIR = "/tmp/microgram/";
	private static final String MEDIA_EXTENSION = ".jpg";
	
	protected OAuth20Service service;
	protected OAuth2AccessToken accessToken;
	
	public DropboxMedia() {
		this.service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
		this.accessToken = new OAuth2AccessToken(accessTokenStr);
	}
	
	@Override
	public Result<String> upload(byte[] bytes) {
		try {
			String id = Hash.of(bytes);
			OAuthRequest createFile = new OAuthRequest(Verb.POST, CREATE_FILE_V2_URL);
			createFile.addHeader(DROPBOX_API_ARG, JSON.encode(new CreateFileV2Args(ROOT_DIR + id + MEDIA_EXTENSION)));
			createFile.addHeader(CONTENT_TYPE, OCTETSTREAM_CONTENT_TYPE);
			createFile.setPayload(bytes);
			
			service.signRequest(accessToken, createFile);
			Response r = service.execute(createFile);
			
			if (r.getCode() == 409) {
				System.err.println("Dropbox file already exists");
				return Result.error(Result.ErrorCode.CONFLICT);
			} else if (r.getCode() == 200) {
				System.err.println("File was uploaded with success");
				return Result.ok(id);
			} else {
				System.err.println("Unexpected error HTTP: " + r.getCode());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<byte[]> download(String id) {
		try {
			OAuthRequest downloadFile = new OAuthRequest(Verb.POST, DOWNLOAD_FILE_V2_URL);
			downloadFile.addHeader(DROPBOX_API_ARG, JSON.encode(new AccessFileV2Args(ROOT_DIR + id + MEDIA_EXTENSION)));
			downloadFile.addHeader(CONTENT_TYPE, "text/plain; charset=utf-8");

			service.signRequest(accessToken, downloadFile);
			Response r = service.execute(downloadFile);
			
			if (r.getCode() == 404) {
				System.err.println("Dropbox file does not exist");
				return Result.error(Result.ErrorCode.NOT_FOUND);
			} else if (r.getCode() == 200) {
				System.err.println("File was downloaded with success");
				DAR s = JSON.decode(r.getHeader("dropbox-api-result"), DAR.class);
				int size = s.size;
				InputStream is = r.getStream();
				int i = 0;
				int l = size;
				byte[] data = new byte[size];
				while(i < size) {
					i += is.read(data, i, l);
					l = size - i;
				}
				return Result.ok(data);
			} else {
				System.err.println("Unexpected error HTTP: " + r.getCode());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> delete(String id) {
		try {
			OAuthRequest deleteFile = new OAuthRequest(Verb.POST, DELETE_FILE_V2_URL);
			deleteFile.addHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);

			deleteFile.setPayload(JSON.encode(new AccessFileV2Args(ROOT_DIR + id + MEDIA_EXTENSION)));

			service.signRequest(accessToken, deleteFile);
			Response r = service.execute(deleteFile);

			if (r.getCode() == 404 || r.getCode() == 409) {
				System.err.println("Dropbox file does not exist");
				return Result.error(Result.ErrorCode.NOT_FOUND);
			} else if (r.getCode() == 200) {
				System.err.println("File was deleted with success");
				return Result.ok();
			} else {
				System.err.println("Unexpected error HTTP: " + r.getCode());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}
}

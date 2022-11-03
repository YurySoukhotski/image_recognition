package com.example.imrecognition.helper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class OmnIntegrationHelper
{
	public static final String COARSE = "COARSE";
	private static final String NO_PREVIEW_SOURCE = "assets/file_s.png";
	private static final String ERROR_PREVIEW_SOURCE = "assets/file_err.gif";
	private static final Logger logger = LogManager.getLogger(OmnIntegrationHelper.class);

	public static byte[] downloadUrl(URL toDownload)
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try
		{
			byte[] chunk = new byte[4096];
			int bytesRead;
			InputStream stream = toDownload.openStream();
			while (( bytesRead = stream.read(chunk) ) > 0)
			{
				outputStream.write(chunk, 0, bytesRead);
			}
		}
		catch (IOException e)
		{
			logger.error("Unexpected error during download", e);
		}
		return outputStream.toByteArray();
	}

	public static byte[] downloadFile(String fileName)
	{
		try
		{
			return Files.readAllBytes(new File(fileName).toPath());
		}
		catch (IOException e)
		{
			logger.error("Unexpected error during read file", e);
			return null;
		}
	}

	/**
	 * Generate url for coarse preview
	 * @return
	 */
	private static String encrypt(String msg)
	{
		String s = getEncryptString(DigestUtils.md5Hex(msg), 5);
		return msg + ":" + DigestUtils.md5Hex(s);
	}

	private static String getEncryptString(String msg, Integer k)
	{
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < k; ++i)
		{
			s.append(msg);
		}
		return s.toString();
	}

	public static String buildPreviewSourceUrl(String guid, String contentType, String serverURL)
	{
		Boolean clientWatermarking = false;
		String type = COARSE;
		return serverURL
			+ ( guid != null ? "preview?previewID=" + getPreviewID(guid, clientWatermarking, type, contentType)
				+ "&noPreviewImage=" + ERROR_PREVIEW_SOURCE : NO_PREVIEW_SOURCE );
	}

	private static String getPreviewID(String guid, Boolean clientWatermarking, String type, String contentType)
	{
		return encrypt("" + guid + ":" + ( clientWatermarking ? 1 : 0 ) + ":" + type + ":" + contentType);
	}
}

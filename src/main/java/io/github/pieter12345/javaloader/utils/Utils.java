package io.github.pieter12345.javaloader.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Utils class.
 * This class contains useful methods that do not belong elsewhere.
 * @author P.J.S. Kools
 * @since 05-01-2017
 */
public abstract class Utils {
	
	/**
	 * removeFile method.
	 * Removes the given file or directory (including subdirectories).
	 * @param file - The File to remove.
	 * @return True if the removal was successful. False if one or more files could not be removed.
	 *  If the file does not exist, true is returned.
	 */
	public static boolean removeFile(File file) {
		boolean ret = true;
		if(file.isDirectory()) {
			File[] localFiles = file.listFiles();
			if(localFiles == null) {
				return false; // IOException occurred or the file was not a directory.
			}
			for(File localFile : localFiles) {
				ret &= removeFile(localFile); // Recursive call.
			}
		}
		return file.delete() && ret;
	}
	
	/**
	 * getStacktrace method.
	 * @param throwable - The Throwable for which to create the stacktrace String.
	 * @return The stacktrace printed when "throwable.printStackTrace()" is called.
	 */
	public static String getStacktrace(Throwable throwable) {
		if(throwable == null) {
			throw new NullPointerException("Exception argument is null.");
		}
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try {
			throwable.printStackTrace(new PrintStream(outStream, true, StandardCharsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(); // Never happens.
		}
		return new String(outStream.toByteArray(), StandardCharsets.UTF_8);
	}
}

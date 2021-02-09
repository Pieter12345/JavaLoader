package io.github.pieter12345.javaloader.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

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
	 * Copies toCopy to the target directory. If toCopy is a directory, its contents will also be copied.
	 * WARNING: When the target location already exists, it will be overwritten. Also, copying a file to itself
	 * causes a file to become empty.
	 * Example invocation: copyFile(".../file", ".../dir2") would copy 'file' to 'dir2/file'.
	 * @param toCopy - The file or directory to copy.
	 * @param targetDir - The target directory to copy toCopy to.
	 * @throws IOException When an I/O error occurs during copying. Some files might already be copied when
	 * @throws FileNotFoundException When the toCopy file does not exist.
	 * this is thrown.
	 */
	public static void copyFile(File toCopy, File targetDir) throws IOException, FileNotFoundException {
		File target = new File(targetDir.getAbsoluteFile(), toCopy.getName());
		if(toCopy.isDirectory()) {
			target.mkdir();
			for(File file : toCopy.listFiles()) {
				copyFile(file, target);
			}
		} else if(toCopy.isFile()) {
			Files.copy(toCopy.toPath(), target.toPath(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		} else {
			throw new FileNotFoundException("File to copy does not exist: " + toCopy.getAbsolutePath());
		}
	}
	
	/**
	 * Reads the given file and returns its contents as a string, using the provided charset for conversion.
	 * @param file - The file to read.
	 * @param charset - The charset used to convert the file bytes to characters.
	 * @return The file contents as a string, or null if the file does not exist.
	 * @throws IOException If an I/O error occurs while reading the file.
	 */
	public static String readFile(File file, Charset charset) throws IOException {
		if(!file.isFile()) {
			return null;
		}
		byte[] fileBytes = new byte[(int) file.length()];
		FileInputStream inStream = new FileInputStream(file);
		inStream.read(fileBytes);
		inStream.close();
		return new String(fileBytes, charset);
	}
	
	/**
	 * Reads the given file and returns its contents as a string, using the
	 * {@link StandardCharsets#UTF_8} for conversion.
	 * @param file - The file to read.
	 * @return The file contents as a string, or null if the file does not exist.
	 * @throws IOException If an I/O error occurs while reading the file.
	 */
	public static String readFile(File file) throws IOException {
		return readFile(file, StandardCharsets.UTF_8);
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
	
	/**
	 * Converts a File to an URL. This method can be convenient as alterntive to catching a never-thrown Exception.
	 * @param file - The file to convert.
	 * @return The URL of the file, or null if the file was null.
	 */
	public static URL fileToURL(File file) {
		try {
			return (file == null ? null : file.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Never happens.");
		}
	}
	
	/**
	 * Glues elements in an iterable together into a string with the given glue.
	 * @param iterable - The iterable containing the elements to generate a string with.
	 * @param stringifier - Used to convert object T into a string.
	 * @param glue - The glue used between elements in the iterable.
	 * @return The glued string(e1+glue+e2+glue+e3 etc) or an empty string if no elements were found.
	 */
	public static <T> String glueIterable(Iterable<T> iterable, Stringifier<T> stringifier, String glue) {
		Iterator<T> it = iterable.iterator();
		if(!it.hasNext()) {
			return "";
		}
		StringBuilder str = new StringBuilder(stringifier.toString(it.next()));
		while(it.hasNext()) {
			str.append(glue).append(stringifier.toString(it.next()));
		}
		return str.toString();
	}
	
	/**
	 * Used to specify how type T should be converted to a String.
	 * @author P.J.S. Kools
	 * @param <T> The type.
	 */
	public static interface Stringifier<T> {
		String toString(T object);
	}
}

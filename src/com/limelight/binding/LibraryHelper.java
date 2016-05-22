package com.limelight.binding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.UUID;

public class LibraryHelper {
	private static final HashSet<String> avcDependencies = new HashSet<String>();
	private static final boolean needsDependencyExtraction;
	private static final String libraryExtractionFolder;

	private static boolean librariesExtracted = false;
	
	static {
		needsDependencyExtraction = System.getProperty("os.name").contains("Windows");
		libraryExtractionFolder = System.getProperty("java.io.tmpdir", ".") + File.separatorChar + UUID.randomUUID().toString();
		
		// AVC dependencies
		if (System.getProperty("os.name").contains("Windows")) {
			avcDependencies.add("avutil-54");
			avcDependencies.add("swresample-1");
//			avcDependencies.add("avdevice-56");
			avcDependencies.add("swscale-3");
			avcDependencies.add("avcodec-56");
//			avcDependencies.add("avfilter-5");
//			avcDependencies.add("avformat-56");

			avcDependencies.add("pthreadVC2");
		}
		
		// The AVC JNI library itself
		avcDependencies.add("nv_avc_dec");
	}
	
	public static void loadNativeLibrary(String libraryName) {
		if (librariesExtracted && avcDependencies.contains(libraryName)) {
			System.load(libraryExtractionFolder + File.separatorChar + System.mapLibraryName(libraryName));
		}
		else {
			System.loadLibrary(libraryName);
		}
	}

	public static void prepareNativeLibraries() {
		if (!needsDependencyExtraction || !isRunningFromJar()) {
			return;
		}
		
		// Create the native library folder
		new File(libraryExtractionFolder).mkdirs();
		
		try {
			for (String dependency : avcDependencies) {
				extractNativeLibrary(dependency);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		librariesExtracted = true;
	}
	
	public static String getRunningPlatformString() {
		String osName = System.getProperty("os.name", "");
		String jreArch = System.getProperty("os.arch", "");
		StringBuilder str = new StringBuilder();
		
		// OS X is a single-arch platform
		if (osName.contains("Mac OS X")) {
			return "osx";
		}
		
		// Windows and Linux are dual-arch
		if (osName.contains("Windows")) {
			str.append("win");
		}
		else if (osName.contains("Linux")) {
			str.append("lin");
		}
		else {
			return null;
		}
		
		// Check OS arch
		if (jreArch.equalsIgnoreCase("x86") ||
			jreArch.equalsIgnoreCase("i386") ||
			jreArch.equalsIgnoreCase("i686")) {
			str.append("32");
		}
		else {
			str.append("64");
		}
		
		return str.toString();
	}
	
	public static String getLibraryPlatformString() throws IOException {
		BufferedReader reader;
		
		if (isRunningFromJar()) {
			reader = getPlatformInJar();
		}
		else {
			reader = getPlatformUnpackaged();
		}
		
		return reader.readLine();
	}
	
	private static BufferedReader getPlatformUnpackaged() throws FileNotFoundException {
		String platformPath = System.getProperty("java.library.path", "") + File.separator + "platform";
		InputStream inStream = new FileInputStream(new File(platformPath));
		return new BufferedReader(new InputStreamReader(inStream));
	}
	
	private static BufferedReader getPlatformInJar() throws FileNotFoundException {
		InputStream platformFile = new Object().getClass().getResourceAsStream("/binlib/platform");
		if (platformFile == null) {
			throw new FileNotFoundException("Unable to find platform designation in JAR");
		}
		
		return new BufferedReader(new InputStreamReader(platformFile));
	}
	
	private static void extractNativeLibrary(String libraryName) throws IOException {
		// convert general library name to platform-specific name
		libraryName = System.mapLibraryName(libraryName);
		
		InputStream resource = new Object().getClass().getResourceAsStream("/binlib/"+libraryName);
		if (resource == null) {
			throw new FileNotFoundException("Unable to find native library in JAR: "+libraryName);
		}
		File destination = new File(libraryExtractionFolder+File.separatorChar+libraryName);
		
		// this will only delete it if it exists, and then create a new file
		destination.delete();
		destination.createNewFile();
		
		// schedule the temporary file to be deleted when the program exits
		destination.deleteOnExit();
		
		//this is the janky java 6 way to copy a file
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(destination);
			int read;
			byte[] readBuffer = new byte[16384];
			while ((read = resource.read(readBuffer)) != -1) {
				fos.write(readBuffer, 0, read);
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}
	
	public static boolean isRunningFromJar() {
		String classPath = LibraryHelper.class.getResource("LibraryHelper.class").toString();
		return classPath.startsWith("jar:");
	}
}

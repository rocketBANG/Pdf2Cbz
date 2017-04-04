package com.rocketbang;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Reuben on 11/02/2017.
 * Actually converts the PDF files into CBZ
 */
public class ConvertPDF implements Runnable
{
	private JTextArea txta_log;
	private File sourceFile;
	private boolean delete;

	ConvertPDF(File sourceFile, JTextArea log, boolean delete)
	{
		txta_log = log;
		this.sourceFile = sourceFile;
		this.delete = delete;
	}

	public void run()
	{
		if (sourceFile.isFile())
		{
			toCBZ(sourceFile);
		}
		else if(sourceFile.isDirectory())
		{
			ConvertFiles(sourceFile);
		}
		else
		{
			logError("Could not read file: " + sourceFile);
			return;
		}

		String filename = sourceFile.getName(); //Remove the extension
		filename = filename.replace(".pdf", "");
	}

	private void toCBZ(File pdfFile)
	{
		logEvent("Processing File: " + pdfFile.getPath());

		String fileTitle = pdfFile.getName().replace(".pdf", ""); //Strip out the filename with no extension

		File tempDir = ExtractImages(pdfFile, fileTitle);
		if(tempDir != null)
		{
			if(ZipFiles(tempDir, fileTitle))
			{
				if(delete)
				{
					new File(sourceFile.getParent()  + "/pdfs/").mkdir();
					pdfFile.renameTo(new File(sourceFile.getParent() + "/pdfs/" + pdfFile.getName()));
				}
			}
		}
	}

	//Extracts the images in a pdf file into an temporary folder, ready for zipping
	//Returns the temp directory where the images are currently stored
	private File ExtractImages(File pdfFile, String fileTitle)
	{
		PdfReader reader;
		try
		{
			reader = new PdfReader(pdfFile.getPath());
		}
		catch (IOException e)
		{
			logError("Could not load PDF file: " + pdfFile);
			e.printStackTrace();
			return null;
		}

		PdfReaderContentParser parser = new PdfReaderContentParser(reader);

		File tempDir = new File(pdfFile.getParent() + "/" + fileTitle + "tmp");

		//Create temporary directory
		try
		{
			Files.createDirectory(tempDir.toPath());
		}
		catch (IOException e)
		{
			logError("Could not create output directory");
			e.printStackTrace();
		}

		//Setup render listener to output to temporary directory
		ImageRenderListener listener = new ImageRenderListener(tempDir.toString() + "/" + fileTitle + "_Page%03d.%s");

		//Read all the pages
		try
		{
			for (int i = 1; i <= reader.getNumberOfPages(); i++)
			{
				parser.processContent(i, listener);
			}
		}
		catch (IOException e)
		{
			logError("Could not load PDF file: " + pdfFile);
			e.printStackTrace();
			reader.close();
			return null;
		}

		reader.close();
		return tempDir;
	}

	private boolean ZipFiles(File filesLocation, String fileTitle)
	{
		try
		{
			File[] allFiles = filesLocation.listFiles();

			if (allFiles != null)
			{
				String fileTarget = filesLocation.getParent() + "/" + fileTitle + ".cbz";
				OutputStream out = new FileOutputStream(fileTarget);
				ZipOutputStream zOut = new ZipOutputStream(new BufferedOutputStream(out));
				for (int i = 0; i < allFiles.length; i++)
				{
					if (allFiles[i].isFile() && (allFiles[i].getName().endsWith(".jpg") || allFiles[i].getName().endsWith(".jpeg") || allFiles[i].getName().endsWith(".png")))
					{
						File input = allFiles[i];
						FileInputStream fis = new FileInputStream(input);
						ZipEntry ze = new ZipEntry(input.getName());
//						System.out.println("Zipping the file: " + input.getName());
						zOut.putNextEntry(ze);
						byte[] tmp = new byte[4 * 1024];
						int size = 0;
						while ((size = fis.read(tmp)) != -1)
						{
							zOut.write(tmp, 0, size);
						}
						zOut.flush();
						fis.close();
					}
					//delete the image after it is added
					allFiles[i].delete();
				}
				zOut.close();

				//Delete the temp directory
				filesLocation.delete();

				logEvent("File Created: " + fileTarget);
			}
		}
		catch(FileNotFoundException e)
		{
			logError("Could not make zip");
			e.printStackTrace();
			return false;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	//Recursively travels through directories and sends all .pdf files to toCBZ function to convert them
	private void ConvertFiles(File directory)
	{
		File[] allFiles = directory.listFiles();

		if(allFiles != null)
		{
			for (int i = 0; i < allFiles.length; i++)
			{
				if (allFiles[i].isFile())
				{
					if (allFiles[i].getName().endsWith(".pdf"))
					{
						toCBZ(allFiles[i]);
					}
				}
				else if (allFiles[i].isDirectory())
				{
					ConvertFiles(allFiles[i]);
				}
				else
				{
					logError("Could not read file: " + allFiles[i]);
					return;
				}
			}
			logEvent("Finished converting directory");
		}
	}

	private void logEvent(String event)
	{
		txta_log.append("\n" + event);
	}

	private void logError(String error)
	{
		txta_log.append("\n\n" + "ERROR: " + error + "\n");
	}
}

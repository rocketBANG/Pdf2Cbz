package com.rocketbang;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Reuben on 6/02/2017.
 * Main window for converter application
 * Handles the code for the interface and uses a ConvertPDF object to handle the conversion
 */

public class MainWindow
{
	private JPanel MainWindow;
	private JTextField txt_filename;
	private JButton btn_load;
	private JButton btn_convert;
	private JTextArea txta_log;
	private JButton btn_clearlog;
	private JCheckBox chk_delete;
	PrintStream stream;

	public MainWindow()
	{
		btn_load.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF files only", "pdf");

				JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
				fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fileChooser.setFileFilter(pdfFilter);

				int result = fileChooser.showOpenDialog(MainWindow);
				if (result == JFileChooser.APPROVE_OPTION)
				{
					File selectedFile = fileChooser.getSelectedFile();
					txt_filename.setText(selectedFile.getPath());
				}
			}
		});
		btn_clearlog.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				txta_log.setText("");
			}
		});
		btn_convert.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				System.out.println("Checked? " + chk_delete.isSelected());
				ConvertPDF pdfConverter = new ConvertPDF(new File(txt_filename.getText()), txta_log, chk_delete.isSelected());

				(new Thread(pdfConverter)).start();
//                super.mouseClicked(e);
//
//				String filename = new File(path).getName();
//				String fileDir = new File(path).getParentFile().getPath();
//				filename = filename.replace(".pdf", "");
//
//				String outputDir = fileDir + "/tmp";
//				setupDirectory(outputDir);
//				ConvertPDF(filename, path, outputDir);
//
//				ZipFiles(filename, outputDir);
			}
		});
	}

	private void ZipFiles(String filename, String outputDir)
	{
		try
		{
			File[] allFiles = new File(outputDir).listFiles();

			OutputStream out = new FileOutputStream(new File(outputDir).getParentFile().getPath() + "/" + filename + ".cbz");
			ZipOutputStream zOut = new ZipOutputStream(new BufferedOutputStream(out));
			for(int i = 0; i < allFiles.length; i++)
			{
				if(allFiles[i].isFile() && (allFiles[i].getName().endsWith(".jpg") || allFiles[i].getName().endsWith(".jpeg") || allFiles[i].getName().endsWith(".png") ))
				{
					File input = allFiles[i];
					FileInputStream fis = new FileInputStream(input);
					ZipEntry ze = new ZipEntry(input.getName());
					System.out.println("Zipping the file: " + input.getName());
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
				allFiles[i].delete();
			}
			zOut.close();
			System.out.println("Done... Zipped the files...");

			new File(outputDir).delete();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Could not make zip");
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void setupDirectory(String directory)
	{
		Path dirPath = new File(directory).toPath();
		if(!Files.exists(dirPath))
		{
			try
			{
				Files.createDirectory(dirPath);
			} catch (IOException e)
			{
				System.err.println("Could not create output directory");
				e.printStackTrace();
			}
		}
	}

	private void ConvertPDF(String filename, String path, String outputDir)
	{
		PdfReader reader;
		try
		{
			reader = new PdfReader(path);
		}
		catch (IOException e)
		{
			System.err.print("Could not open the file");
			e.printStackTrace();
			return;
		}

		PdfReaderContentParser parser = new PdfReaderContentParser(reader);
		ImageRenderListener listener = new ImageRenderListener(outputDir + "/" + filename + "_Page%03d.%s");
		try
		{
			for (int i = 1; i <= reader.getNumberOfPages(); i++)
			{
				parser.processContent(i, listener);
			}
		}
		catch (IOException e)
		{
			System.err.print("Error reading file");
			e.printStackTrace();
		}

		reader.close();
	}

	public static void main(String[] args)
	{
		try
		{
			// Set the Look and Feel of the application to the operating
			// system's look and feel.
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e)
		{
		} catch (InstantiationException e)
		{
		} catch (IllegalAccessException e)
		{
		} catch (UnsupportedLookAndFeelException e)
		{
		}

		JFrame frame = new JFrame("PDF to CBZ Converter");

		frame.setContentPane(new MainWindow().MainWindow);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);


	}


}

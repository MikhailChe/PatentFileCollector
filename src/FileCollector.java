import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileCollector {
	public static void main(String[] args) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Откуда брать файлы?");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setCurrentDirectory(new File(new File(".").getAbsolutePath()));

		final List<File> inputDirectories = new ArrayList<>();

		AbstractListModel<File> model = new AbstractListModel<File>() {

			@Override
			public int getSize() {
				return inputDirectories.size();
			}

			@Override
			public File getElementAt(int index) {
				return inputDirectories.get(index);
			}
		};

		JList<File> directoriesListGUI = new JList<>(model);

		JFrame frame = new JFrame("Патентомат");

		JPanel twoColumns = new JPanel(new GridLayout(0, 2));
		frame.setContentPane(twoColumns);

		JPanel directoriesColumn = new JPanel(new BorderLayout());
		twoColumns.add(directoriesColumn);

		directoriesColumn.add(directoriesListGUI);

		JPanel dirButtonsGrp = new JPanel(new GridLayout(0, 2, 16, 16));
		directoriesColumn.add(dirButtonsGrp, BorderLayout.SOUTH);

		JButton oneMoreFolder = new JButton("Ещё папочку ...");
		dirButtonsGrp.add(oneMoreFolder);
		oneMoreFolder.addActionListener((a) -> {
			int dialog = fileChooser.showOpenDialog(null);

			if (dialog != JFileChooser.APPROVE_OPTION)
				return;
			File[] inDirs = fileChooser.getSelectedFiles();

			if (inDirs == null)
				return;

			for (File inDir : inDirs) {
				if (inDir.isDirectory()) {
					inputDirectories.add(inDir);

					for (ListDataListener ldl : model.getListDataListeners()) {
						ldl.intervalAdded(new ListDataEvent(model, ListDataEvent.INTERVAL_ADDED, 0, model.getSize()));
					}
					frame.pack();
				}
			}

			fileChooser.setSelectedFile(null);
			fileChooser.setSelectedFiles(null);
		});

		JButton deleteFolder = new JButton("Убери её");
		dirButtonsGrp.add(deleteFolder);
		deleteFolder.addActionListener((a) -> {
			if (directoriesListGUI.getSelectedIndex() >= 0) {
				inputDirectories.remove(directoriesListGUI.getSelectedIndex());
				for (ListDataListener ldl : model.getListDataListeners()) {
					ldl.intervalRemoved(new ListDataEvent(model, ListDataEvent.INTERVAL_REMOVED, 0, model.getSize()));
				}
			}
		});

		JPanel extensionsColumn = new JPanel(new BorderLayout());
		twoColumns.add(extensionsColumn);

		JTextArea extension = new JTextArea(".txt\r\n.css\r\n.html");
		extensionsColumn.add(extension);

		JButton GOGOGO = new JButton("<html>По<b>на</b>ехали</html>");
		extensionsColumn.add(GOGOGO, BorderLayout.SOUTH);

		GOGOGO.addActionListener((a) -> {
			fileChooser.setDialogTitle("Куда сохранить");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

			fileChooser.resetChoosableFileFilters();
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setFileFilter(new FileNameExtensionFilter("Html files", "html"));

			File outputFile = null;
			while (outputFile == null) {
				fileChooser.setMultiSelectionEnabled(false);
				int dialog = fileChooser.showSaveDialog(null);
				if (dialog != JFileChooser.APPROVE_OPTION)
					return;
				outputFile = fileChooser.getSelectedFile();
				if (outputFile != null && outputFile.exists() && !outputFile.isFile()) {
					outputFile = null;
				}
			}
			synchronized (postfixes) {
				postfixes.clear();
				for (String suffix : extension.getText().split("\n")) {
					postfixes.add(suffix.trim());
				}
			}

			try (BufferedWriter outputChannel = Files
					.newBufferedWriter(outputFile.toPath(), Charset.forName("UTF-8"), StandardOpenOption.WRITE,
							StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
				outputChannel.write("<html>" + "<head>" + "<meta charset=\"UTF-8\">" + "</head>" + "<body>");
				for (File inDir : inputDirectories)
					appendToChannel(outputChannel, inDir);
				outputChannel.write("</body></html>");

			} catch (IOException e) {
				e.printStackTrace();
			}

		});

		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}

	static List<String> postfixes = new ArrayList<>();

	static boolean endsWithAny(String input, List<String> postfix) {
		for (String fix : postfix) {
			if (input.endsWith(fix))
				return true;
		}
		return false;
	}

	public static void appendToChannel(BufferedWriter bWr, File folder) {
		File[] list = folder.listFiles();
		for (File dir : list) {
			if (dir.isDirectory()) {
				appendToChannel(bWr, dir);
			}
		}
		for (File f : list) {
			String name = f.getName();
			if (endsWithAny(name, postfixes)) {
				try {
					appendFile(bWr, f);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void appendFile(BufferedWriter bWr, File file) throws UnsupportedEncodingException, IOException {
		bWr.write("<div>");
		bWr.write("<div>");
		bWr.write("<b>");
		bWr.write("Файл " + file.getName());
		bWr.write("</b>");
		bWr.write("</div>");

		bWr.newLine();
		bWr.write("<div>");
		bWr.write("<code>");
		try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			String ss;
			while ((ss = br.readLine()) != null) {
				bWr
						.write(ss
								.replaceAll("&", "&amp;")
								.replaceAll("<", "&lt;")
								.replaceAll(">", "&gt;")
								.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
								.replaceAll("    ", "&nbsp;&nbsp;&nbsp;&nbsp;"));
				bWr.write("<br>");
				bWr.newLine();
			}
		}
		bWr.write("</code>");
		bWr.write("</div>");
		bWr.write("</div>");
		bWr.newLine();
		bWr.newLine();
	}
}

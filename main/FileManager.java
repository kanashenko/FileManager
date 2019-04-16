package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import persistence.DBWorker;

public class FileManager extends JPanel {

	private JPanel mainPanel = this;
	private JButton backButton;
	private JLabel pathLabel;
	private JSplitPane splitPane;
	private JList<String> list;
	private DefaultListModel<String> listModel;
	private JPanel view;
	private JTextArea text;
	private JLabel imageLabel;
	private JButton copyButton;
	private JButton deleteButton;

	private List<String> currentDirs = new ArrayList<>();
	private final String root = "C:\\Users\\Zver\\Desktop";
	private String currentPath = root;
	
	private DBWorker dbWorker = new DBWorker();
	
	private enum Action{
		LOOK, COPY, DELETE
	}
	
	private void recordAction(Action action, String fileName, String dirName){
		String SQL = "INSERT INTO records (action, file, directory) values (?,?,?)";
		dbWorker.insert(SQL, action.toString(), fileName, dirName);
	}
	
	private void loadContents(File file) {
		currentDirs.clear();
		List<File> list = Arrays.asList(file.listFiles());
		list.stream().filter(f -> f.isDirectory()).map(f -> f.getName()).forEach(currentDirs::add);
		list.stream().filter(f -> f.isFile()).map(f -> f.getName()).forEach(currentDirs::add);
	}

	abstract class MyDialog extends JDialog {
		public MyDialog(Frame frame, String title, boolean modal) {
			super(frame, title, modal);
		}

		boolean result = false;

		boolean getResult() {
			return result;
		}
	}

	public class ConfirmDialog extends MyDialog {
		public ConfirmDialog(Frame frame, String title, boolean modal, String message) {
			super(frame, title, modal);
			Point p = new Point(683, 384);
			setLocation(p.x, p.y);

			JPanel messagePane = new JPanel();
			messagePane.add(new JLabel(message));
			getContentPane().add(messagePane);

			JPanel buttonPane = new JPanel();
			JButton confirmButton = new JButton("OK");
			confirmButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					result = true;
					dispose();
				}
			});
			buttonPane.add(confirmButton);
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			buttonPane.add(cancelButton);

			getContentPane().add(buttonPane, BorderLayout.PAGE_END);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			pack();
			setVisible(true);
		}
	}

	public class ProhibitedDialog extends JDialog {
		public ProhibitedDialog(Frame frame, String title, boolean modal, String message) {
			super(frame, title, modal);
			Point p = new Point(683, 384);
			setLocation(p.x, p.y);
			JPanel messagePane = new JPanel();
			messagePane.add(new JLabel(message));
			getContentPane().add(messagePane);
			JPanel buttonPane = new JPanel();
			JButton confirmButton = new JButton("OK");
			confirmButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			buttonPane.add(confirmButton);
			getContentPane().add(buttonPane, BorderLayout.PAGE_END);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			pack();
			setVisible(true);
		}
	}

	private void showProhibitedDialog(String message) {
		Frame parentFrame = (Frame) SwingUtilities.windowForComponent(mainPanel);
		new ProhibitedDialog(parentFrame, "", true, message);
	}

	private boolean showConfirmDialog(String message) {
		Frame parentFrame = (Frame) SwingUtilities.windowForComponent(mainPanel);
		ConfirmDialog dialog = new ConfirmDialog(parentFrame, "", true, message);
		return dialog.getResult();
	}

	private class CopyButtonActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String selected = (String) list.getSelectedValue();
			if (selected != null && currentPath.equals("C:\\Users\\Zver\\Desktop")) {
				boolean result = showConfirmDialog("Confirm copy");
				if (result) {
					Path targetPath = Paths.get(currentPath + "/" + "copy_" + selected);
					Path sourcePath = Paths.get(currentPath + "/" + selected);
					try {
						Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
									throws IOException {
								Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
									throws IOException {
								Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
								return FileVisitResult.CONTINUE;
							}
						});
						loadContents(new File(currentPath));
						updateListModel();
						recordAction(Action.COPY, selected, currentPath);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} 
			}else {
				showProhibitedDialog("Can't copy in this directory");
			}
		}
	}

	private class DeleteButtonActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String selected = (String) list.getSelectedValue();
			if (selected != null && currentPath.equals("C:\\Users\\Zver\\Desktop")) {
				boolean result = showConfirmDialog("Are you sure to delete?");
				if (result) {
					Path sourcePath = Paths.get(currentPath + "/" + selected);
					try {
						Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
							@Override
							   public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
							       Files.delete(file);
							       return FileVisitResult.CONTINUE;
							   }
							   @Override
							   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							       Files.delete(dir);
							       return FileVisitResult.CONTINUE;
							   }
							});
						loadContents(new File(currentPath));
						updateListModel();
						recordAction(Action.DELETE, selected, currentPath);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} 
			}else {
				showProhibitedDialog("Can't delete in this directory");
			}
		}
	}

	FileManager() {
		// Left
		File file = new File(currentPath);
		loadContents(file);
		listModel = new DefaultListModel<>();
		updateListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				JList list = (JList) evt.getSource();
				if (evt.getClickCount() == 2) {
					processSelected((String) list.getSelectedValue());
				}
			}
		});

		JScrollPane listScrollPane = new JScrollPane(list);

		// Right
		view = new JPanel();
		text = new JTextArea();
		text.setVisible(false);
		view.add(text);
		imageLabel = new JLabel();
		imageLabel.setVisible(false);
		view.add(imageLabel);
		JScrollPane viewScrollPane = new JScrollPane(view);
		Dimension minimumSize = new Dimension(100, 50);
		listScrollPane.setMinimumSize(minimumSize);
		viewScrollPane.setMinimumSize(minimumSize);

		// Create a split pane with the two scroll panes in it.
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, viewScrollPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(200);
		splitPane.setPreferredSize(new Dimension(1200, 700));

		pathLabel = new JLabel("Current");
		pathLabel.setText(currentPath);

		backButton = new JButton("..");
		backButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = new File(currentPath);
				if (f.getParent() != null) {
					currentPath = f.getParent();
					openDirectory(new File(currentPath));
				}
			}
		});

		copyButton = new JButton("copy");
		copyButton.addActionListener(new CopyButtonActionListener());
		deleteButton = new JButton("delete");
		deleteButton.addActionListener(new DeleteButtonActionListener());

		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
		constraints.fill = java.awt.GridBagConstraints.BOTH;
		// First Row
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.gridx = 0;
		constraints.gridy = 0;
		add(backButton, constraints);
		constraints.gridx = 1;
		add(pathLabel, constraints);
		// Second Row
		constraints.weightx = 1;
		constraints.weighty = 1;
		// constraints.gridx = 0;
		constraints.gridy = 1;
		add(splitPane, constraints);
		// Third Row
		constraints.fill = java.awt.GridBagConstraints.VERTICAL;
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.gridx = 0;
		constraints.gridy = 2;
		add(copyButton, constraints);
		constraints.gridx = 1;
		add(deleteButton, constraints);
	}

	private void updateListModel() {
		listModel.clear();
		for (int i = 0; i < currentDirs.size(); i++) {
			listModel.addElement(currentDirs.get(i));
		}
	}

	private void processSelected(String selected) {
		File file = new File(currentPath + "/" + selected);
		if (file.isDirectory()) {
			openDirectory(file);
		} else if (selected.endsWith(".png") || selected.endsWith(".jpeg") || selected.endsWith(".jpg")) {
			showImage(file.getAbsolutePath());
			recordAction(Action.LOOK, selected, currentPath);
		} else {
			readTxt(file.getAbsolutePath());
			recordAction(Action.LOOK, selected, currentPath);
		}
	}

	private void readTxt(String path) {
		text.setText("");
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = br.readLine()) != null) {
				text.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		text.setVisible(true);
		imageLabel.setVisible(false);
	}

	private void openDirectory(File file) {
		text.setText("");
		text.append("Directory: " + file.getAbsolutePath());
		text.append("\n" + "Last Modified : " + new Date(file.lastModified()));
		text.append("\n" + "Directories : " + countContents(file, File::isDirectory));
		text.append("\n" + "Files: " + +countContents(file, File::isFile));
		imageLabel.setVisible(false);
		text.setVisible(true);
		currentPath = file.getAbsolutePath();
		pathLabel.setText(currentPath);
		loadContents(file);
		updateListModel();
	}

	private long countContents(File file, Predicate<File> predicate) {
		long count = Arrays.stream(file.listFiles()).filter(predicate).count();
		return count;
	}

	protected void showImage(String path) {
		ImageIcon icon = new ImageIcon(path);
		imageLabel.setIcon(icon);
		text.setVisible(false);
		imageLabel.setVisible(true);
	}

	private static void createAndShowGUI() {
		JFrame frame = new JFrame("FileManager");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JComponent newContentPane = new FileManager();
		newContentPane.setOpaque(true);
		frame.setContentPane(newContentPane);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) throws IOException {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

}

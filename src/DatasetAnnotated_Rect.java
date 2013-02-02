import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class DatasetAnnotated_Rect extends JFrame implements DropTargetListener {

	private static final long serialVersionUID = 1L;
	private final static String[] categories = { "Ham", "Pan", "Frying Pan",
			"Knife", "Tuner", "Chopsticks", "Salt Box", "Vegetable Oil Bottle",
			"Bowl", "Eggs" };

	private Image offScreenImage = null; // avoid flicker
	private Graphics gOffScreenGraphics;

	private BufferedImage initImage = null;
	private String curFileName = null;
	private LinkedList<File> file_list = new LinkedList<File>();
	private int curFileIdx = 0;

	private Vector<Annotation> annos = new Vector<Annotation>();
	private Annotation curAnno = new Annotation();

	private boolean isDrawing = false;

	private String outDir = "/media/work/dataset/kitchen_scene/object";

	public DatasetAnnotated_Rect() {
		setTitle("Dataset Annotation");
		setSize(300, 300);
		setLocation(100, 100);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		addMouseListener(new MouseAdapter() {
			// @Override
			// public void mouseClicked(MouseEvent e) {
			// System.out.println("mouseClicked");
			//
			// if (e.getButton() != MouseEvent.BUTTON1 || initImage == null
			// || curAnno.x[0] == -1)
			// return;
			// if (e.getClickCount() == 2) {
			// System.out.println("mouseClicked 2");
			// }
			//
			// repaint();
			// }

			@Override
			public void mousePressed(MouseEvent e) {
				// System.out.println("mousePressed");

				if (e.getButton() != MouseEvent.BUTTON1 || initImage == null)
					return;

				isDrawing = true;

				curAnno.x[0] = curAnno.x[1] = e.getX();
				curAnno.y[0] = curAnno.y[1] = e.getY();
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// System.out.println("mousePressed");

				if (e.getButton() != MouseEvent.BUTTON1 || initImage == null)
					return;

				isDrawing = false;

				repaint();
			}

		});

		addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(MouseEvent e) {
				// System.out.println("mouseDragged");

				if (!isDrawing || initImage == null)
					return;

				curAnno.x[1] = Math.min(initImage.getWidth(null) - 1, e.getX());
				curAnno.x[1] = Math.max(0, curAnno.x[1]);

				curAnno.y[1] = Math
						.min(initImage.getHeight(null) - 1, e.getY());
				curAnno.y[1] = Math.max(0, curAnno.y[1]);

				repaint();
			}
		});

		addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {

				switch (Character.toLowerCase(e.getKeyChar())) {

				// undo last annotation
				case 'u':
					if (annos.size() != 0)
						curAnno = annos.remove(annos.size() - 1);
					break;

				// save current annotation
				case 's':
					saveCurAnno();
					break;

				// complete current image annotations
				case 'c':
					saveCurAnno();
					saveAnnotation();
					break;

				// previous image
				case 'a':
					setNextPic(0);
					break;

				// next image
				case 'd':
					setNextPic(1);
					break;

				default:
					char c = e.getKeyChar();
					if (c >= '0' && c <= '9') {
						curAnno.annoID = c - '0';
					}
				}
				DatasetAnnotated_Rect.this.repaint();
			}
		});

		new DropTarget(this, this);

		setVisible(true);
	}

	private void saveCurAnno() {
		if (curAnno.x[0] != -1) {
			if (curAnno.x[0] > curAnno.x[1]) {
				curAnno.x[0] ^= curAnno.x[1];
				curAnno.x[1] ^= curAnno.x[0];
				curAnno.x[0] ^= curAnno.x[1];
			}

			if (curAnno.y[0] > curAnno.y[1]) {
				curAnno.y[0] ^= curAnno.y[1];
				curAnno.y[1] ^= curAnno.y[0];
				curAnno.y[0] ^= curAnno.y[1];
			}

			annos.add(new Annotation(curAnno));
			Arrays.fill(curAnno.x, -1);
			Arrays.fill(curAnno.y, -1);
			isDrawing = false;
		}
	}

	protected void saveAnnotation() {
		try {
			File curFile = file_list.get(curFileIdx);
			if (curFile.getParent() == null
					|| !outDir.equals(curFile.getParentFile().getParent())) {
				File baseFolder = new File(outDir + "/Annotations");
				int count = baseFolder.list().length;
				curFileName = String.format("%011d", count);
			}

			FileOutputStream fos = new FileOutputStream(outDir
					+ "/Annotations/" + curFileName + ".txt");
			for (int i = 0; i < annos.size(); i++) {
				Annotation tmp = annos.elementAt(i);
				fos.write(String.format("%d %d %d %d %d\n", tmp.annoID,
						tmp.x[0], tmp.y[0], tmp.x[1], tmp.y[1]).getBytes());
			}
			fos.close();

			File outputImage = new File(outDir + "/Images/" + curFileName
					+ ".jpg");
			ImageIO.write(initImage, "JPEG", outputImage);
			file_list.set(curFileIdx, outputImage);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void update(Graphics g) {
		if (initImage == null)
			return;

		if (offScreenImage == null
				|| offScreenImage.getHeight(this) != this.getHeight()
				|| offScreenImage.getWidth(this) != this.getWidth()) {
			offScreenImage = this
					.createImage(this.getWidth(), this.getHeight());
			gOffScreenGraphics = offScreenImage.getGraphics();
		}

		gOffScreenGraphics.drawImage(initImage, 0, 0, this);

		gOffScreenGraphics.setColor(Color.BLUE);
		int x, y, width, height;
		if (curAnno.x[0] != -1) {
			x = Math.min(curAnno.x[0], curAnno.x[1]);
			y = Math.min(curAnno.y[0], curAnno.y[1]);
			width = Math.abs(curAnno.x[0] - curAnno.x[1]);
			height = Math.abs(curAnno.y[0] - curAnno.y[1]);
			gOffScreenGraphics.drawRect(x, y, width, height);
			gOffScreenGraphics.drawString(categories[curAnno.annoID], x, y);
		}

		gOffScreenGraphics.setColor(Color.RED);
		String hasAnnotated = "已标记：";
		for (int i = 0; i < annos.size(); i++) {
			Annotation tmp = annos.elementAt(i);
			gOffScreenGraphics.drawRect(tmp.x[0], tmp.y[0],
					tmp.x[1] - tmp.x[0], tmp.y[1] - tmp.y[0]);
			gOffScreenGraphics.drawString(categories[tmp.annoID], tmp.x[0],
					tmp.y[0]);

			if (i != 0)
				hasAnnotated = hasAnnotated + ", ";
			hasAnnotated = hasAnnotated + categories[tmp.annoID];
		}
		gOffScreenGraphics.drawString(hasAnnotated, 10, 50);
		gOffScreenGraphics.drawString("正在标记：" + categories[curAnno.annoID], 10,
				70);

		g.drawImage(offScreenImage, 0, 0, null);
		g.dispose();
	}

	@Override
	public void paint(Graphics g) {
		update(g);
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void drop(DropTargetDropEvent dtde) {
		try {
			if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				List list = (List) (dtde.getTransferable()
						.getTransferData(DataFlavor.javaFileListFlavor));
				Iterator iterator = list.iterator();
				file_list.clear();
				curFileIdx = -1;
				while (iterator.hasNext()) {
					File file = (File) iterator.next();
					if (file.isDirectory()) {
						addFileToQueue(file);
					} else {
						String fileName = file.getName().toLowerCase();
						if (fileName.endsWith(".jpg")
								|| fileName.endsWith(".png")
								|| file.getName().endsWith(".txt"))
							file_list.add(file);
					}
				}
				annos.clear();
				curAnno.annoID = 1;
				Arrays.fill(curAnno.x, -1);
				Arrays.fill(curAnno.y, -1);
				isDrawing = false;

				setNextPic(1);
				dtde.dropComplete(true);
			} else {
				dtde.rejectDrop();
			}
		} catch (Exception e) {
			e.printStackTrace();
			dtde.rejectDrop();
			JOptionPane.showMessageDialog(this, "Failed to open image");
		}
	}

	// flag == 0 -- previous pic
	// flag == 1 -- next pic
	private void setNextPic(int flag) {
		int tmp = curFileIdx;

		if (flag == 0) {
			if (curFileIdx == 0) {
				JOptionPane.showMessageDialog(this, "The first one!");
				repaint();
				return;
			}
			curFileIdx--;
		} else {
			curFileIdx++;
		}
		boolean ok = false;
		while (!ok) {
			try {
				if (curFileIdx == file_list.size()) {
					curFileIdx = tmp;
					if (curFileIdx == -1)
						return;
					JOptionPane.showMessageDialog(this, "The last one!");
					repaint();
					return;
				}
				File file = file_list.get(curFileIdx);

				initImage = ImageIO.read(file);

				String path = file.getParent();
				curFileName = file.getName();
				setTitle("Dataset Annotation(" + curFileName + ")");

				int idx = curFileName.lastIndexOf('.');
				if (idx != -1)
					curFileName = curFileName.substring(0, idx);

				setSize(initImage.getWidth(null), initImage.getHeight(null));

				readAnnotation(path + "/../Annotations/" + curFileName + ".txt");
				repaint();
				ok = true;

			} catch (Exception e) {
				e.printStackTrace();
				if (flag == 0) {
					if (curFileIdx == 0) {
						return;
					}
					curFileIdx--;
				} else {
					curFileIdx++;
				}
			}
		}
	}

	private void addFileToQueue(File folder) {
		for (File file : folder.listFiles()) {
			if (file.isDirectory())
				addFileToQueue(file);
			else
				file_list.add(file);
		}
	}

	@SuppressWarnings("resource")
	private boolean readAnnotation(String path) {
		File annoFile = new File(path);
		if (!annoFile.exists())
			return false;

		annos.clear();
		curAnno.annoID = 1;
		Arrays.fill(curAnno.x, -1);
		Arrays.fill(curAnno.y, -1);

		
		int choice = JOptionPane.showConfirmDialog(this,
				"Annotation file is found, read it or not?", "tips",
				JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION)
			return false;

		try {
			Scanner scanner = new Scanner(annoFile);
			while (scanner.hasNext()) {
				Annotation tmp = new Annotation();
				tmp.annoID = scanner.nextInt();
				for (int i = 0; i < tmp.x.length; i++) {
					tmp.x[i] = scanner.nextInt();
					tmp.y[i] = scanner.nextInt();
				}
				annos.add(tmp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Invalid annotation file");
		}
		return true;
	}

	public static void main(String[] args) {
		new DatasetAnnotated_Rect();
		// System.out.println(String.format("%011d", 123));
	}

	public class Annotation {

		public Annotation(Annotation tmpAnno) {
			for (int i = 0; i < this.x.length; i++) {
				this.x[i] = tmpAnno.x[i];
				this.y[i] = tmpAnno.y[i];
			}
			this.annoID = tmpAnno.annoID;
		}

		public Annotation() {
		}

		public int annoID;
		public int[] x = new int[2];
		public int[] y = new int[2];
	}

}

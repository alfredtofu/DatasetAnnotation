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
import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DatasetAnnotated_Pt extends JFrame implements DropTargetListener {

	private static final long serialVersionUID = 1L;
	private static Color[] COLOR_TYPE = { Color.RED, Color.GREEN, Color.BLUE };
	private static String[] hands = { "left", "right" };
	private static String[] hand_component = { "upper_arm", "forearm", "hand" };

	private Image offScreenImage = null; // avoid flicker
	private Graphics gOffScreenGraphics;

	private Image initImage = null;
	private String path = null;
	private String fileName = null;
	private LinkedList<File> file_list = new LinkedList<File>();
	private int curFileIdx = 0;

	private int flag = 0; // if shift is pressed, then draw horizontal or
							// vertical line.

	private int[][] xPoints = new int[6][5];
	private int[][] yPoints = new int[6][5];
	private int idx1 = 0;
	private int idx2 = 0;
	private double scale = 2;

	public DatasetAnnotated_Pt() {
		setTitle("Dataset Annotation");
		setSize(300, 300);
		setLocation(100, 100);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (initImage == null || idx1 == 6
						|| e.getButton() == MouseEvent.BUTTON2)
					return;

				if (e.getButton() == MouseEvent.BUTTON3) {
					xPoints[idx1][idx2] = 0;
					yPoints[idx1][idx2] = 0;
					idx2--;
					if (idx2 < 0) {
						Arrays.fill(xPoints[idx1], 0);
						Arrays.fill(yPoints[idx1], 0);
						idx2 = 0;
					}
					return;
				}

				xPoints[idx1][idx2] = e.getX();
				yPoints[idx1][idx2] = e.getY();

				if (xPoints[idx1][idx2] < 0)
					xPoints[idx1][idx2] = 0;
				if (xPoints[idx1][idx2] >= initImage.getWidth(null))
					xPoints[idx1][idx2] = initImage.getWidth(null) - 1;

				if (yPoints[idx1][idx2] < 0)
					yPoints[idx1][idx2] = 0;
				if (yPoints[idx1][idx2] >= initImage.getHeight(null))
					yPoints[idx1][idx2] = initImage.getHeight(null) - 1;

				if (idx2 == 2) {
					// the angle between adjacent edges is 90.
					if (xPoints[idx1][idx2 - 1] == xPoints[idx1][idx2 - 2]) {
						yPoints[idx1][idx2] = yPoints[idx1][idx2 - 1];
					} else if (yPoints[idx1][idx2 - 1] == yPoints[idx1][idx2 - 2]) {
						xPoints[idx1][idx2] = xPoints[idx1][idx2 - 1];
					} else {
						double k = -(xPoints[idx1][idx2 - 2] - xPoints[idx1][idx2 - 1])
								/ (double) (yPoints[idx1][idx2 - 2] - yPoints[idx1][idx2 - 1]);
						double b = yPoints[idx1][idx2 - 1]
								- xPoints[idx1][idx2 - 1] * k;
						if (Math.abs(k) <= 1)
							yPoints[idx1][idx2] = (int) (xPoints[idx1][idx2]
									* k + b);
						else
							xPoints[idx1][idx2] = (int) ((yPoints[idx1][idx2] - b) / k);
					}

					// autogeneration the fourth point
					xPoints[idx1][idx2 + 1] = xPoints[idx1][idx2 - 2]
							+ xPoints[idx1][idx2] - xPoints[idx1][idx2 - 1];
					yPoints[idx1][idx2 + 1] = yPoints[idx1][idx2 - 2]
							+ yPoints[idx1][idx2] - yPoints[idx1][idx2 - 1];

					xPoints[idx1][4] = xPoints[idx1][0];
					yPoints[idx1][4] = yPoints[idx1][0];

					idx1++;
					idx2 = 0;
				} else {
					if (flag == 1 && idx2 == 1) {
						if (xPoints[idx1][idx2] != xPoints[idx1][idx2 - 1]
								&& yPoints[idx1][idx2] != yPoints[idx1][idx2 - 1]) {
							double k = (yPoints[idx1][idx2] - yPoints[idx1][idx2 - 1])
									/ (double) (xPoints[idx1][idx2] - xPoints[idx1][idx2 - 1]);
							if (Math.abs(k) <= 1) {
								yPoints[idx1][idx2] = yPoints[idx1][idx2 - 1];
							} else {
								xPoints[idx1][idx2] = xPoints[idx1][idx2 - 1];
							}
						}
					}

					idx2++;
					xPoints[idx1][idx2] = xPoints[idx1][idx2 - 1];
					yPoints[idx1][idx2] = yPoints[idx1][idx2 - 1];
				}
				DatasetAnnotated_Pt.this.repaint();

				if (idx1 == 6) {
					int choice = JOptionPane.showConfirmDialog(
							DatasetAnnotated_Pt.this,
							"complete! save annotation or not?", "tips",
							JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.YES_OPTION) {
						saveAnnotation();
						if (file_list.size() > 1)
							setNextPic(1);
					}
				}
			}

		});

		addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				if (initImage == null || idx1 == 6)
					return;
				// System.out.println("mouseMoved");
				xPoints[idx1][idx2] = e.getX();
				yPoints[idx1][idx2] = e.getY();

				if (xPoints[idx1][idx2] < 0)
					xPoints[idx1][idx2] = 0;
				if (xPoints[idx1][idx2] >= initImage.getWidth(null))
					xPoints[idx1][idx2] = initImage.getWidth(null) - 1;

				if (yPoints[idx1][idx2] < 0)
					yPoints[idx1][idx2] = 0;
				if (yPoints[idx1][idx2] >= initImage.getHeight(null))
					yPoints[idx1][idx2] = initImage.getHeight(null) - 1;

				if (idx2 > 1) {
					if (xPoints[idx1][idx2 - 1] == xPoints[idx1][idx2 - 2]) {
						yPoints[idx1][idx2] = yPoints[idx1][idx2 - 1];
					} else if (yPoints[idx1][idx2 - 1] == yPoints[idx1][idx2 - 2]) {
						xPoints[idx1][idx2] = xPoints[idx1][idx2 - 1];
					} else {
						double k = -(xPoints[idx1][idx2 - 2] - xPoints[idx1][idx2 - 1])
								/ (double) (yPoints[idx1][idx2 - 2] - yPoints[idx1][idx2 - 1]);
						double b = yPoints[idx1][idx2 - 1]
								- xPoints[idx1][idx2 - 1] * k;
						if (Math.abs(k) <= 1)
							yPoints[idx1][idx2] = (int) (xPoints[idx1][idx2]
									* k + b);
						else
							xPoints[idx1][idx2] = (int) ((yPoints[idx1][idx2] - b) / k);
					}
				} else if (flag == 1 && idx2 == 1) {
					if (xPoints[idx1][idx2] != xPoints[idx1][idx2 - 1]
							&& yPoints[idx1][idx2] != yPoints[idx1][idx2 - 1]) {
						double k = (yPoints[idx1][idx2] - yPoints[idx1][idx2 - 1])
								/ (double) (xPoints[idx1][idx2] - xPoints[idx1][idx2 - 1]);
						if (Math.abs(k) <= 1) {
							yPoints[idx1][idx2] = yPoints[idx1][idx2 - 1];
						} else {
							xPoints[idx1][idx2] = xPoints[idx1][idx2 - 1];
						}
					}
				}

				repaint();
			}
		});

		addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_SHIFT:
					flag = 0;
					break;
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {

				switch (Character.toLowerCase(e.getKeyChar())) {

				case 'u':
					if (idx1 != 0) {
						if (idx2 == 0)
							idx1--;
						idx2 = 0;
						Arrays.fill(xPoints[idx1], 0);
						Arrays.fill(yPoints[idx1], 0);
					}
					break;

				case 's':
					if (idx1 != 6) {
						JOptionPane.showMessageDialog(DatasetAnnotated_Pt.this,
								"not complete yet.");
					} else {
						saveAnnotation();
					}
					break;

				case 'n':
					// skip current annotation due to missing
					Arrays.fill(xPoints[idx1], 0);
					Arrays.fill(yPoints[idx1], 0);
					idx1++;
					idx2 = 0;
					break;

				case 'a':
					setNextPic(0);
					break;

				case 'd':
					setNextPic(1);
					break;

				default:
					switch (e.getKeyCode()) {
					case KeyEvent.VK_ESCAPE:
						Arrays.fill(xPoints[idx1], 0);
						Arrays.fill(yPoints[idx1], 0);
						idx2 = 0;
						break;

					case KeyEvent.VK_SHIFT:
						flag = 1;
						break;
					}
				}
				DatasetAnnotated_Pt.this.repaint();
			}
		});

		new DropTarget(this, this);

		setVisible(true);
	}

	protected void saveAnnotation() {
		try {
			File file = new File(path + "/groundTruth");
			file.mkdir();
			file = new File(path + "/groundTruth/" + fileName + ".xml");
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Annotation");
			rootElement
					.setAttribute("filename", path + "/" + fileName + ".jpg");
			doc.appendChild(rootElement);

			for (int i = 0; i < hands.length; i++) {
				Element hand = doc.createElement("hand_arm");
				hand.setAttribute("attr", hands[i]);
				rootElement.appendChild(hand);

				for (int j = 0; j < hand_component.length; j++) {
					int tmp = j + i * hand_component.length;
					Element component = doc.createElement(hand_component[j]);
					hand.appendChild(component);

					for (int k = 0; k < 5; k++) {
						Element pts = doc.createElement("point");
						component.appendChild(pts);

						Element x = doc.createElement("x");
						x.appendChild(doc
								.createTextNode((int) (xPoints[tmp][k] / scale)
										+ ""));
						pts.appendChild(x);

						Element y = doc.createElement("y");
						y.appendChild(doc
								.createTextNode((int) (yPoints[tmp][k] / scale)
										+ ""));
						pts.appendChild(y);
					}
				}
			}

			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(file);
			transformer.transform(source, result);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "failed to save annotation");
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
		gOffScreenGraphics.setColor(COLOR_TYPE[idx1 % 3]);
		if (idx1 != 6)
			gOffScreenGraphics.drawString(hands[idx1 / 3] + " "
					+ hand_component[idx1 % 3], 10, 50);
		else
			gOffScreenGraphics.drawString("annotatation is done!", 10, 50);
		for (int i = 0; i < idx2; i++) {
			gOffScreenGraphics.drawLine(xPoints[idx1][i], yPoints[idx1][i],
					xPoints[idx1][i + 1], yPoints[idx1][i + 1]);
		}
		for (int i = 0; i < idx1; i++) {
			gOffScreenGraphics.setColor(COLOR_TYPE[i % 3]);
			gOffScreenGraphics.drawPolyline(xPoints[i], yPoints[i], 5);
		}
		// paint(gOffScreen);
		g.drawImage(offScreenImage, 0, 0, null);
		g.dispose();
	}

	@Override
	public void paint(Graphics g) {
		update(g);
		/*
		 * if (initImage == null) return;
		 * 
		 * g.drawImage(initImage, 0, 0, this); g.setColor(COLOR_TYPE[idx1 % 3]);
		 * if (idx1 != 6) g.drawString(hands[idx1 / 3] + " " +
		 * hand_component[idx1 % 3], 10, 50); else
		 * g.drawString("annotatation is done!", 10, 50); for (int i = 0; i <
		 * idx2; i++) { g.drawLine(xPoints[idx1][i], yPoints[idx1][i],
		 * xPoints[idx1][i + 1], yPoints[idx1][i + 1]); } for (int i = 0; i <
		 * idx1; i++) { g.setColor(COLOR_TYPE[i % 3]);
		 * g.drawPolyline(xPoints[i], yPoints[i], 5); }
		 */
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
						if (file.getName().endsWith(".jpg")
								|| file.getName().endsWith(".xml"))
							file_list.add(file);
					}
					/*
					 * initImage = ImageIO.read(file);
					 * 
					 * initImage = initImage.getScaledInstance( (int)
					 * (initImage.getWidth(null) * scale), (int)
					 * (initImage.getHeight(null) * scale), Image.SCALE_SMOOTH);
					 * 
					 * path = file.getParent(); fileName = file.getName();
					 * setTitle("Dataset Annotation(" + fileName + ")");
					 * 
					 * int idx = fileName.lastIndexOf('.'); if (idx != -1)
					 * fileName = fileName.substring(0, idx);
					 * 
					 * idx1 = 0; idx2 = 0; for (int i = 0; i < 6; i++) {
					 * Arrays.fill(xPoints[i], 0); Arrays.fill(yPoints[i], 0); }
					 * setSize(initImage.getWidth(null),
					 * initImage.getHeight(null));
					 * 
					 * readAnnotation(path + "/groundTruth/" + fileName +
					 * ".xml"); repaint();
					 */
				}
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
					return;
				}
				File file = file_list.get(curFileIdx);

				if (file.getName().endsWith(".xml")) {
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(file);
					doc.getDocumentElement().normalize();

					File tmpFile = new File(doc.getFirstChild().getAttributes()
							.item(0).getNodeValue());
					if (!tmpFile.exists()) {
						path = file.getParent();
						fileName = file.getName();

						int idx = fileName.lastIndexOf('.');
						if (idx != -1)
							fileName = fileName.substring(0, idx);

						file = new File(path + "/../" + fileName + ".jpg");
						if (!file.exists())
							throw new Exception("No such image");

					} else {
						file = tmpFile;
					}
				}

				initImage = ImageIO.read(file);

				initImage = initImage.getScaledInstance(
						(int) (initImage.getWidth(null) * scale),
						(int) (initImage.getHeight(null) * scale),
						Image.SCALE_SMOOTH);

				path = file.getParent();
				fileName = file.getName();
				setTitle("Dataset Annotation(" + fileName + ")");

				int idx = fileName.lastIndexOf('.');
				if (idx != -1)
					fileName = fileName.substring(0, idx);

				idx1 = 0;
				idx2 = 0;
				for (int i = 0; i < 6; i++) {
					Arrays.fill(xPoints[i], 0);
					Arrays.fill(yPoints[i], 0);
				}
				setSize(initImage.getWidth(null), initImage.getHeight(null));

				readAnnotation(path + "/groundTruth/" + fileName + ".xml");
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

	private boolean readAnnotation(String path) {
		File gt = new File(path);
		if (!gt.exists())
			return false;

		int choice = JOptionPane.showConfirmDialog(this,
				"Annotation file is found, read it or not?", "tips",
				JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION)
			return false;

		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(gt);
			doc.getDocumentElement().normalize();

			NodeList hands_node_list = doc.getElementsByTagName("hand_arm");

			for (int i = 0; i < hands_node_list.getLength(); i++) {

				Node hand_node = hands_node_list.item(i);

				Element hand_node_element = (Element) hand_node;
				int j = 0;
				if (hand_node_element.getAttribute("attr").equals("right")) {
					j = 3;
				}
				for (int m = 0; m < hand_component.length; m++) {
					NodeList hand_component_list = hand_node_element
							.getElementsByTagName(hand_component[m]);
					Element hand_component_element = (Element) hand_component_list
							.item(0);

					NodeList pt_list = hand_component_element
							.getElementsByTagName("point");

					for (int n = 0; n < 5; n++) {
						Element point = (Element) pt_list.item(n);
						xPoints[j + m][n] = (int) (Integer.valueOf(getTagValue(
								"x", point)) * scale);
						yPoints[j + m][n] = (int) (Integer.valueOf(getTagValue(
								"y", point)) * scale);
					}
				}
			}
			idx1 = 6;
		} catch (Exception e) {
			e.printStackTrace();
			idx1 = 0;
			idx2 = 0;
			for (int i = 0; i < 6; i++) {
				Arrays.fill(xPoints[i], 0);
				Arrays.fill(yPoints[i], 0);
			}
			JOptionPane.showMessageDialog(this, "Invalid annotation file");
		}
		return true;
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0)
				.getChildNodes();

		Node nValue = (Node) nlList.item(0);

		return nValue.getNodeValue();
	}

	public static void main(String[] args) {
		new DatasetAnnotated_Pt();
	}

}

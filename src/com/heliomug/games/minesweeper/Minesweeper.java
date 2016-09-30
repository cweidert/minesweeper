package com.heliomug.games.minesweeper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This is the main class for the whole game.  
 * This is a fake comment just to test out git check ins, etc.  
 */
public class Minesweeper extends JFrame {
	// color constants
	private static final int DARK = 64;
	private static final int MEDIUM = 128;
	private static final int BRIGHT = 255;
	
	// constants for phase of game
	private static final int BEFORE_GAME = 0;
	private static final int DURING_GAME = 1;
	private static final int AFTER_GAME = 2;
	
	// This is a set of colors to use for the game.  
	// They are the colors for the cells of varying mine counts.  
	private static final Color[] COLORS = {
		new Color(192, 192, 192), 
		new Color(MEDIUM, MEDIUM, BRIGHT),
		new Color(MEDIUM, BRIGHT, MEDIUM),
		new Color(BRIGHT, MEDIUM, MEDIUM),
		new Color(0, 0, BRIGHT),
		new Color(0, MEDIUM, 0),
		new Color(MEDIUM, 0, 0),
		new Color(MEDIUM, MEDIUM, MEDIUM),
		new Color(0, 0, 0)
	};

	// This is an array of pixel sizes for each cell on a side.  
	private static final int[] CELL_SIZES = {10, 15, 20, 25, 30, 35, 40};

	// This is the default selection from the above.  
	private static final int DEFAULT_SIZE = 20;
	private static final Object[][] CONFIGS = {{"Easy", 9, 9, 10}, {"Medium", 16, 16, 40}, {"Hard", 30, 16, 99}};
	private static final int DEFAULT_CONFIG = 1;
	private static final boolean DEFAULT_NUMBERS_ON = true;
	
	private static final long serialVersionUID = -913399936352121840L;

	private Cell[][] cells;
	private int mines;
	private int width;
	private int height;
	
	private int phase;
	private Scores scores;
	private int difficulty;
	
	private long startTime; 
	private double finishTime; 
	
	JPanel board;

	private int pix;
	private Font font;
	private boolean isNumbersOn;
	
	public Minesweeper() {
		this(CONFIGS[DEFAULT_CONFIG], DEFAULT_CONFIG);
	}
	
	private Minesweeper(Object[] config, int configNumber) {
		this((int)config[1], (int)config[2], (int)config[3], configNumber);
	}
	
	public Minesweeper(int width, int height, int mines, int configNumber) {
		super("Minesweeper");
		setupGame(width, height, mines, configNumber);
		scores = new Scores();
		setupGUI();
	}
	
	private void setupGame(int width, int height, int mines, int configNumber) {
		this.difficulty = configNumber;
		this.mines = mines;
		this.width = width;
		this.height = height;
		resetGame();
	}

	private void resetGame() {
		cells = new Cell[height][width];
		for (int y = 0 ; y < height ; y++) {
			for (int x = 0 ; x < width ; x++) {
				cells[y][x] = new Cell();
			}
		}
		for (int i = 0 ; i < mines ; i++) {
			int x, y;
			do {
				x = (int)(Math.random() * width);
				y = (int)(Math.random() * height);
			} while (cells[y][x].isMine);
			cells[y][x].isMine = true;
		}
		this.phase = BEFORE_GAME;
		this.startTime = 0;
	}
	
	@SuppressWarnings("serial")
	private void setupGUI() {
		isNumbersOn = DEFAULT_NUMBERS_ON;
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		

		board = new JPanel() {
			private static final long serialVersionUID = -2368428293416801104L;

			public void paintComponent(Graphics graphics) {
				//super.paintComponent(graphics);
				Minesweeper.this.paintBoard(graphics);
			}
		};
		board.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int cellX = e.getX() / pix;
				int cellY = e.getY() / pix; 
				if (phase == DURING_GAME || phase == BEFORE_GAME) {
					if (SwingUtilities.isRightMouseButton(e) && SwingUtilities.isLeftMouseButton(e)) {
						handleBothClick(cellX, cellY);
					} else if (SwingUtilities.isRightMouseButton(e)) {
						handleRightClick(cellX, cellY);
					} else {
						handleLeftClick(cellX, cellY);
					}
				}
			}
		});
		board.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					System.exit(0);
				} else if (e.getKeyCode() == KeyEvent.VK_F5) {
					resetGame();
					repaint();
				}
			}
		});
		board.setFocusable(true);
		setCellSize(DEFAULT_SIZE);
		panel.add(board, BorderLayout.CENTER);

		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new GridLayout(1, 0));
		infoPanel.add(new JLabel("Mines: ", SwingConstants.CENTER) {
			public void paint(Graphics g) {
				this.setText(String.format("Mines: %d", minesLeft()));
				super.paint(g);
			}
		});		
		JButton button = new JButton("Reset") {
			@Override
			public void paint(Graphics g) {
				if (Minesweeper.this.phase == DURING_GAME || Minesweeper.this.phase == BEFORE_GAME) {
					this.setText("Reset");
				} else if (Minesweeper.this.isWinner()) {
					this.setText("Win!");
				} else {
					this.setText("Lose!");
				}
				super.paint(g);
			}
		};
		button.setFocusable(false);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Minesweeper.this.resetGame();
				Minesweeper.this.repaint();
			}
		});
		infoPanel.add(button);		
		infoPanel.add(new JLabel("Time: ", SwingConstants.CENTER) {
			public void paint(Graphics g) {
				int secs = (int)timeElapsed();
				this.setText(String.format("%d:%02d", secs/60, secs%60));
				super.paint(g);
			}
		});		
		panel.add(infoPanel, BorderLayout.NORTH);

		setupMenus();
	
		this.add(panel);
		this.pack();
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}
	
	private void setupMenus() {
		JMenuBar bar = new JMenuBar();
		JMenu menu, submenu;
		JMenuItem item;
		
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		item = new JMenuItem("Exit");
		item.setMnemonic(KeyEvent.VK_X);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menu.add(item);
		bar.add(menu);
		
		menu = new JMenu("Options");
		menu.setMnemonic(KeyEvent.VK_O);
		submenu = new JMenu("Cell Size");
		submenu.setMnemonic(KeyEvent.VK_Z);
		for (int i = 0 ; i < CELL_SIZES.length ; i++) {
			submenu.add(sizeChangeItem(i));
		}
		menu.add(submenu);
		submenu = new JMenu("Difficulty");
		submenu.setMnemonic(KeyEvent.VK_D);
		for (int i = 0 ; i < 3 ; i++) {
			submenu.add(changeDifficultyItem(i));
		}
		item = new JMenuItem("Custom");
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				configureCustomSize();
			}
		});
		submenu.add(item);
		menu.add(submenu);
		submenu = new JMenu("Numbers");
		submenu.setMnemonic(KeyEvent.VK_N);
		item = new JMenuItem("On");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Minesweeper.this.isNumbersOn = true;
				Minesweeper.this.repaint();
			}
		});
		submenu.add(item);
		item = new JMenuItem("Off");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Minesweeper.this.isNumbersOn = false;
				Minesweeper.this.repaint();
			}
		});
		submenu.add(item);
		menu.add(submenu);
		bar.add(menu);
		menu = new JMenu("Scores");
		menu.setMnemonic(KeyEvent.VK_S);
		item = new JMenuItem("See Top Scores");
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showScores();
			}
		});
		menu.add(item);
		bar.add(menu);
		this.setJMenuBar(bar);
	}

	private JMenuItem changeDifficultyItem(int notch) {
		Object[] config = CONFIGS[notch];
		String name = (String)config[0] + ":";
		int width = (int)config[1];
		int height = (int)config[2];
		int mines = (int)config[3];
		String str = String.format("%s %d X %d, %d mines", name, width, height, mines); 
		JMenuItem item = new JMenuItem(str);
		item.setHorizontalAlignment(SwingConstants.LEFT);
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeDifficulty(width, height, mines, notch);
			}
		});
		return item;
	}
	
	private JMenuItem sizeChangeItem(int notch) {
		JMenuItem item = new JMenuItem(String.valueOf(CELL_SIZES[notch]));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Minesweeper.this.setCellSize(CELL_SIZES[notch]);
			}
		});
		return item;
	}
	
	private void showScores() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><table>");
		sb.append("<tr><th>Difficulty</th><th>Seconds</th></tr>");
		for (int i = 0 ; i < 3 ; i++) {
			String diff = (String)CONFIGS[i][0];
			sb.append(String.format("<tr><td>%s</td><td>%.02f</td></tr>", diff, scores.getTopScore(i))); 
		}
		sb.append("</table></html>");
		JOptionPane.showMessageDialog(this, sb.toString(), "Top Scores", JOptionPane.PLAIN_MESSAGE);
	}
	
	private void changeDifficulty(int w, int h, int m, int configNumber) {
		setupGame(w, h, m, configNumber);
		if (configNumber == 0 && pix < 25) {
			pix = 25;
		}
		int maxW = (Toolkit.getDefaultToolkit().getScreenSize().width - 100)/ width;
		int maxH = (Toolkit.getDefaultToolkit().getScreenSize().height - 100) / height;
		pix = Math.min(maxW, Math.min(pix, maxH));
		board.setPreferredSize(new Dimension(width * pix, height * pix));
		Minesweeper.this.pack();
		Minesweeper.this.repaint();
	}

	private void setCellSize(int size) {
		pix = size;
		board.setPreferredSize(new Dimension(width * pix, height * pix));
		font = new Font("SansSerif", Font.BOLD, (int)(pix * .6));
		this.pack();
		this.repaint();
	}
	
	private void configureCustomSize() {
		JDialog dialog = new JDialog(this, "Set Custom Minefield");
		dialog.setLayout(new BorderLayout(0, 1));
		JPanel panel;
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));
		panel.add(new JLabel("Width: "));
		panel.add(new JLabel("Height: "));
		panel.add(new JLabel("Mines: "));
		dialog.add(panel, BorderLayout.WEST);
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));
		JSlider w = new JSlider(1, 100, width);
		JSlider h = new JSlider(1, 100, height);
		JSlider m = new JSlider(1, 400, mines);
		panel.add(w);
		panel.add(h);
		panel.add(m);
		dialog.add(panel, BorderLayout.CENTER);
		panel = new JPanel();
		panel.setLayout(new GridLayout(0, 1));
		JLabel widthLabel = new JLabel(String.valueOf(w.getValue()));
		JLabel heightLabel = new JLabel(String.valueOf(h.getValue()));
		JLabel mineLabel = new JLabel(String.valueOf(m.getValue()));
		panel.add(widthLabel);
		panel.add(heightLabel);
		panel.add(mineLabel);
		dialog.add(panel, BorderLayout.EAST);
		ChangeListener listener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				m.setMaximum(w.getValue() * h.getValue());
				widthLabel.setText(String.valueOf(w.getValue()));
				heightLabel.setText(String.valueOf(h.getValue()));
				mineLabel.setText(String.valueOf(m.getValue()));
			}
		};
		w.addChangeListener(listener);
		h.addChangeListener(listener);
		m.addChangeListener(listener);
		
		JButton button = new JButton("Set!");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeDifficulty(w.getValue(), h.getValue(), m.getValue(), 3);
				dialog.dispose();
			}
		});
		dialog.add(button, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setVisible(true);

	}
	
	
	private void setStartTime() {
		startTime = System.currentTimeMillis();
		Timer t = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Minesweeper.this.repaint();
			}
		});
		t.start();
	}
	
	private double timeElapsed() {
		if (phase == BEFORE_GAME) {
			return 0.0;
		} else if (phase == DURING_GAME) {
			return (System.currentTimeMillis() - startTime) / 1000.0;
		} else {
			return finishTime;
		}
	}
	
	private int minesLeft() {
		int count = mines;
		for (int i = 0 ; i < width ; i++) {
			for (int j = 0 ; j < height ; j++) {
				if (!cells[j][i].isOpen && cells[j][i].isFlag) {
					count--;
				}
			}
		}
		return count;
	}

	
	private int minesAround(int x, int y) {
		int count = 0;
		for (int i = x - 1 ; i <= x + 1 ; i++) {
			for (int j = y - 1 ; j <= y + 1 ; j++) {
				if (inBounds(i, j) && cells[j][i].isMine) {
					count++;
				}
			}
		}
		return count;
	}
	
	private int flagsAround(int x, int y) {
		int count = 0;
		for (int i = x - 1 ; i <= x + 1 ; i++) {
			for (int j = y - 1 ; j <= y + 1 ; j++) {
				if (inBounds(i, j) && !cells[j][i].isOpen && cells[j][i].isFlag) {
					count++;
				}
			}
		}
		return count;
	}

	private boolean inBounds(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}
	

	private void winGame() {
		for (int i = 0 ; i < width ; i++) {
			for (int j = 0 ; j < height ; j++) {
				cells[j][i].isFlag = true;
			}
		}
		if (scores.isTopScore(finishTime, difficulty)) {
			scores.setTopScore(finishTime, difficulty);
			String diff = (String)CONFIGS[difficulty][0];
			String message = String.format("New top score for %s difficulty: %.02f seconds!", diff, finishTime);
			JOptionPane.showMessageDialog(this, message, "New Top Score!", JOptionPane.PLAIN_MESSAGE);
		} 
		this.phase = AFTER_GAME;
	}
	
	private void loseGame() {
		this.phase = AFTER_GAME;
	}
	
	private boolean isWinner() {
		for (int x = 0 ; x < width ; x++) {
			for (int y = 0 ; y < height ; y++) {
				if (!cells[y][x].isMine && !cells[y][x].isOpen) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isLoser() {
		for (int x = 0 ; x < width ; x++) {
			for (int y = 0 ; y < height ; y++) {
				if (cells[y][x].isMine && cells[y][x].isOpen) {
					return true;
				}
			}
		}
		return false;
	}
	


	private void openFrom(int x, int y) {
		int count = 0;
		while (phase == BEFORE_GAME && cells[y][x].isMine && count < 10) {
			resetGame();
			count++;
		}
		
		if (startTime == 0) {
			setStartTime();
		}
		if (!cells[y][x].isFlag) {
			cells[y][x].isOpen = true;
			if (minesAround(x, y) == 0) {
				for (int i = x - 1 ; i <= x + 1 ; i++) {
					for (int j = y - 1 ; j <= y + 1 ; j++) {
						if (inBounds(i, j) && !cells[j][i].isOpen) {
							openFrom(i, j);
						}
					}
				}
			}
		}
	}

	private void finish() {
		phase = DURING_GAME;
		repaint();
		if (isWinner()) {
			finishTime = timeElapsed();
			phase = AFTER_GAME;
			winGame();
		}
		if (isLoser()) {
			finishTime = timeElapsed();
			phase = AFTER_GAME;
			loseGame();
		}
	}
	
	private void paintBoard(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		
		for (int x = 0 ; x < width ; x++) {
			for (int y = 0 ; y < height ; y++) {
				int m = minesAround(x, y);  
				
				if (cells[y][x].isOpen) {
					if (cells[y][x].isMine) {
						g.setColor(new Color(0, 0, 0));
						g.fill(new Rectangle2D.Double(x * pix, y * pix, pix, pix));
						g.setColor(new Color(BRIGHT, 0, 0));
						g.fill(new Ellipse2D.Double(x * pix + pix / 4, y * pix + pix / 4, pix / 2, pix / 2));					
					} else {
						g.setColor(COLORS[m]);
						g.fill(new Rectangle2D.Double(x * pix, y * pix, pix, pix));
						if (isNumbersOn && m > 0) {
							int xOff = fm.stringWidth(String.valueOf(m)) / 2;
							int yOff = fm.getHeight()/ 2;
							g.setColor(Color.BLACK);
							if (m > 3) g.setColor(Color.WHITE);
							g.drawString(String.valueOf(m), x * pix + pix/2 - xOff, y * pix + pix/2 + yOff);
						}
					}
				} else {
					if (phase == AFTER_GAME && cells[y][x].isFlag && !cells[y][x].isMine && !cells[y][x].isOpen) {
						g.setColor(new Color(BRIGHT, 0, 0));
						g.fill(new Rectangle2D.Double(x * pix, y * pix, pix, pix));
						g.setColor(new Color(BRIGHT, BRIGHT, BRIGHT));
						g.fill(new Rectangle2D.Double(x * pix + pix / 4, y * pix + pix / 4, pix / 2, pix / 2));					
					} else if (phase == AFTER_GAME && cells[y][x].isMine && isLoser() && !cells[y][x].isFlag) {
						g.setColor(new Color(DARK, DARK, DARK));
						g.fill(new Rectangle2D.Double(x * pix, y * pix, pix, pix));
						g.setColor(new Color(BRIGHT, 0, 0));
						g.fill(new Ellipse2D.Double(x * pix + pix / 4, y * pix + pix / 4, pix / 2, pix / 2));					
					} else if (cells[y][x].isFlag) {
						g.setColor(new Color(DARK, DARK, DARK));
						g.fill(new Rectangle2D.Double(x * pix, y * pix, pix, pix));
						g.setColor(new Color(BRIGHT, BRIGHT, BRIGHT));
						g.fill(new Rectangle2D.Double(x * pix + pix / 4, y * pix + pix / 4, pix / 2, pix / 2));					
					} else {
						g.setColor(new Color(DARK, DARK, DARK));
						g.fill(new Rectangle2D.Double(x * pix, y * pix, pix, pix));
					}
				}
				g.setColor(new Color(0, 0, 0));
				g.draw(new Rectangle2D.Double(x * pix, y * pix, pix, pix));
			}
		}
	}
	
	
	
	private void handleBothClick(int x, int y) {
		if (flagsAround(x, y) == minesAround(x, y)) {
			for (int i = x - 1 ; i <= x + 1 ; i++) {
				for (int j = y - 1 ; j <= y + 1 ; j++) {
					if (inBounds(i, j)) {
						openFrom(i, j);	
					}
				}
			}
			finish();
		}
	}
	
	private void handleLeftClick(int x, int y) {
		openFrom(x, y);
		finish();
	}
	
	private void handleRightClick(int x, int y) {
		cells[y][x].isFlag = !cells[y][x].isFlag;
		finish();
	}
	
	/*
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int y = 0 ; y < cells.length ; y++) {
			for (int x = 0 ; x < cells[y].length ; x++) {
				sb.append(cells[y][x].mine ? "X" : minesAround(x, y));
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	*/
	
	private class Cell {
		public boolean isMine;
		public boolean isOpen;
		public boolean isFlag;
		
		public Cell() {
			isMine = false;
			isOpen = false;
			isFlag = false;
		}
	}
	
	private class Scores implements Serializable {
		private static final long serialVersionUID = -6717488649767459992L;

		double[] scores;
		
		public Scores() {
			loadScores();
		}
		
		public double getTopScore(int diff) {
			return scores[diff];
		}
		
		public boolean isTopScore(double time, int difficulty) {
			if (difficulty < 0 || difficulty > 2) {
				return false;
			}
			return time < scores[difficulty];
		}

		public String getScoresPath() {
			String scoreSavePath = ".minescores";
			scoreSavePath = System.getProperty("user.home") + File.separator + scoreSavePath;
			return scoreSavePath;
		}
		
		public void setTopScore(double time, int difficulty) {
			scores[difficulty] = time;
			
			File f = new File(getScoresPath());
			try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
				oos.writeObject(scores);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void loadScores() {
			File f = new File(getScoresPath());
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
				scores = (double[])ois.readObject();
			} catch (IOException | ClassNotFoundException e) {
				scores = new double[3];
				for (int i = 0 ; i < scores.length ; i++) {
					scores[i] = Double.POSITIVE_INFINITY;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new Minesweeper();
			}
		});
	}
}

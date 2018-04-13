package com.andreruegg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;


public class BJS {

	public static String saveLocation = "C:/Users/Andre/Desktop/";
	public static HashMap<HandCombination, OptionTable> data = new HashMap<HandCombination, OptionTable>();

	public static int handsPlayed = 0;
	public static int lastHandsPlayed = 0;

	public static int averageHandsPerX = 0;

	public static int averagedAmount = 0;
	
	static boolean useHistory = false;
	
	public static String tableToLoad = "table";
	
	//Eval variables
	public static boolean evaluationMode = false;
	
	public static int masterGameWin = 0;
	
	public static int masterGameLoss = 0;

	public static HashMap<HandCombination, Option> evalTable = new HashMap<HandCombination, Option>();
	
	public static void main(String[] args) {
		if (useHistory && !evaluationMode) {
			try {
				File handStats = new File(saveLocation + "handstats.data");
				if (handStats.exists()) {
					String s = FileUtils.readFileToString(handStats);
					String[] ss = s.split(",");
					handsPlayed = Integer.parseInt(ss[0]);
					lastHandsPlayed = Integer.parseInt(ss[1]);
					averageHandsPerX = Integer.parseInt(ss[2]);
					averagedAmount = Integer.parseInt(ss[3]);
				}
				
				FileInputStream fileInputStream = new FileInputStream(saveLocation + "bjs.data");
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				
				data = (HashMap<HandCombination, OptionTable>) objectInputStream.readObject();
				System.out.println("Loaded data: " + data.size());
				System.out.println("Data loaded");
			} catch (IOException | ClassNotFoundException e) {
				System.out.println("Couldn't load data");
				e.printStackTrace();
			}
		}
		
		if(evaluationMode) {
			//Load eval data
			File handComboTable = new File(saveLocation + tableToLoad + ".data");
			if (handComboTable.exists()) {
				String s;
				try {
					s = FileUtils.readFileToString(handComboTable);
					String[] ss = s.split("&");
					for(String sss : ss) {
						String[] comps = sss.split(":");
						String dealersUp = comps[0];
						CardValue cv = CardValue.valueOf(dealersUp);
						String playersHandCode = comps[1];
						String option = comps[2];
						Option o = Option.valueOf(option);
						
						evalTable.put(new HandCombination(playersHandCode, cv), o);
					}
					System.out.println("Eval loaded");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		List<String> handCodesChecking = new ArrayList<String>();
		// Prepare data table
		// Every card pair
		for (CardValue c : CardValue.values()) {
			CardValue using = c;
			if (c == CardValue.King || c == CardValue.Queen || c == CardValue.Jack) {
				//using = CardValue.C10;
				continue;
			}
			for (CardValue cc : CardValue.values()) {
				data.put(new HandCombination((c + "," + c), cc), new OptionTable(true));
			}
			handCodesChecking.add(using + "," + using);
		}
		
		for (int i = 2; i != 21; i++) {
			for (CardValue c : CardValue.values()) {
				if(i < 11) {
					data.put(new HandCombination(("Ace," + i), c), new OptionTable(false));
				}
				data.put(new HandCombination((i + ""), c), new OptionTable(false));
			}
			if(i < 11) {
				handCodesChecking.add("Ace," + i);
			}
		}

		// Split for neat presenting purposes because collection sorting doesn't work
		for (int i = 5; i != 21; i++) {
			handCodesChecking.add(i + "");
		}

		// Collections.sort(handCodesChecking);

		// Run simulation
		int threadsToRun = 5;
		for (int i = 0; i < threadsToRun; i++) {
			new SimulatorThread().start();
		}

		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JLabel handsPlayedLabel = new JLabel("0 hands played");
		handsPlayedLabel.setPreferredSize(new Dimension(1000, 24));

		frame.add(handsPlayedLabel, BorderLayout.PAGE_START);
		JTable table = new JTable() {
			@Override
		    public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
		        Component comp = super.prepareRenderer(renderer, row, col);
		        Object value = getModel().getValueAt(row, col);
	        	String valString = (String) value;
	        	if(valString.startsWith("STAND")) {
	        		comp.setBackground(Color.red);
	        	}else if(valString.startsWith("HIT")) {
	        		comp.setBackground(Color.green);
	        	}else if(valString.startsWith("SPLIT")) {
	        		comp.setBackground(Color.blue);
	        	}else if(valString.startsWith("DBLE")) {
	        		comp.setBackground(Color.yellow);
	        	}else {
	        		comp.setBackground(Color.GRAY);
	        	}
		        return comp;
		    }
		};

		JScrollPane scrollPane = new JScrollPane(table);
		//scrollPane.setBounds(0, 24, 1000, 700);
		frame.add(scrollPane, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024, 780);
		frame.setVisible(true);
		
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					if(useHistory) {
						FileOutputStream fileOutputStream = new FileOutputStream(saveLocation + "bjs.data");
						ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
						
						objectOutputStream.writeObject(data);
						objectOutputStream.close();
						System.out.println("Saved data: " + data.size());
						
						FileUtils.writeStringToFile(new File(saveLocation + "handstats.data"), handsPlayed + "," + lastHandsPlayed + "," + averageHandsPerX + "," + averagedAmount);
						
						//Create readable simulation
						String handCombinationTable = "";
						for(Entry<HandCombination, OptionTable> ee : data.entrySet()) {
							handCombinationTable += ee.getKey().dealersUp.toString() + ":" + ee.getKey().playersHandCode + ":" + ee.getValue().getRecommendedOption().toString() + "&";
						}
						
						if(handCombinationTable.length() > 0) {
							handCombinationTable = handCombinationTable.substring(0,handCombinationTable.length()-1);
						}
						
						FileUtils.writeStringToFile(new File(saveLocation + tableToLoad + ".data"), handCombinationTable);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});

		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {

				if (lastHandsPlayed != 0) {
					int newHandsInLastX = handsPlayed - lastHandsPlayed;
					averageHandsPerX = ((averageHandsPerX * averagedAmount) + newHandsInLastX) / (averagedAmount + 1);
					averagedAmount++;
				}
				
				lastHandsPlayed = handsPlayed;

				List<Object[]> rowDataList = new ArrayList<Object[]>();

				if(!evaluationMode) {
					handsPlayedLabel.setText(NumberFormat.getNumberInstance(Locale.US).format(handsPlayed) + " hands played (" + (averageHandsPerX * 2) + ") per second");
				}else {
					handsPlayedLabel.setText("Total win+push percentage: " + (((double)masterGameWin/(double)(masterGameWin+masterGameLoss))*100) + " - " + NumberFormat.getNumberInstance(Locale.US).format(masterGameWin+masterGameLoss) + " games");
				}
				
				for (String s : handCodesChecking) {
					List<String> thisRow = new ArrayList<String>();
					thisRow.add(s);
					for (CardValue cv : CardValue.values()) {
						String option = "";
						
						Option optionUsing = evaluationMode ? getStrictMove(new HandCombination(s, cv), null) : data.get(new HandCombination(s, cv)).getRecommendedOption();
						OptionTable ot = data.get(new HandCombination(s,cv));
						int standWin = 100-(int) Math.round((ot.standBust/(double) (ot.standWin+ot.standBust+ot.standPush))*100);
						int hitWin = 100-(int) Math.round((ot.hitBust/(double)(ot.hitWin+ot.hitBust+ot.hitPush))*100);
						int dbleWin = 100-(int) Math.round((ot.dbleBust/(double)(ot.dbleWin+ot.dbleBust+ot.dblePush))*100);
						int splitWin = 100-(int) ((double)(ot.splitBust+ot.splitBust+ot.splitPush) > 0 ? Math.round((ot.splitBust/(double)(ot.splitWin+ot.splitBust+ot.splitPush))*100) : 0);
						
						option = optionUsing.name() + " |" + standWin + "," + hitWin + "," + dbleWin + "," + splitWin;
						thisRow.add(option);
					}
					rowDataList.add(thisRow.toArray());
				}

				Object[][] rowDataConverted = new Object[rowDataList.size()][];
				rowDataConverted = rowDataList.toArray(rowDataConverted);

				// Every single handCombination we want to check
				Object[] columnHeaders = CardValue.values();
				Object[] emptySpace = { "" };
				columnHeaders = ArrayUtils.addAll(emptySpace, columnHeaders);

				DefaultTableModel dtm = (DefaultTableModel) table.getModel();

				if (dtm.getRowCount() != 0) {
					int rowIterated = 0;
					for (Object[] row : rowDataConverted) {
						int columnIterated = 0;
						for (Object rowObject : row) {
							dtm.setValueAt(rowObject, rowIterated, columnIterated);
							columnIterated++;
						}
						rowIterated++;
					}
				} else { 
					dtm.setDataVector(rowDataConverted, columnHeaders);
				}
			}
		}, 0, 200);
	}	
	
	public static Option getStrictMove(HandCombination hc, List<MoveHistory> moves) {		
		if(moves != null) {
			if (moves.size() > 0) {
				MoveHistory lastMove = moves.get(moves.size() - 1);
				if (lastMove.o == Option.DBLE) {
					return Option.STAND;
				}
			}
			
			int splitsOccurred = 0;
			for (MoveHistory m : moves) {
				if (m.o == Option.SPLIT) {
					splitsOccurred++;
				}
			}
			if (splitsOccurred == 3) {
				if(evalTable.get(hc) == Option.SPLIT) {
					return null;
				}
			}
		}
		return evalTable.get(hc);
	}

	public static List<Card> createDeck() {
		List<Card> cards = new ArrayList<Card>();
		for (CardValue cv : CardValue.values()) {
			for (Suit s : Suit.values()) {
				cards.add(new Card(s, cv));
			}
		}
		return cards;
	}

}

package com.andreruegg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionTable implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3926474939888466894L;

	// Game rules
	int splitsAllowed = 3;

	int standWin = 0;
	int standBust = 0;
	int standPush = 0;
	int hitWin = 0;
	int hitBust = 0;
	int hitPush = 0;
	int splitWin = 0;
	int splitBust = 0;
	int splitPush = 0;
	int dbleWin = 0;
	int dbleBust = 0;
	int dblePush = 0;
	
	boolean isPair = false;

	public OptionTable(boolean isPair) {
		this.isPair = isPair;
	}

	// Balance option, try to balance for accurate results
	// We implement rules here, don't allow a hit if they just double down, etc.
	public Option getBalancedOption(List<MoveHistory> moves) {
		List<Integer> notAllowedIndexes = new ArrayList<Integer>();
		if (moves.size() > 0) {
			MoveHistory lastMove = moves.get(moves.size() - 1);
			if (lastMove.o == Option.DBLE) {
				return Option.STAND;
			}
			int splitsOccurred = 0;
			for (MoveHistory m : moves) {
				if (m.o == Option.SPLIT) {
					splitsOccurred++;
				}
			}
			if (splitsOccurred == splitsAllowed) {
				notAllowedIndexes.add(3);// Split index
			}

		}

		int standsPlayed = standWin + standBust + standPush;
		int hitsPlayed = hitWin + hitBust + hitPush;
		int dblesPlayed = dbleWin + dbleBust + dblePush;
		int splitsPlayed = splitWin + splitBust + splitPush;
		int[] amountsPlayedArr = new int[] { standsPlayed, hitsPlayed, dblesPlayed };

		if (isPair) {
			amountsPlayedArr = new int[] { standsPlayed, hitsPlayed, dblesPlayed, splitsPlayed };
		}

		int bestOptionIndex = 0;
		double lowestValue = amountsPlayedArr[0];
		for (int i = 0; i != amountsPlayedArr.length; i++) {
			if (!notAllowedIndexes.contains(i)) {
				double val = amountsPlayedArr[i];
				if (val <= lowestValue) {
					bestOptionIndex = i;
					lowestValue = val;
				}
			}
		}
		return Option.values()[bestOptionIndex];

	}
	//Possible future weight system
	
	
	//Splitting = % win
	//Double = % double doesn't bust
	//hit = % hit doesn't bust
	//standing = percent that stand makes us  win

	// Focus on best result and keep testing it.
	public Option getRecommendedOption() {
		double standNoLossRatio = (double) 1 - (double) ((double) standBust / (double) (standWin + standBust+standPush));
		double hitNoLossRatio = (double) 1 - (double) ((double) hitBust / (double) (hitWin + hitBust+hitPush));
		double dbleNoLossRatio = (double) 1 - (double) ((double) dbleBust / (double) (dbleWin + dbleBust+dblePush));
		double splitNoLossRatio = (double) 1 - (double) ((double) splitBust / (double) (splitWin + splitBust+splitPush));
		
		double[] ratioArrAll = new double[] { standNoLossRatio, hitNoLossRatio, dbleNoLossRatio };// Same order as Options enum

		if (isPair) {
			ratioArrAll = new double[] { standNoLossRatio, hitNoLossRatio, dbleNoLossRatio, splitNoLossRatio };
		}

		int bestOptionIndex = 0;
		double highestValue = ratioArrAll[0];
		for (int i = 0; i != ratioArrAll.length; i++) {
			double val = ratioArrAll[i];
			if (val > highestValue) {
				bestOptionIndex = i;
				highestValue = val;
			}
		}
		return Option.values()[bestOptionIndex];
	}

	public void processGameResult(Option o, int gameResult) {
		if (o == Option.HIT) {
			if (gameResult == 1) {
				hitWin++;
				return;
			}else if(gameResult == 2) {
				hitBust++;
				return;
			}
			hitPush++;//0
		} else if (o == Option.STAND) {
			if (gameResult == 1) {
				standWin++;
				return;
			}else if(gameResult == 2) {
				standBust++;
				return;
			}
			standPush++;
		} else if (o == Option.SPLIT) {
			if (gameResult == 1) {
				splitWin++;
				return;
			}else if(gameResult == 2) {
				splitBust++;
				return;
			}
			splitPush++;
		} else if (o == Option.DBLE) {
			if (gameResult == 1) {
				dbleWin++;
				return;
			}else if(gameResult == 2) {
				dbleBust++;
				return;
			}
			dblePush++;
		}
	}

	@Override
	public int hashCode() {
		Object[] stats = {splitsAllowed, standWin, standBust, standPush, hitWin, hitBust, hitPush, splitWin, splitBust, splitPush, dbleWin, dbleBust, dblePush, isPair};
		return stats.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OptionTable)) {
			return false;
		}
		OptionTable ot = (OptionTable) obj;
		int[] statsOT = {ot.splitsAllowed, ot.standWin, ot.standBust, ot.standPush, ot.hitWin, ot.hitBust,ot.hitPush,  ot.splitWin, ot.splitBust, ot.splitPush, ot.dbleWin, ot.dbleBust, ot.dblePush};
		int[] stats = {splitsAllowed, standWin, standBust, standPush, hitWin, hitBust, hitPush, splitWin, splitBust, splitPush, dbleWin, dbleBust, dblePush};
		if(Arrays.equals(statsOT, stats) && ot.isPair == isPair) {
			return true;
		}
		return false;
	}
}

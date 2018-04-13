package com.andreruegg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SimulatorThread extends Thread{
	
	List<Card> deck = new ArrayList<Card>();
	
	List<SplitPostponer> standingSplits = new ArrayList<SplitPostponer>();
	
	boolean debug = false;
	
	/*
	 * Why does it prefer doubling over hitting
	 * hitting would be statistically better because we can hit AND have other options
	 *
	 */
	@Override
	public void run() {
		while (!isInterrupted()) {
			List<Card> dealersHand = new ArrayList<Card>();
			dealersHand.add(getNewCard());//Dealers upcard
			dealersHand.add(getNewCard());
						
			List<Card> playersHand = new ArrayList<Card>();
			playersHand.add(getNewCard());
			playersHand.add(getNewCard());
			
			dbg("-----------------------");
			play(dealersHand, playersHand, new ArrayList<MoveHistory>());
			//Base it off the current table of our past games, this way we will test current and refine it.
			
		}
	}
		
	//Split logic
	//
	//If one of the splits bust, then just credit there until the last split
	//If one of them is standing, at to standingSplits list and wait until root right deals with it.
	
	//If the tree splits due to a double/split it will be counted twice but I believe it should anyways.
	public void play(List<Card> dealersHand, List<Card> playersHand, List<MoveHistory> pastMoves) { 
		String handCode = getPlayersHandCode(playersHand);
	//	System.out.println(handCode);
		HandCombination thisPlaysCombo = new HandCombination(handCode, dealersHand.get(0).cv);
		Option ourMove = null;
		if(!BJS.evaluationMode) {
			dbg("Getting move");
			ourMove = BJS.data.get(thisPlaysCombo).getBalancedOption(pastMoves);
		}else {
			ourMove = BJS.getStrictMove(thisPlaysCombo, pastMoves);
		}
		if(ourMove == null) {//Can no longer split
			handCode = "" + getLargestValidValue(playersHand);
			thisPlaysCombo = new HandCombination(handCode, dealersHand.get(0).cv);
			if(!BJS.evaluationMode) {
				ourMove = BJS.data.get(thisPlaysCombo).getBalancedOption(pastMoves);
			}else {
				ourMove = BJS.getStrictMove(thisPlaysCombo, pastMoves);
			}
		}
		
		dbg(createLogHandString(playersHand) + " against " + createLogHandString(dealersHand) + " and " + ourMove.toString());
		
		if(ourMove == Option.STAND) {
			pastMoves.add(new MoveHistory(thisPlaysCombo, Option.STAND));
			//TURN ENDING
			int ourHandValue = getLargestValidValue(playersHand);
			
			if(!isFromSplit(pastMoves)) {
				//Draw the dealer until at least 17
				while(getLargestValidValue(dealersHand) < 17) {//Draw until 17
					dealersHand.add(getNewCard());
				}
				
				dbg(createLogHandString(dealersHand) + " hand after stand");
				
				int dealersHandValue = getLargestValidValue(dealersHand);
				
				if(dealersHandValue > 21) {
					dbg("Dealer bust.");
					creditMoves(pastMoves, 1);//They busted
					return;
				}
				
				if(ourHandValue > dealersHandValue) {//We won
					dbg("We won");
					creditMoves(pastMoves, 1);
					return;
				}else if(ourHandValue == dealersHandValue) {//Draw
					dbg("Draw");
					creditMoves(pastMoves, 0);
					return;
				}
				dbg("Lost");
				creditMoves(pastMoves, 2);//We lost
				return;
			}
			
			dbg("Not drawing yet, but standing due to split");
			standingSplits.add(new SplitPostponer(pastMoves, getLargestValidValue(playersHand)));//Dont draw dealer yet.
			return;
		}else if(ourMove == Option.HIT || ourMove == Option.DBLE) {//Hit and double are the same except no more drawing for double which is already accounted
			pastMoves.add(new MoveHistory(thisPlaysCombo, ourMove));
			
			playersHand.add(getNewCard());
			
			dbg(createLogHandString(playersHand));
			
			if(getLargestValidValue(playersHand) > 21) {
				//We busted
				dbg("We busted hitting/dbling");
				creditMoves(pastMoves, 2);
				return;
			}
			
			if(getLargestValidValue(playersHand) == 21) {
				if(!isFromSplit(pastMoves)) {
					
					while(getLargestValidValue(dealersHand) < 17) {//Draw until 17
						dealersHand.add(getNewCard());
					}
					
					if(getLargestValidValue(dealersHand) != 21) {//If it's anything but a 21 then we win, else draw.
						dbg("We won");
						creditMoves(pastMoves, 1);
					}else {//Draw
						creditMoves(pastMoves, 0);
					}
					return;
				}
				dbg("Postponing");
				standingSplits.add(new SplitPostponer(pastMoves, getLargestValidValue(playersHand)));//Dont draw dealer yet.
				return;
			}
			
			play(dealersHand, playersHand, pastMoves);
		}else if(ourMove == Option.SPLIT) {//WE NEEDA FIX THIS IN THE FUTURE, IT DOESN'T SPLIT CORRECTLY
			pastMoves.add(new MoveHistory(thisPlaysCombo, Option.SPLIT));
			Card c = playersHand.get(0);
			playersHand.remove(0);//Theoretically add this to 2nd hand
			playersHand.add(getNewCard());
			
			//Add new card before playing first hand
			List<Card> newHand = new ArrayList<Card>();
			newHand.add(new Card(c.s, c.cv));
			newHand.add(getNewCard());
			
			ArrayList<MoveHistory> itsArray = (ArrayList<MoveHistory>) pastMoves;
			List<MoveHistory> duplicatePastMoves = (ArrayList<MoveHistory>)itsArray.clone();
			
			if(getLargestValidValue(playersHand) == 21) {
				dbg("First hand won immediate split");
				standingSplits.add(new SplitPostponer(duplicatePastMoves, getLargestValidValue(playersHand)));
			}else {
				play(dealersHand, playersHand, duplicatePastMoves);
			}
			
			//If we're here and there is only 1 split then we are the last execution of the the split and need to
			//credit everything and hit the dealer.
			
			int splitAmount = 0;
			for(MoveHistory m : pastMoves) {
				if(m.o == Option.SPLIT) {
					splitAmount++;
				}
			}
			
			if(getLargestValidValue(playersHand) == 21) {
				dbg("2nd hand won immediate split");
				standingSplits.add(new SplitPostponer(pastMoves, getLargestValidValue(playersHand)));
			}else {
				play(dealersHand, newHand, pastMoves);
			}
			
			if(splitAmount == 1) {//This is the last, we gotta deal with everything here
				//Credit every stand
				
				//Finally draw the hand
				if(standingSplits.size() == 0) {
					return;//We busted everything, no point of drawing dealer
				}
				
				while(getLargestValidValue(dealersHand) < 17) {//Draw until 17
					dealersHand.add(getNewCard());
				}
				
				int dealersHandValue = getLargestValidValue(dealersHand);
				
				dbg("Last split drew dealer to " + dealersHandValue);
								
				for(SplitPostponer sp : standingSplits) {
					dbg("Split postponer hand val: " + sp.handValue + " " + createMoveHistoryString(sp.moves));
					int playersHandValue = sp.handValue;
					
					if(dealersHandValue > 21) {//They busted
						creditMoves(sp.moves, 1);
						dbg("Dealer bust");
						continue;
					}
					if(playersHandValue > dealersHandValue) {
						creditMoves(sp.moves, 1);//We won
						dbg("We won");
						continue;
					}else if(playersHandValue == dealersHandValue) {//Draw
						dbg("Draw");
						creditMoves(sp.moves, 0);
						continue;
					}
					dbg("We lost");
					creditMoves(sp.moves, 2);//We lost
				}
				standingSplits.clear();
			}
			
		}
	}
	
	public boolean isFromSplit(List<MoveHistory> moves) {
		for(MoveHistory m : moves) {
			if(m.o == Option.SPLIT) {
				return true;
			}
		}
		return false;
	}
	
	public void creditMoves(List<MoveHistory> moves, int gameResult) {//Credit moves after a play
		if(gameResult == 1 || gameResult == 0) {
			BJS.masterGameWin++;
		}else if(gameResult == 2){
			BJS.masterGameLoss++;
		}
		for(int i = (moves.size()-1);i != -1;i--) {
			MoveHistory m = moves.get(i);
			BJS.data.get(m.hc).processGameResult(m.o, gameResult);
			BJS.handsPlayed++;
			if(m.o == Option.SPLIT) {
				break;
			}
		}
	}
	
	public Card getNewCard() {
		if(deck.size() > 0) {
			Card c = deck.get(0);
			deck.remove(0);
			return c;
		}
		deck = BJS.createDeck();
		Collections.shuffle(deck);
		return getNewCard();
	}
	
	public int getLargestValidValue(List<Card> hand) {
		int aceAmount = 0;
		int remainingHardValue = 0;
		for(Card c : hand) {
			if(c.cv == CardValue.Ace) {
				aceAmount++;
			}else {
				remainingHardValue += getHardValue(c.cv);
			}
		}
		if(aceAmount == 0) {
			return remainingHardValue;
		}
		
		if((11+(aceAmount-1)+remainingHardValue) > 21) {//We can't even have one ace
			return remainingHardValue+aceAmount;//All of them are one's
		}
		return 11+(aceAmount-1)+remainingHardValue;
	}
	
	public String getPlayersHandCode(List<Card> playersHand) {
		if(playersHand.size() == 2) {
			if(playersHand.get(0).cv == playersHand.get(1).cv) {
				CardValue pairCV = playersHand.get(0).cv;
				if(pairCV == CardValue.King || pairCV == CardValue.Queen || pairCV == CardValue.Jack) {
					return "C10,C10";
				}
				return playersHand.get(0).cv + "," + playersHand.get(0).cv;//Pair
			}
		}
		int aceAmount = 0;
		int remainingHardValue = 0;
		for(Card c : playersHand) {
			if(c.cv == CardValue.Ace) {
				aceAmount++;
			}else {
				remainingHardValue += getHardValue(c.cv);
			}
		}
		//If two aces, and could be two possible combos, etc: A,A,4 = A 5 OR A 15, consider the lower one always.
		// only use 1 ace if more and doesn't bust
		if(aceAmount > 0) {
		//	System.out.println(createLogHandString(playersHand) + " and " + aceAmount + "aces");
			if((11+remainingHardValue+(aceAmount-1)) > 21) {//This will always be more than 11 because if the other was an Ace it'd pair.
		//		System.out.println((aceAmount+remainingHardValue));
				return "" + (aceAmount+remainingHardValue);//Some value 21 or under
			}
			if(aceAmount == 1) {
			//	System.out.println("Ace," + remainingHardValue);
				return "Ace," + remainingHardValue;//Should be A2-A10, pre-function should check for 21 already.
			}
			//Don't bother checking if all 1's and hard value bust, we make sure this is a valid hand before function.
		//	System.out.println("Ace," + (remainingHardValue+(aceAmount-1)));
			return "Ace," + (remainingHardValue+(aceAmount-1));//Only consider 1 ace.
		}
		
		return "" + remainingHardValue;
	}
	
	public String createLogHandString(List<Card> hand) {
		String s = "";
		for(Card c : hand) {
			s += c.cv.toString() + ",";
		}
		if(hand.size() > 0) {
			s = s.substring(0, s.length()-1);
		}
		return s;
	}
	
	public String createMoveHistoryString(List<MoveHistory> moves) {
		String s = "";
		for(MoveHistory m : moves) {
			s += "->" + m.o.name();
		}
		if(s.length() > 0) {
			s = s.substring(2, s.length());
		}
		return s;
	}
	
	public void dbg(String s) {
		if(debug) {
			System.out.println(s);
		}
	}
	public int getHardValue(CardValue cv) {
		switch(cv) {
			case C2:
				return 2;
			case C3:
				return 3;
			case C4:
				return 4;
			case C5:
				return 5;
			case C6:
				return 6;
			case C7:
				return 7;
			case C8:
				return 8;
			case C9:
				return 9;
			case C10:
				return 10;
			case Jack:
				return 10;
			case Queen:
				return 10;
			case King:
				return 10;
			default:
				return 0;
		}
	}
	
	//Check if pair
	//Check if contains ace
	
}

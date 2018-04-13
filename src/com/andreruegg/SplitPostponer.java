package com.andreruegg;

import java.util.List;

public class SplitPostponer {

	public List<MoveHistory> moves;
	public int handValue;//Most valid hand value (not bust)
	
	public SplitPostponer(List<MoveHistory> moves, int handValue) {
		this.moves = moves;
		this.handValue = handValue;
	}
}

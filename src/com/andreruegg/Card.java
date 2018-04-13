package com.andreruegg;

import java.io.Serializable;

public class Card implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3335197221761311510L;
	public Suit s;
	public CardValue cv;
	
	public Card(Suit s, CardValue cv) {
		this.s = s;
		this.cv = cv;
	}
	
	@Override
	public int hashCode() {
		int hash = 13;
		hash *= s.hashCode();
		hash *= cv.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Card)) {
            return false;
        }
		Card check = (Card) obj;
		if(this.s == check.s && this.cv == check.cv) {
			return true;
		}
		return false;
	}
	
	
}

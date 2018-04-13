package com.andreruegg;

import java.io.Serializable;

public class HandCombination implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5947800838367265796L;
	
	public String playersHandCode;
	public CardValue dealersUp;
	
	public HandCombination(String playersHandCode, CardValue dealersUp) {
		this.playersHandCode = playersHandCode;
		this.dealersUp = dealersUp;
	}
	
	@Override
	public int hashCode() {
		return playersHandCode.hashCode()*dealersUp.hashCode();
	}

	@Override
	public boolean equals(Object obj){
	    if(!(obj instanceof HandCombination)){
	        return false;
	    }
	    HandCombination hc = (HandCombination) obj;
	    return (hc.playersHandCode.equals(this.playersHandCode)) && (hc.dealersUp == this.dealersUp);
	}
}

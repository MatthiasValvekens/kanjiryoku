package be.mapariensis.kanjiryoku.util;

import java.lang.Character.UnicodeBlock;

public class UnicodeBlockFilter implements Filter<Character> {
	private final UnicodeBlock block;
	
	public UnicodeBlockFilter(UnicodeBlock block) {
		this.block = block;
	}

	@Override
	public boolean accepts(Character thing) {
		return UnicodeBlock.of(thing) == block;
	}

}

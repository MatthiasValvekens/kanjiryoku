package be.mapariensis.kanjiryoku.model;


public class Word {
	public final String furigana;
	public final String main;
	public Word(String main) {
		this(main,"");
	}
	public Word(String main,String furigana) {
		this.furigana = furigana != null ? furigana : "";
		if(main == null) throw new IllegalArgumentException("Main part must not be null");
		this.main = main;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((furigana == null) ? 0 : furigana.hashCode());
		result = prime * result + ((main == null) ? 0 : main.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Word other = (Word) obj;
		if (furigana == null) {
			if (other.furigana != null)
				return false;
		} else if (!furigana.equals(other.furigana))
			return false;
		if (main == null) {
			if (other.main != null)
				return false;
		} else if (!main.equals(other.main))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "<"+main+","+furigana+">";
		
	}
	
}

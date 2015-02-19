package be.mapariensis.kanjiryoku.util;

import java.util.LinkedList;
import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;

public class ParsingUtils {
	// don't use pure Pattern here, input strings might be very long
	public static List<Dot> parseDots(String in) {
		in = in.replaceAll("(\\[<|>\\])", "");
		String[] dots = in.split(">,\\s*<");
		List<Dot> res = new LinkedList<Dot>();
		for (String dotstring : dots) {
			String[] vals = dotstring.split(",\\s*");
			if (vals.length != 2)
				throw new NumberFormatException("Too many values in dot");
			res.add(new Dot(Integer.parseInt(vals[0]), Integer
					.parseInt(vals[1])));
		}
		return res;
	}
}

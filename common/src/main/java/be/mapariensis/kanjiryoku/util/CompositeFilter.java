package be.mapariensis.kanjiryoku.util;

import java.util.Arrays;
import java.util.Collection;

public class CompositeFilter<T> implements Filter<T> {
	public static enum CombinationMode {
		PERMISSIVE_MODE { //at least one check must pass (OR reduce)
			@Override
			public <T> boolean reduce(Collection<? extends Filter<? super T>> filters,
					T thing) {
				for(Filter<? super T> f : filters) {
					if(f.accepts(thing)) return true;
				}
				return false;
			}
		}, RESTRICTIVE_MODE { // all checks must pass (AND reduce)
			@Override
			public <T> boolean reduce(Collection<? extends Filter<? super T>> filters,
					T thing) {
				for(Filter<? super T> f : filters) {
					if(!f.accepts(thing)) return false;
				}
				return true;
			}
		};

		public abstract <T> boolean reduce(Collection<? extends Filter<? super T>> filters, T thing);
	}
	
	private final Collection<? extends Filter<? super T>> filters;
	
	private final CombinationMode mode;
	
	public CompositeFilter(CombinationMode mode, Collection<? extends Filter<? super T>> filters) {
		this.filters = filters;
		this.mode = mode;
	}
	
	@SafeVarargs
	public CompositeFilter(CombinationMode mode, Filter<? super T>... filters) {
		this(mode,Arrays.asList(filters));
	}
	
	@Override
	public boolean accepts(T thing) {
		return mode.reduce(filters, thing);
	}
}

package be.mapariensis.kanjiryoku.cr;

public class Dot {
    public final int x;
    public final int y;

    public Dot(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
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
        Dot other = (Dot) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString() {
        return "<" + x + "," + y + ">";
    }
}

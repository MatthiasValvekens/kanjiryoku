package be.mapariensis.kanjiryoku.net.server.games;

public class Score {
    int correct, failed;

    Score() {
    }

    public Score(int correct, int failed) {
        this.correct = correct;
        this.failed = failed;
    }

    public int getCorrect() {
        return correct;
    }

    public int getFailed() {
        return failed;
    }
}

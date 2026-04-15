package com.codemetricstudio.model;

public class LocMetrics {
    private int totalLines;
    private int blankLines;
    private int commentLines;
    private int codeLines;

    public LocMetrics() {
    }

    public LocMetrics(int totalLines, int blankLines, int commentLines, int codeLines) {
        this.totalLines = totalLines;
        this.blankLines = blankLines;
        this.commentLines = commentLines;
        this.codeLines = codeLines;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getBlankLines() {
        return blankLines;
    }

    public void setBlankLines(int blankLines) {
        this.blankLines = blankLines;
    }

    public int getCommentLines() {
        return commentLines;
    }

    public void setCommentLines(int commentLines) {
        this.commentLines = commentLines;
    }

    public int getCodeLines() {
        return codeLines;
    }

    public void setCodeLines(int codeLines) {
        this.codeLines = codeLines;
    }

    public void add(LocMetrics other) {
        this.totalLines += other.totalLines;
        this.blankLines += other.blankLines;
        this.commentLines += other.commentLines;
        this.codeLines += other.codeLines;
    }
}

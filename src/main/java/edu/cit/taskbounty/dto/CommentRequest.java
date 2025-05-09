package edu.cit.taskbounty.dto;

public class CommentRequest {
    private String parentCommentId;
    private String content;

    public String getParentCommentId() {
        return this.parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
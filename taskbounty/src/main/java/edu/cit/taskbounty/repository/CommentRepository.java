package edu.cit.taskbounty.repository;

import edu.cit.taskbounty.model.Comment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, ObjectId> {
    List<Comment> findByBountyPostId(ObjectId bountyPostId);
    List<Comment> findByParentCommentId(ObjectId parentCommentId);
}
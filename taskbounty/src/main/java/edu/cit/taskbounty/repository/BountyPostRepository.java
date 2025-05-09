package edu.cit.taskbounty.repository;

import edu.cit.taskbounty.model.BountyPost;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BountyPostRepository extends MongoRepository<BountyPost, ObjectId> {
    @Query("{ 'isPublic': true }")
    Page<BountyPost> findAllPublic(Pageable pageable);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ], 'isPublic': true }")
    Page<BountyPost> searchPublicBountyPosts(String search, Pageable pageable);

    // New method to find draft posts (non-public) for a specific user
    @Query("{ 'creatorId': ?0, 'isPublic': false }")
    Page<BountyPost> findDraftsByCreatorId(String creatorId, Pageable pageable);

    // New method to find a specific draft post for a user
    @Query("{ '_id': ?0, 'creatorId': ?1, 'isPublic': false }")
    Optional<BountyPost> findDraftByIdAndCreatorId(ObjectId id, String creatorId);
}
package com.agroverse.backend.controller;

import com.agroverse.backend.model.CommunityComment;
import com.agroverse.backend.model.CommunityPost;
import com.agroverse.backend.model.User;
import com.agroverse.backend.repository.CommunityCommentRepository;
import com.agroverse.backend.repository.CommunityPostRepository;
import com.agroverse.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommunityController(
            CommunityPostRepository postRepository,
            CommunityCommentRepository commentRepository,
            UserRepository userRepository
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/posts")
    public ResponseEntity<List<CommunityPost>> getPosts(@RequestParam(required = false) String category) {
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
            return ResponseEntity.ok(postRepository.findByCategoryOrderByCreatedAtDesc(category.trim()));
        }
        return ResponseEntity.ok(postRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/posts")
    @Transactional
    public ResponseEntity<?> createPost(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required to post."));
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found."));
        }

        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Post content cannot be blank."));
        }

        String title = payload.get("title");
        if (title == null || title.trim().isEmpty()) {
            title = content.length() > 60 ? content.substring(0, 57).trim() + "..." : content.trim();
        }
        String category = payload.get("category");

        CommunityPost post = new CommunityPost();
        post.setUser(user);
        post.setAuthorName(user.getFullName() != null ? user.getFullName() : "Farmer " + user.getFarmerId());
        post.setAuthorAvatar(user.getProfileImageUrl());
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setCategory(category != null ? category.trim() : "General");
        post.setLikesCount(0);
        post.setCommentsCount(0);

        CommunityPost saved = postRepository.save(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/posts/{id}/like")
    @Transactional
    public ResponseEntity<?> likePost(@PathVariable Long id) {
        CommunityPost post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Post not found."));
        }
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
        return ResponseEntity.ok(Map.of("likes", post.getLikesCount()));
    }

    @DeleteMapping("/posts/{id}")
    @Transactional
    public ResponseEntity<?> deletePost(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        CommunityPost post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Post not found."));
        }

        if (post.getUser() == null || !post.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only delete your own posts."));
        }

        postRepository.delete(post);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    @GetMapping("/snapshot")
    public ResponseEntity<Map<String, Object>> getSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        long count = postRepository.count();
        long users = userRepository.count();
        snapshot.put("totalPosts", count);
        snapshot.put("activeUsers", Math.max(1, users));
        snapshot.put("trendingTopic", "#OrganicFarming");
        return ResponseEntity.ok(snapshot);
    }
}

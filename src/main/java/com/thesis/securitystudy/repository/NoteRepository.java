package com.thesis.securitystudy.repository;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByOwner(User owner);
    List<Note> findByOwnerOrderByCreatedAtDesc(User owner);
    List<Note> findByIsPublicTrue();

    @Query("/* TODO: JPQL for searching a user's own notes by title/content. " +
            "Replace this comment with your JPQL, e.g. SELECT n FROM Note n ... */")
    List<Note> searchByOwnerAndQuery(@Param("owner") User owner, @Param("q") String q);

    @Query("/* TODO: JPQL for searching public notes by title/content. " +
            "Replace this comment with your JPQL, e.g. SELECT n FROM Note n ... */")
    List<Note> searchPublicByQuery(@Param("q") String q);
}

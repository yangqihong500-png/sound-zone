package com.soundzone.message.repository;

import com.soundzone.message.entity.DirectMessage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    @Query(
            """
            select m from DirectMessage m
            where (m.sender.id = :first and m.recipient.id = :second)
               or (m.sender.id = :second and m.recipient.id = :first)
            order by m.createdAt desc, m.id desc
            """)
    List<DirectMessage> findConversation(
            @Param("first") Long first,
            @Param("second") Long second,
            Pageable pageable);

    @Query(
            """
            select count(m) from DirectMessage m
            where (m.sender.id = :first and m.recipient.id = :second)
               or (m.sender.id = :second and m.recipient.id = :first)
            """)
    long countConversation(@Param("first") Long first, @Param("second") Long second);
}

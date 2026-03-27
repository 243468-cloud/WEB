package com.squadup.repository;

import com.squadup.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Notificaciones recientes del usuario (últimas 30) */
    List<Notification> findTop30ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /** Conteo de no leídas — para el badge de la campana 🔔 */
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /** Marcar todas como leídas */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllReadByUserId(@Param("userId") Long userId);
}

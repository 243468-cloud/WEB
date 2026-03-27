package com.squadup.repository;

import com.squadup.entity.OAuthAccount;
import com.squadup.entity.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderUid(OAuthProvider provider, String providerUid);
}

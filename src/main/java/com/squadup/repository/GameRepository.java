package com.squadup.repository;

import com.squadup.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByNameIgnoreCase(String name);

    List<Game> findByGenreIgnoreCase(String genre);
}

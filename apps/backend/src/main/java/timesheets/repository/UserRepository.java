// package timesheets.repository;

// import java.util.Optional;
// import java.util.UUID;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;
// import timesheets.domain.User;

// // Spring Data JPA repository for the User entity.
// // JpaRepository gives us save(), findById(), findAll(), delete() etc for free.
// // We only need to define custom queries here that aren't covered by the defaults.
// @Repository
// public interface UserRepository extends JpaRepository<User, UUID> {

//   // finds a user by their email address, used during login and registration checks
//   Optional<User> findByEmail(String email);

//   // checks if an email already exists without loading the full user object
//   boolean existsByEmail(String email);
// }

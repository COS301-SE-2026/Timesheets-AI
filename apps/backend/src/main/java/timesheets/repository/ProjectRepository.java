package timesheets.repository;

import timesheets.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    //just having this as a stub for now, I won't add the custom functions yet, only basic CRUD will be available rn
}
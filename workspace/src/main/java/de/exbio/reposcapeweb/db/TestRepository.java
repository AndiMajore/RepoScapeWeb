package de.exbio.reposcapeweb.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestRepository extends MongoRepository<TestDoc,String> {
    public Optional<TestDoc> findById(String id);
}

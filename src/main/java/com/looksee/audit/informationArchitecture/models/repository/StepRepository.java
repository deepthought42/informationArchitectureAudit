package com.looksee.audit.informationArchitecture.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.audit.informationArchitecture.models.ElementState;
import com.looksee.audit.informationArchitecture.models.PageState;
import com.looksee.audit.informationArchitecture.models.journeys.Step;

import io.github.resilience4j.retry.annotation.Retry;


@Repository
@Retry(name = "neoforj")
public interface StepRepository extends Neo4jRepository<Step, Long>{

	public Step findByKey(@Param("key") String step_key);

	@Query("MATCH (:ElementInteractionStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	@Query("MATCH (s:Step)WHERE id(s)=$step_id MATCH (p:PageState) WHERE id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (p:PageState) WHERE id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (p:ElementState) WHERE id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);

	@Query("MATCH (p:PageState) WHERE id(s)=$step_id WHERE id(p)=$page_state_id MATCH (step:Step)-[:STARTS_WITH]->(p:PageState) RETURN step")
	public List<Step> getStepsWithStartPage(@Param("page_state_id") long id);

}

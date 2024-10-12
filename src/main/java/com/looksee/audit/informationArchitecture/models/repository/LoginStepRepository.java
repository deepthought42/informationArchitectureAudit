package com.looksee.audit.informationArchitecture.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.audit.informationArchitecture.models.ElementState;
import com.looksee.audit.informationArchitecture.models.PageState;
import com.looksee.audit.informationArchitecture.models.TestUser;
import com.looksee.audit.informationArchitecture.models.journeys.LoginStep;

import io.github.resilience4j.retry.annotation.Retry;


@Repository
@Retry(name = "neoforj")
public interface LoginStepRepository extends Neo4jRepository<LoginStep, Long> {

	@Query("MATCH (step:LoginStep{key:$step_key}) RETURN step")
	public LoginStep findByKey(@Param("step_key") String step_key);

	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (e:ElementState) WHERE id(e)=$element_id MERGE (s)-[:USERNAME_INPUT]->(e) RETURN e")
	public ElementState addUsernameElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	@Query("MATCH (s:LoginStep)-[:USERNAME_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getUsernameElement(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (e:ElementState) WHERE id(e)=$element_id MERGE (s)-[:PASSWORD_INPUT]->(e) RETURN e")
	public ElementState addPasswordElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	@Query("MATCH (s:LoginStep)-[:PASSWORD_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getPasswordElement(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (e:ElementState) WHERE id(e)=$element_id MERGE (s)-[:SUBMIT]->(e) RETURN e")
	public ElementState addSubmitElement(@Param("step_id") long id, @Param("element_id") long element_id);

	@Query("MATCH (s:LoginStep)-[:SUBMIT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getSubmitElement(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (p:PageState) AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:LoginStep)-[:STARTS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getStartPage(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (p:PageState) WHERE id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:Step)-[:ENDS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getEndPage(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WHERE id(s)=$step_id MATCH (user:TestUser) WHERE id(user)=$user_id MERGE (s)-[:USES]->(user) RETURN user")
	public TestUser addTestUser(@Param("step_id") long id, @Param("user_id") long user_id);

	@Query("MATCH (s:LoginStep)-[:USES]->(user:TestUser) WHERE id(s)=$step_id RETURN user")
	public TestUser getTestUser(@Param("step_id") long id);
}

package org.openmrs.eip.app.management.repository;

import java.util.List;

import org.openmrs.eip.app.management.entity.JMSBroker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("jmsBrokerRepository")
public interface JMSBrokerRepository extends JpaRepository<JMSBroker, Long> {
	
	List<JMSBroker> findByDisabledFalse();
	
}

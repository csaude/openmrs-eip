package org.openmrs.eip.app.management.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "jms_broker")
public class JMSBroker extends AbstractEntity {
	
	public static final long serialVersionUID = 1;
	
	@Column(nullable = false, unique = true)
	private String identifier;
	
	@Column(nullable = false)
	private String name;
	
	@Column(nullable = false)
	private String host;
	
	@Column(nullable = false)
	private Integer port;
	
	@Column(nullable = false)
	private String username;
	
	@Column(nullable = false)
	private String password;
	
	@Column(nullable = false)
	private Boolean disabled = Boolean.FALSE;
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public Integer getPort() {
		return port;
	}
	
	public void setPort(Integer port) {
		this.port = port;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public Boolean getDisabled() {
		return disabled;
	}
	
	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}
	
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	@Override
	public String toString() {
		return "JMSBroker [identifier=" + identifier + ", name=" + name + ", host=" + host + ", port=" + port + ", disabled="
		        + disabled + "]";
	}
	
}

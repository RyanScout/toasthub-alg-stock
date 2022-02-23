/*
 * Copyright (C) 2016 The ToastHub Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.toasthub.analysis.model;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass()
public class BaseEntity implements Serializable{
	private static final long serialVersionUID = 1L;
	
	protected Long id;
	protected Boolean active;
	protected Boolean archive;
	protected Boolean locked;
	protected Long lockOwnerRefId;
	protected Instant lockTime;
	protected Instant modified;
	protected Instant created;
	protected Long version;
	
	// Constructor
	public BaseEntity() {
	}
	
	// Setter/Getter
	@Id	
	@GeneratedValue(strategy=GenerationType.IDENTITY) 
	@Column(name = "id")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "modified",updatable = false, insertable = false)
	public Instant getModified() {
		return modified;
	}
	public void setModified(Instant modified) {
		this.modified = modified;
	}
	
	@Column(name = "created", updatable = false, insertable = false)
	public Instant getCreated() {
		return created;
	}
	public void setCreated(Instant created) {
		this.created = created;
	}
	
	@JsonIgnore
	@Version 
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}

	@Column(name = "is_active")
	public Boolean isActive() {
		return active;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}
	
	@Column(name = "is_archive")
	public Boolean isArchive() {
		return archive;
	}
	public void setArchive(Boolean archive) {
		this.archive = archive;
	}
	
	@Column(name = "is_locked")
	public Boolean isLocked() {
		return locked;
	}
	public void setLocked(Boolean locked) {
		this.locked = locked;
	}
	
	@Column(name = "lockowner_id")
	public Long getLockOwnerRefId() {
		return lockOwnerRefId;
	}
	public void setLockOwnerRefId(Long lockOwnerRefId) {
		this.lockOwnerRefId = lockOwnerRefId;
	}
	
	@Column(name = "lock_time")
	public Instant getLockTime() {
		return lockTime;
	}
	public void setLockTime(Instant lockTime) {
		this.lockTime = lockTime;
	}

}

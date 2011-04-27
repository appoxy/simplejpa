package com.spaceprogram.simplejpa.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.EntityListeners;
import java.io.Serializable;
import java.util.Date;

/**
 * Can use as a common base class.
 *
 * User: treeder
 * Date: Feb 16, 2008
 * Time: 4:44:05 PM
 */
@MappedSuperclass
@EntityListeners(TimestampEntityListener.class)
public class IdedTimestampedBase extends IdedBase implements Ided, Timestamped, Serializable {
    protected String id;
    private Date created;
    private Date updated;

    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public String getId() {
		return id;
	}

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date now) {
        this.updated = now;
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("created", created)
                .append("updated", updated)
                .toString();
    }
}

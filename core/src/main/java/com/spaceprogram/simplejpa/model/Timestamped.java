package com.spaceprogram.simplejpa.model;

import java.util.Date;

/**
 * User: treeder
 * Date: Jun 2, 2008
 * Time: 11:23:57 AM
 */
public interface Timestamped {
    void setCreated(Date d);
    Date getCreated();
    void setUpdated(Date d);
    Date getUpdated();
}

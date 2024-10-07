package org.sansorm;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Created by lbayer on 2/25/16.
 */
@Table(name = "target_class1")
public class TargetTimestampClass1
{
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @Column(name = "string")
    private String string;

    public TargetTimestampClass1()
    {
    }

    public TargetTimestampClass1(Timestamp timestamp, String string)
    {
        this.timestamp = timestamp;
        this.string = string;
    }

    public int getId()
    {
        return id;
    }

    public Timestamp getTimestamp()
    {
        return timestamp;
    }

    public String getString()
    {
        return string;
    }
}
